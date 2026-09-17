package com.terinit.rhythmicreader.domain

import com.terinit.rhythmicreader.domain.recovery.ActiveReadingTracker
import com.terinit.rhythmicreader.test.FakeMonotonicClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActiveReadingTrackerTest {

    private lateinit var clock: FakeMonotonicClock
    private lateinit var tracker: ActiveReadingTracker

    @Before
    fun setup() {
        clock = FakeMonotonicClock(10_000L)
        tracker = ActiveReadingTracker(clock)
    }

    @Test
    fun startsAtZero() {
        assertEquals(0L, tracker.totalMs())
        assertFalse(tracker.isTracking())
    }

    @Test
    fun qualifyingState_startsTimer() {
        tracker.updateQualification(true)
        assertTrue(tracker.isTracking())

        clock.advanceBy(5_000L)
        assertEquals(5_000L, tracker.totalMs())
    }

    @Test
    fun nonQualifyingState_pausesTimer() {
        tracker.updateQualification(true)
        clock.advanceBy(4_000L)

        tracker.updateQualification(false)
        assertFalse(tracker.isTracking())
        assertEquals(4_000L, tracker.totalMs())

        clock.advanceBy(10_000L)
        // Time while paused must NOT accumulate
        assertEquals(4_000L, tracker.totalMs())
    }

    @Test
    fun pausedInterval_doesNotAccumulate() {
        tracker.updateQualification(true)
        clock.advanceBy(3_000L)
        tracker.pause()

        clock.advanceBy(100_000L)
        assertEquals(3_000L, tracker.totalMs())
    }

    @Test
    fun resume_addsNewActiveInterval() {
        tracker.updateQualification(true)
        clock.advanceBy(3_000L)
        tracker.updateQualification(false)

        clock.advanceBy(5_000L) // Paused time

        tracker.updateQualification(true)
        clock.advanceBy(7_000L)

        assertEquals(10_000L, tracker.totalMs())
    }

    @Test
    fun restoredPersistedTime_isRetained() {
        tracker.restoreAccumulatedTime(45_000L)
        assertEquals(45_000L, tracker.totalMs())

        tracker.updateQualification(true)
        clock.advanceBy(5_000L)
        assertEquals(50_000L, tracker.totalMs())
    }

    @Test
    fun checkpoint_preservesAccumulatedMsWithoutDisruptingActiveTracking() {
        tracker.updateQualification(true)
        clock.advanceBy(15_000L)

        val checkpointed = tracker.checkpoint()
        assertEquals(15_000L, checkpointed)
        assertTrue(tracker.isTracking())

        clock.advanceBy(5_000L)
        assertEquals(20_000L, tracker.totalMs())
    }
}
