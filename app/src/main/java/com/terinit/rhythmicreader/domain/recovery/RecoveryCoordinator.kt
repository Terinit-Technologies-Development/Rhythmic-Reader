package com.terinit.rhythmicreader.domain.recovery

import com.terinit.rhythmicreader.data.repository.RecoveryRepository
import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoverySession
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import com.terinit.rhythmicreader.domain.model.meets
import com.terinit.rhythmicreader.domain.time.LocalDateClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private data class QualifiedReadingPage(
    val bookId: String,
    val pageIndex: Int,
    val qualifiedAtEpochMs: Long
)

private data class VerifiedReadingEvent(
    val duration: ActiveReadingDelta?,
    val qualifiedPage: QualifiedReadingPage?,
    val recoverySessionId: String?,
    val recoveryAccumulatedActiveMs: Long?
)

private sealed interface ProjectionQueueItem {
    data class Event(val value: VerifiedReadingEvent) : ProjectionQueueItem
    data class Barrier(val completed: CompletableDeferred<Unit>) : ProjectionQueueItem
}

/**
 * Measures one credible-reading stream and projects each measured delta into the daily ledger
 * and, when present, the active recovery session.
 */
class RecoveryCoordinator(
    private val repository: RecoveryRepository,
    val activeReadingTracker: ActiveReadingTracker,
    val pageQualificationEngine: PageQualificationEngine,
    private val dailyEvidenceRecorder: DailyEvidenceRecorder,
    private val localDateClock: LocalDateClock,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val _currentSession = MutableStateFlow<RecoverySession?>(null)
    val currentSession: StateFlow<RecoverySession?> = _currentSession.asStateFlow()

    private val _activityState = MutableStateFlow(ReadingActivityState())
    val activityState: StateFlow<ReadingActivityState> = _activityState.asStateFlow()

    private val projectionQueue = Channel<ProjectionQueueItem>(Channel.UNLIMITED)
    private val stateLock = Any()
    private var currentBookId: String? = null
    private var currentPageIndex: Int? = null
    private var qualificationDateKey: String? = localDateClock.todayDateKey()
    private var checkpointJob: Job? = null
    private var recoveryAccumulatedActiveMs: Long = 0L
    private var recoveryTrackerBaselineMs: Long = 0L
    private var recoverySessionStartTrackerMs: Long = 0L

    init {
        scope.launch(dispatcher) {
            for (item in projectionQueue) {
                when (item) {
                    is ProjectionQueueItem.Event -> {
                        try {
                            persistVerifiedReading(item.value)
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            // Keep the projection actor available for lifecycle barriers and later events.
                        }
                    }

                    is ProjectionQueueItem.Barrier -> item.completed.complete(Unit)
                }
            }
        }

        scope.launch(dispatcher) {
            repository.observeActiveSession().collect { session ->
                if (session != null) {
                    val current = _currentSession.value
                    if (current == null || current.sessionId == session.sessionId) {
                        val statusChanged = current?.status != session.status
                        _currentSession.value = session
                        if (current == null || current.sessionId != session.sessionId) {
                            synchronized(stateLock) {
                                recoveryAccumulatedActiveMs = session.accumulatedActiveMs
                                recoveryTrackerBaselineMs = activeReadingTracker.totalMs()
                                recoverySessionStartTrackerMs = recoveryTrackerBaselineMs
                            }
                        }
                        if (statusChanged) {
                            _activityState.update {
                                it.copy(sessionActive = session.status == RecoveryStatus.ACTIVE)
                            }
                            recalculateTracking()
                        }
                    }
                }
            }
        }
    }

    suspend fun startSession(
        requirement: RecoveryRequirement,
        sessionId: String = java.util.UUID.randomUUID().toString()
    ): RecoverySession {
        performCheckpoint()

        val session = repository.createSession(requirement, sessionId)
        synchronized(stateLock) {
            recoveryAccumulatedActiveMs = session.accumulatedActiveMs
            recoveryTrackerBaselineMs = activeReadingTracker.totalMs()
            recoverySessionStartTrackerMs = recoveryTrackerBaselineMs
        }
        _currentSession.value = session
        pageQualificationEngine.reset()
        currentPageIndex?.let(pageQualificationEngine::onPageChanged)
        _activityState.update { it.copy(sessionActive = true) }
        recalculateTracking()
        return session
    }

    suspend fun restoreSession(sessionId: String): RecoverySession? {
        flushProjectionQueue()
        val session = repository.getSession(sessionId) ?: return null
        setSession(session)
        pageQualificationEngine.reset()
        currentPageIndex?.let(pageQualificationEngine::onPageChanged)
        recalculateTracking()
        return session
    }

    fun attachExistingSession(session: RecoverySession) {
        setSession(session)
        pageQualificationEngine.reset()
        currentPageIndex?.let(pageQualificationEngine::onPageChanged)
        recalculateTracking()
    }

    private fun setSession(session: RecoverySession) {
        _currentSession.value = session
        synchronized(stateLock) {
            recoveryAccumulatedActiveMs = session.accumulatedActiveMs
            recoveryTrackerBaselineMs = activeReadingTracker.totalMs()
            recoverySessionStartTrackerMs = recoveryTrackerBaselineMs
        }
        _activityState.update { it.copy(sessionActive = session.status == RecoveryStatus.ACTIVE) }
    }

    fun updateAppForeground(isForeground: Boolean) {
        _activityState.update { it.copy(appForeground = isForeground) }
        recalculateTracking()
    }

    fun updateScreenInteractive(isInteractive: Boolean) {
        _activityState.update { it.copy(screenInteractive = isInteractive) }
        recalculateTracking()
    }

    fun updateDocumentLoaded(isLoaded: Boolean, bookId: String?) {
        val previousBookId = synchronized(stateLock) { currentBookId }
        _activityState.update { it.copy(documentLoaded = isLoaded) }

        if (!isLoaded || (previousBookId != null && previousBookId != bookId)) {
            pauseTracking(previousBookId)
            pageQualificationEngine.reset()
            synchronized(stateLock) { currentPageIndex = null }
        }

        synchronized(stateLock) { currentBookId = bookId.takeIf { isLoaded } }
        recalculateTracking()
    }

    fun updateReaderVisible(isVisible: Boolean) {
        _activityState.update { it.copy(readerVisible = isVisible) }
        recalculateTracking()
    }

    suspend fun onVisiblePageChanged(bookId: String, pageIndex: Int) {
        val isDuplicate = synchronized(stateLock) {
            currentBookId == bookId && currentPageIndex == pageIndex
        }
        if (isDuplicate) return
        refreshQualificationDate()
        val previousBookId = synchronized(stateLock) { currentBookId }
        val candidate = pageQualificationEngine.onPageChanged(pageIndex)
        val previousPageBookId = previousBookId ?: bookId
        val canQualify = _activityState.value.qualifiesForVerifiedReading
        var shouldFlushProjection = false

        if (canQualify && candidate is PageQualificationResult.Candidate) {
            enqueueProjection(
                duration = null,
                qualifiedPage = QualifiedReadingPage(
                    bookId = previousPageBookId,
                    pageIndex = candidate.pageIndex,
                    qualifiedAtEpochMs = localDateClock.nowEpochMs()
                )
            )
            shouldFlushProjection = true
        }

        synchronized(stateLock) {
            currentBookId = bookId
            currentPageIndex = pageIndex
        }
        if (shouldFlushProjection) flushProjectionQueue()
    }

    suspend fun performCheckpoint() {
        checkpoint()
        flushProjectionQueue()
    }

    fun flushPendingEvidenceAsync() {
        scope.launch(dispatcher) {
            flushProjectionQueue()
        }
    }

    private suspend fun checkpoint() {
        refreshQualificationDate()
        val isTracking = activeReadingTracker.isTracking()
        val candidate = if (isTracking) {
            pageQualificationEngine.checkCurrentPageDwell()
        } else {
            PageQualificationResult.None
        }
        val delta = activeReadingTracker.checkpointDelta()
        val bookId = synchronized(stateLock) { currentBookId }
        val page = if (
            isTracking && candidate is PageQualificationResult.Candidate && bookId != null
        ) {
            QualifiedReadingPage(bookId, candidate.pageIndex, localDateClock.nowEpochMs())
        } else {
            null
        }
        if (delta != null || page != null) enqueueProjection(delta, page)
    }

    private fun pauseTracking(bookId: String? = synchronized(stateLock) { currentBookId }) {
        val wasTracking = activeReadingTracker.isTracking()
        val candidate = pageQualificationEngine.onPause()
        val delta = activeReadingTracker.pauseAndGetDelta()
        val page = if (
            wasTracking && candidate is PageQualificationResult.Candidate && bookId != null
        ) {
            QualifiedReadingPage(bookId, candidate.pageIndex, localDateClock.nowEpochMs())
        } else {
            null
        }
        if (delta != null || page != null) enqueueProjection(delta, page)
    }

    private fun enqueueProjection(
        duration: ActiveReadingDelta?,
        qualifiedPage: QualifiedReadingPage?
    ) {
        val session = _currentSession.value?.takeIf { it.status == RecoveryStatus.ACTIVE }
        val recoveryActiveMs = if (session != null) {
            synchronized(stateLock) {
                if (duration != null) {
                    val projectedFromTrackerMs = maxOf(
                        recoveryTrackerBaselineMs,
                        recoverySessionStartTrackerMs
                    )
                    recoveryAccumulatedActiveMs +=
                        (duration.totalActiveMsAtEnd - projectedFromTrackerMs).coerceAtLeast(0L)
                    recoveryTrackerBaselineMs = duration.totalActiveMsAtEnd
                }
                recoveryAccumulatedActiveMs
            }
        } else {
            null
        }
        projectionQueue.trySend(
            ProjectionQueueItem.Event(
                VerifiedReadingEvent(
                    duration = duration,
                    qualifiedPage = qualifiedPage,
                    recoverySessionId = session?.sessionId,
                    recoveryAccumulatedActiveMs = recoveryActiveMs
                )
            )
        )
    }

    private suspend fun persistVerifiedReading(event: VerifiedReadingEvent) {
        val duration = event.duration
        // Every measured delta reaches the daily ledger. The recovery projection is optional.
        if (duration != null) {
            dailyEvidenceRecorder.recordVerifiedReading(duration)
        }
        val page = event.qualifiedPage
        if (page != null) {
            dailyEvidenceRecorder.recordQualifiedPage(
                page.bookId,
                page.pageIndex,
                page.qualifiedAtEpochMs
            )
        }

        val sessionId = event.recoverySessionId ?: return
        val activeMs = event.recoveryAccumulatedActiveMs ?: return
        if (event.duration != null) {
            repository.updateActiveTime(sessionId, activeMs)
        }
        if (page != null) {
            repository.qualifyPage(sessionId, page.bookId, page.pageIndex)
        }

        val updated = repository.getSession(sessionId) ?: return
        if (_currentSession.value?.sessionId == sessionId) {
            _currentSession.value = updated
        }
        evaluateCompletion(updated)
    }

    private suspend fun evaluateCompletion(session: RecoverySession) {
        if (session.status == RecoveryStatus.COMPLETE) return
        if (session.toProgress().meets(session.requirement)) {
            repository.markComplete(session.sessionId)
            val completed = repository.getSession(session.sessionId)
            if (completed != null && _currentSession.value?.sessionId == session.sessionId) {
                _currentSession.value = completed
            }
            _activityState.update { it.copy(sessionActive = false) }
        }
    }

    private fun recalculateTracking() {
        if (_activityState.value.qualifiesForVerifiedReading) {
            if (!activeReadingTracker.isTracking()) {
                activeReadingTracker.updateQualification(true)
                pageQualificationEngine.onResume()
                startPeriodicCheckpointing()
            }
        } else if (activeReadingTracker.isTracking()) {
            stopPeriodicCheckpointing()
            pauseTracking()
        } else {
            pageQualificationEngine.onPause()
        }
    }

    private fun refreshQualificationDate() {
        val newDateKey = localDateClock.todayDateKey()
        val previousDateKey = synchronized(stateLock) {
            val previous = qualificationDateKey
            qualificationDateKey = newDateKey
            previous
        }
        if (previousDateKey != null && previousDateKey != newDateKey) {
            pageQualificationEngine.restartCurrentDwell()
        }
    }

    private fun startPeriodicCheckpointing() {
        if (checkpointJob?.isActive == true) return
        checkpointJob = scope.launch(dispatcher) {
            while (isActive) {
                delay(20_000L)
                checkpoint()
            }
        }
    }

    private fun stopPeriodicCheckpointing() {
        checkpointJob?.cancel()
        checkpointJob = null
    }

    private suspend fun flushProjectionQueue() {
        val barrier = CompletableDeferred<Unit>()
        projectionQueue.send(ProjectionQueueItem.Barrier(barrier))
        barrier.await()
    }

    fun close() {
        stopPeriodicCheckpointing()
        if (activeReadingTracker.isTracking()) {
            pauseTracking()
        } else {
            pageQualificationEngine.onPause()
        }
    }

    fun getActiveReadingSeconds(): Long {
        val session = _currentSession.value ?: return 0L
        if (session.status != RecoveryStatus.ACTIVE) return session.activeSeconds
        val activeMs = synchronized(stateLock) {
            val liveUncheckpointedMs =
                (activeReadingTracker.totalMs() - recoveryTrackerBaselineMs).coerceAtLeast(0L)
            recoveryAccumulatedActiveMs + liveUncheckpointedMs
        }
        return activeMs / 1_000L
    }
}
