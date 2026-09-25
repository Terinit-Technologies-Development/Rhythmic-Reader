package com.terinit.rhythmicreader.domain.recovery

import com.terinit.rhythmicreader.domain.time.MonotonicClock
import com.terinit.rhythmicreader.domain.time.SystemWallClock
import com.terinit.rhythmicreader.domain.time.WallClock

class ActiveReadingTracker(
    private val clock: MonotonicClock,
    private val wallClock: WallClock = SystemWallClock
) {

    private var activeSinceMs: Long? = null
    private var activeSinceEpochMs: Long? = null
    private var accumulatedMs: Long = 0L

    @Synchronized
    fun restoreAccumulatedTime(ms: Long) {
        accumulatedMs = ms.coerceAtLeast(0L)
    }

    @Synchronized
    fun updateQualification(qualifies: Boolean) {
        if (qualifies) {
            startIfNeeded()
        } else {
            pause()
        }
    }

    private fun startIfNeeded() {
        if (activeSinceMs == null) {
            activeSinceMs = clock.nowMs()
            activeSinceEpochMs = wallClock.nowEpochMs()
        }
    }

    @Synchronized
    fun pause() {
        pauseAndGetDelta()
    }

    @Synchronized
    fun pauseAndGetDelta(): ActiveReadingDelta? {
        val delta = readCurrentDelta()
        if (delta != null) {
            accumulatedMs += delta.durationMs
        }
        activeSinceMs = null
        activeSinceEpochMs = null
        return delta
    }

    @Synchronized
    fun isTracking(): Boolean = activeSinceMs != null

    @Synchronized
    fun totalMs(): Long {
        val currentInterval = activeSinceMs?.let {
            (clock.nowMs() - it).coerceAtLeast(0L)
        } ?: 0L
        return accumulatedMs + currentInterval
    }

    /**
     * Rolls open active time into [accumulatedMs] without stopping tracking if currently active.
     * Returns the up-to-date accumulated milliseconds suitable for database persistence.
     */
    @Synchronized
    fun checkpoint(): Long {
        checkpointDelta()
        return accumulatedMs
    }

    /**
     * Commits only the newly elapsed portion of the active interval and keeps tracking open.
     * Each returned delta can be persisted once without replaying earlier checkpoints.
     */
    @Synchronized
    fun checkpointDelta(): ActiveReadingDelta? {
        val delta = readCurrentDelta() ?: return null
        accumulatedMs += delta.durationMs
        activeSinceMs = delta.endedAtMonotonicMs
        activeSinceEpochMs = delta.endedAtEpochMs
        return delta
    }

    private fun readCurrentDelta(): ActiveReadingDelta? {
        val startedAtMonotonicMs = activeSinceMs ?: return null
        val startedAtEpochMs = activeSinceEpochMs ?: return null
        val endedAtMonotonicMs = clock.nowMs()
        val endedAtEpochMs = wallClock.nowEpochMs()
        val durationMs = (endedAtMonotonicMs - startedAtMonotonicMs).coerceAtLeast(0L)
        if (durationMs == 0L) return null
        return ActiveReadingDelta(
            durationMs = durationMs,
            startedAtEpochMs = startedAtEpochMs,
            endedAtEpochMs = endedAtEpochMs,
            endedAtMonotonicMs = endedAtMonotonicMs,
            totalActiveMsAtEnd = accumulatedMs + durationMs
        )
    }

    @Synchronized
    fun reset() {
        activeSinceMs = null
        activeSinceEpochMs = null
        accumulatedMs = 0L
    }
}
