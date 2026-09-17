package com.terinit.rhythmicreader.domain.time

import android.os.SystemClock

fun interface MonotonicClock {
    fun nowMs(): Long
}

object AndroidMonotonicClock : MonotonicClock {
    override fun nowMs(): Long {
        return SystemClock.elapsedRealtime()
    }
}
