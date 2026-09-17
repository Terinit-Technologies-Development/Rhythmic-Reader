package com.terinit.rhythmicreader.domain.recovery

import com.terinit.rhythmicreader.domain.time.MonotonicClock

class ActiveReadingTracker(
    private val clock: MonotonicClock
) {

    private var activeSinceMs: Long? = null
    private var accumulatedMs: Long = 0L

    fun restoreAccumulatedTime(ms: Long) {
        accumulatedMs = ms.coerceAtLeast(0L)
    }

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
        }
    }

    fun pause() {
        val started = activeSinceMs ?: return
        accumulatedMs += (clock.nowMs() - started).coerceAtLeast(0L)
        activeSinceMs = null
    }

    fun isTracking(): Boolean = activeSinceMs != null

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
    fun checkpoint(): Long {
        val started = activeSinceMs
        if (started != null) {
            val now = clock.nowMs()
            accumulatedMs += (now - started).coerceAtLeast(0L)
            activeSinceMs = now
        }
        return accumulatedMs
    }

    fun reset() {
        activeSinceMs = null
        accumulatedMs = 0L
    }
}
