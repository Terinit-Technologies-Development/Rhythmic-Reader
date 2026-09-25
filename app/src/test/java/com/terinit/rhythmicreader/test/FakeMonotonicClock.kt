package com.terinit.rhythmicreader.test

import com.terinit.rhythmicreader.domain.time.MonotonicClock
import com.terinit.rhythmicreader.domain.time.WallClock

class FakeMonotonicClock(
    private var currentMs: Long = 0L
) : MonotonicClock, WallClock {

    override fun nowMs(): Long = currentMs

    override fun nowEpochMs(): Long = currentMs

    fun advanceBy(ms: Long) {
        require(ms >= 0) { "Cannot advance backwards in time: $ms" }
        currentMs += ms
    }

    fun set(ms: Long) {
        currentMs = ms
    }
}
