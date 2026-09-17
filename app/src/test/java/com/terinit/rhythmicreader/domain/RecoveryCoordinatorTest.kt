package com.terinit.rhythmicreader.domain

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.terinit.rhythmicreader.data.db.ReaderDatabase
import com.terinit.rhythmicreader.data.repository.DefaultRecoveryRepository
import com.terinit.rhythmicreader.data.repository.RecoveryRepository
import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import com.terinit.rhythmicreader.domain.recovery.ActiveReadingTracker
import com.terinit.rhythmicreader.domain.recovery.PageQualificationEngine
import com.terinit.rhythmicreader.domain.recovery.QualificationPolicy
import com.terinit.rhythmicreader.domain.recovery.RecoveryCoordinator
import com.terinit.rhythmicreader.test.FakeMonotonicClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RecoveryCoordinatorTest {

    private lateinit var db: ReaderDatabase
    private lateinit var repository: RecoveryRepository
    private lateinit var clock: FakeMonotonicClock
    private lateinit var tracker: ActiveReadingTracker
    private lateinit var engine: PageQualificationEngine
    private lateinit var testScope: CoroutineScope
    private lateinit var coordinator: RecoveryCoordinator

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DefaultRecoveryRepository(db.recoveryDao())
        clock = FakeMonotonicClock(100_000L)
        tracker = ActiveReadingTracker(clock)
        engine = PageQualificationEngine(
            clock = clock,
            policy = QualificationPolicy(
                minimumPageDwellMs = 15_000L,
                rapidFlipWindowMs = 10_000L,
                rapidFlipThreshold = 4
            )
        )
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        coordinator = RecoveryCoordinator(
            repository = repository,
            activeReadingTracker = tracker,
            pageQualificationEngine = engine,
            scope = testScope,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    private fun setFullyQualifyingState(bookId: String = "book-1") {
        coordinator.updateAppForeground(true)
        coordinator.updateScreenInteractive(true)
        coordinator.updateDocumentLoaded(true, bookId)
        coordinator.updateReaderVisible(true)
    }

    @Test
    fun activeReadingTime_onlyAccumulatesWhenAllConditionsTrue() = runBlocking {
        coordinator.startSession(
            RecoveryRequirement(requiredActiveSeconds = 60, requiredQualifiedPages = 2),
            sessionId = "session-test-1"
        )

        // Initial state: not fully qualifying
        clock.advanceBy(10_000L)
        assertEquals(0L, coordinator.getActiveReadingSeconds())

        // Make fully qualifying
        setFullyQualifyingState()
        clock.advanceBy(20_000L)
        assertEquals(20L, coordinator.getActiveReadingSeconds())

        // App backgrounded
        coordinator.updateAppForeground(false)
        clock.advanceBy(15_000L) // Should NOT count
        assertEquals(20L, coordinator.getActiveReadingSeconds())

        // App foregrounded again
        coordinator.updateAppForeground(true)
        clock.advanceBy(10_000L)
        assertEquals(30L, coordinator.getActiveReadingSeconds())

        // Screen turned off
        coordinator.updateScreenInteractive(false)
        clock.advanceBy(30_000L) // Should NOT count
        assertEquals(30L, coordinator.getActiveReadingSeconds())

        // Screen turned back on
        coordinator.updateScreenInteractive(true)
        clock.advanceBy(10_000L)
        assertEquals(40L, coordinator.getActiveReadingSeconds())

        // Reader hidden (e.g. navigated to library or settings)
        coordinator.updateReaderVisible(false)
        clock.advanceBy(25_000L) // Should NOT count
        assertEquals(40L, coordinator.getActiveReadingSeconds())

        // Reader visible again
        coordinator.updateReaderVisible(true)
        clock.advanceBy(5_000L)
        assertEquals(45L, coordinator.getActiveReadingSeconds())

        // Document unavailable/unloaded
        coordinator.updateDocumentLoaded(false, null)
        clock.advanceBy(50_000L) // Should NOT count
        assertEquals(45L, coordinator.getActiveReadingSeconds())
    }

    @Test
    fun dualThresholdCompletion_requiresBothTimeAndPages() = runBlocking {
        // Requirement: 30 seconds active reading AND 2 qualified pages
        val session = coordinator.startSession(
            RecoveryRequirement(requiredActiveSeconds = 30, requiredQualifiedPages = 2),
            sessionId = "session-dual"
        )
        setFullyQualifyingState("book-alpha")

        // 1. Dwell on page 1 for 15 seconds (qualifies page 1)
        coordinator.onVisiblePageChanged("book-alpha", 1)
        clock.advanceBy(15_000L)
        coordinator.performCheckpoint()

        var current = repository.getSession("session-dual")
        assertEquals(1, current?.qualifiedPages)
        assertEquals(15L, coordinator.getActiveReadingSeconds())
        assertEquals(RecoveryStatus.ACTIVE, current?.status)

        // 2. Dwell on page 2 for 15 seconds (qualifies page 2)
        coordinator.onVisiblePageChanged("book-alpha", 2)
        clock.advanceBy(15_000L)
        coordinator.performCheckpoint()

        current = repository.getSession("session-dual")
        assertEquals(2, current?.qualifiedPages)
        assertEquals(30L, coordinator.getActiveReadingSeconds())
        // Both conditions met!
        assertEquals(RecoveryStatus.COMPLETE, current?.status)
        assertNotNull(current?.completedAtEpochMs)
    }

    @Test
    fun completionIsIdempotent_completedAtNotOverwritten() = runBlocking {
        coordinator.startSession(
            RecoveryRequirement(requiredActiveSeconds = 15, requiredQualifiedPages = 1),
            sessionId = "session-idempotent"
        )
        setFullyQualifyingState("book-beta")

        coordinator.onVisiblePageChanged("book-beta", 1)
        clock.advanceBy(15_000L)
        coordinator.performCheckpoint()

        val completedSession = repository.getSession("session-idempotent")
        assertEquals(RecoveryStatus.COMPLETE, completedSession?.status)
        val originalCompletedAt = completedSession?.completedAtEpochMs
        assertNotNull(originalCompletedAt)

        // Advance time and flip more pages
        clock.advanceBy(30_000L)
        coordinator.onVisiblePageChanged("book-beta", 2)
        coordinator.performCheckpoint()

        val recheckedSession = repository.getSession("session-idempotent")
        assertEquals(RecoveryStatus.COMPLETE, recheckedSession?.status)
        assertEquals(originalCompletedAt, recheckedSession?.completedAtEpochMs)
    }

    @Test
    fun repeatedPageFarming_doesNotIncreaseQualifiedCount() = runBlocking {
        coordinator.startSession(
            RecoveryRequirement(requiredActiveSeconds = 300, requiredQualifiedPages = 5),
            sessionId = "session-farming"
        )
        setFullyQualifyingState("book-gamma")

        // Qualify page 1
        coordinator.onVisiblePageChanged("book-gamma", 1)
        clock.advanceBy(16_000L)
        coordinator.performCheckpoint()
        assertEquals(1, repository.getSession("session-farming")?.qualifiedPages)

        // Flip to page 2 and qualify it
        coordinator.onVisiblePageChanged("book-gamma", 2)
        clock.advanceBy(16_000L)
        coordinator.performCheckpoint()
        assertEquals(2, repository.getSession("session-farming")?.qualifiedPages)

        // Revisit page 1 (repeated page farming) and dwell 20 seconds
        coordinator.onVisiblePageChanged("book-gamma", 1)
        clock.advanceBy(20_000L)
        coordinator.performCheckpoint()
        // Must remain 2!
        assertEquals(2, repository.getSession("session-farming")?.qualifiedPages)
    }

    @Test
    fun processDeathAndRecreation_restoresPersistedTimeAndPagesWithoutBackgroundCredit() = runBlocking {
        coordinator.startSession(
            RecoveryRequirement(requiredActiveSeconds = 120, requiredQualifiedPages = 3),
            sessionId = "session-recreation"
        )
        setFullyQualifyingState("book-delta")

        // Read for 40 seconds on page 1
        coordinator.onVisiblePageChanged("book-delta", 1)
        clock.advanceBy(40_000L)
        coordinator.performCheckpoint()

        val beforeDeath = repository.getSession("session-recreation")
        assertEquals(40_000L, beforeDeath?.accumulatedActiveMs)
        assertEquals(1, beforeDeath?.qualifiedPages)

        // Simulate process death: app closes, 10 minutes pass offline
        coordinator.close()
        clock.advanceBy(600_000L)

        // New Coordinator instantiated in new process
        val newClock = FakeMonotonicClock(clock.nowMs())
        val newTracker = ActiveReadingTracker(newClock)
        val newEngine = PageQualificationEngine(newClock)
        val newCoordinator = RecoveryCoordinator(
            repository = repository,
            activeReadingTracker = newTracker,
            pageQualificationEngine = newEngine,
            scope = testScope,
            dispatcher = Dispatchers.Unconfined
        )

        // Restore session from DB
        val restored = newCoordinator.restoreSession("session-recreation")
        assertNotNull(restored)
        // Persisted time must be exactly 40s (NO offline 10 minutes added!)
        assertEquals(40L, newCoordinator.getActiveReadingSeconds())
        assertEquals(1, restored?.qualifiedPages)

        // Resume reading on new process
        newCoordinator.updateAppForeground(true)
        newCoordinator.updateScreenInteractive(true)
        newCoordinator.updateDocumentLoaded(true, "book-delta")
        newCoordinator.updateReaderVisible(true)

        newClock.advanceBy(20_000L)
        assertEquals(60L, newCoordinator.getActiveReadingSeconds())
    }
}
