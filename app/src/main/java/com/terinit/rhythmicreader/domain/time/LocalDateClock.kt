package com.terinit.rhythmicreader.domain.time

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface LocalDateClock {
    fun nowEpochMs(): Long
    fun todayDateKey(): String
    fun dateKeyAt(epochMs: Long): String
    fun zoneId(): ZoneId
}

class DeviceLocalDateClock(
    private val wallClock: WallClock = SystemWallClock,
    private val zoneProvider: () -> ZoneId = ZoneId::systemDefault
) : LocalDateClock {
    override fun nowEpochMs(): Long = wallClock.nowEpochMs()

    override fun todayDateKey(): String = dateKeyAt(nowEpochMs())

    override fun dateKeyAt(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs)
            .atZone(zoneId())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

    override fun zoneId(): ZoneId = zoneProvider()
}
