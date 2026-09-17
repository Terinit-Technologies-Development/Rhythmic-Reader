package com.terinit.rhythmicreader.domain.recovery

import com.terinit.rhythmicreader.data.repository.RecoveryRepository
import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoverySession
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import com.terinit.rhythmicreader.domain.model.meets
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RecoveryCoordinator(
    private val repository: RecoveryRepository,
    val activeReadingTracker: ActiveReadingTracker,
    val pageQualificationEngine: PageQualificationEngine,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    private val _currentSession = MutableStateFlow<RecoverySession?>(null)
    val currentSession: StateFlow<RecoverySession?> = _currentSession.asStateFlow()

    private val _activityState = MutableStateFlow(ReadingActivityState())
    val activityState: StateFlow<ReadingActivityState> = _activityState.asStateFlow()

    private var currentBookId: String? = null
    private var checkpointJob: Job? = null

    init {
        // Observe repository active session changes to keep _currentSession up to date
        scope.launch(dispatcher) {
            repository.observeActiveSession().collect { session ->
                if (session != null) {
                    val current = _currentSession.value
                    if (current == null || current.sessionId == session.sessionId) {
                        val statusChanged = current?.status != session.status
                        _currentSession.value = session
                        if (statusChanged) {
                            _activityState.update { it.copy(sessionActive = session.status == RecoveryStatus.ACTIVE) }
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
        if (activeReadingTracker.isTracking()) {
            handlePause()
        }

        val session = repository.createSession(requirement, sessionId)
        _currentSession.value = session
        activeReadingTracker.reset()
        pageQualificationEngine.reset()

        _activityState.update { it.copy(sessionActive = true) }
        recalculateTracking()
        return session
    }

    suspend fun restoreSession(sessionId: String): RecoverySession? {
        val session = repository.getSession(sessionId) ?: return null
        _currentSession.value = session

        activeReadingTracker.reset()
        activeReadingTracker.restoreAccumulatedTime(session.accumulatedActiveMs)
        pageQualificationEngine.reset()

        _activityState.update { it.copy(sessionActive = session.status == RecoveryStatus.ACTIVE) }
        recalculateTracking()
        return session
    }

    fun attachExistingSession(session: RecoverySession) {
        _currentSession.value = session
        activeReadingTracker.restoreAccumulatedTime(session.accumulatedActiveMs)
        _activityState.update { it.copy(sessionActive = session.status == RecoveryStatus.ACTIVE) }
        recalculateTracking()
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
        currentBookId = bookId
        _activityState.update { it.copy(documentLoaded = isLoaded) }
        recalculateTracking()
    }

    fun updateReaderVisible(isVisible: Boolean) {
        _activityState.update { it.copy(readerVisible = isVisible) }
        recalculateTracking()
    }

    suspend fun onVisiblePageChanged(bookId: String, pageIndex: Int) {
        val session = _currentSession.value ?: return
        if (session.status != RecoveryStatus.ACTIVE) return

        currentBookId = bookId
        val candidate = pageQualificationEngine.onPageChanged(pageIndex)
        if (candidate is PageQualificationResult.Candidate) {
            creditCandidate(session, bookId, candidate.pageIndex)
        }
    }

    suspend fun performCheckpoint() {
        checkpoint()
    }

    private suspend fun checkpoint() {
        val session = _currentSession.value ?: return
        if (session.status != RecoveryStatus.ACTIVE) return

        val bookId = currentBookId
        if (bookId != null) {
            // Check if current page satisfied dwell without moving
            val dwellCandidate = pageQualificationEngine.checkCurrentPageDwell()
            if (dwellCandidate is PageQualificationResult.Candidate) {
                creditCandidate(session, bookId, dwellCandidate.pageIndex)
            }
        }

        val accumulatedMs = activeReadingTracker.checkpoint()
        repository.updateActiveTime(session.sessionId, accumulatedMs)

        // Reload latest session state and evaluate completion
        val updated = repository.getSession(session.sessionId)
        if (updated != null) {
            _currentSession.value = updated
            evaluateCompletion(updated)
        }
    }

    private suspend fun creditCandidate(
        session: RecoverySession,
        bookId: String,
        pageIndex: Int
    ) {
        val newlyQualified = repository.qualifyPage(session.sessionId, bookId, pageIndex)
        if (newlyQualified) {
            val updated = repository.getSession(session.sessionId)
            if (updated != null) {
                _currentSession.value = updated
                evaluateCompletion(updated)
            }
        }
    }

    private suspend fun evaluateCompletion(session: RecoverySession) {
        if (session.status == RecoveryStatus.COMPLETE) return

        val currentProgress = session.toProgress().copy(
            activeSeconds = activeReadingTracker.totalMs() / 1000L
        )

        if (currentProgress.meets(session.requirement)) {
            val finalActiveMs = activeReadingTracker.totalMs()
            activeReadingTracker.pause()
            repository.updateActiveTime(session.sessionId, finalActiveMs)
            repository.markComplete(session.sessionId)

            val completedSession = repository.getSession(session.sessionId)
            if (completedSession != null) {
                _currentSession.value = completedSession
            }

            _activityState.update { it.copy(sessionActive = false) }
            recalculateTracking()
        }
    }

    private fun recalculateTracking() {
        val qualifies = _activityState.value.qualifiesForActiveTime

        if (qualifies) {
            if (!activeReadingTracker.isTracking()) {
                activeReadingTracker.updateQualification(true)
                pageQualificationEngine.onResume()
                startPeriodicCheckpointing()
            }
        } else {
            if (activeReadingTracker.isTracking()) {
                handlePause()
            }
        }
    }

    private fun handlePause() {
        stopPeriodicCheckpointing()
        val candidate = pageQualificationEngine.onPause()
        activeReadingTracker.pause()
        val session = _currentSession.value

        if (session != null && session.status == RecoveryStatus.ACTIVE) {
            val bookId = currentBookId
            val totalMs = activeReadingTracker.totalMs()
            scope.launch(dispatcher) {
                if (candidate is PageQualificationResult.Candidate && bookId != null) {
                    creditCandidate(session, bookId, candidate.pageIndex)
                }
                repository.updateActiveTime(session.sessionId, totalMs)

                val updated = repository.getSession(session.sessionId)
                if (updated != null) {
                    _currentSession.value = updated
                    evaluateCompletion(updated)
                }
            }
        }
    }

    private fun startPeriodicCheckpointing() {
        if (checkpointJob?.isActive == true) return
        checkpointJob = scope.launch(dispatcher) {
            while (isActive) {
                delay(20_000L) // Periodic 20-second checkpoint
                checkpoint()
            }
        }
    }

    private fun stopPeriodicCheckpointing() {
        checkpointJob?.cancel()
        checkpointJob = null
    }

    fun close() {
        stopPeriodicCheckpointing()
        if (activeReadingTracker.isTracking()) {
            handlePause()
        }
    }

    fun getActiveReadingSeconds(): Long {
        return activeReadingTracker.totalMs() / 1000L
    }
}
