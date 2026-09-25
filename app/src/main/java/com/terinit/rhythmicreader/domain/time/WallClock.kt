package com.terinit.rhythmicreader.domain.time

interface WallClock {
    fun nowEpochMs(): Long
}

object SystemWallClock : WallClock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}
