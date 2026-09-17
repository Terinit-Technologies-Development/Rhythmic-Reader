package com.terinit.rhythmicreader.test

import com.terinit.rhythmicreader.domain.time.MonotonicClock

class FakeMonotonicClock(
    private var currentMs: Long = 0L
) : MonotonicClock {

    override fun nowMs(): Long = currentMs

    fun advanceBy(ms: Long) {
        require(ms >= 0) { "Cannot advance backwards in time: $ms" }
        currentMs += ms
    }

    fun set(ms: Long) {
        currentMs = ms
    }
}
