package com.terinit.rhythmicreader.domain

import com.terinit.rhythmicreader.domain.recovery.ActiveReadingDelta
import com.terinit.rhythmicreader.domain.recovery.DailyReadingTimeSlicer
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DailyReadingTimeSlicerTest {

    @Test
    fun checkpointSpanningLocalMidnightIsSplitAcrossBothDateLedgers() {
        val start = Instant.parse("2026-09-22T23:59:50Z").toEpochMilli()
        val end = Instant.parse("2026-09-23T00:00:20Z").toEpochMilli()

        val slices = DailyReadingTimeSlicer.splitByLocalDate(
            ActiveReadingDelta(durationMs = 30_000L, startedAtEpochMs = start, endedAtEpochMs = end),
            ZoneId.of("UTC")
        )

        assertEquals(2, slices.size)
        assertEquals("2026-09-22", slices[0].dateKey)
        assertEquals(10_000L, slices[0].durationMs)
        assertEquals("2026-09-23", slices[1].dateKey)
        assertEquals(20_000L, slices[1].durationMs)
        assertEquals(30_000L, slices.sumOf { it.durationMs })
    }

    @Test
    fun daylightSavingDayUsesDeviceLocalCalendarBoundary() {
        val zone = ZoneId.of("America/New_York")
        val start = Instant.parse("2026-03-08T04:59:50Z").toEpochMilli()
        val end = Instant.parse("2026-03-08T07:00:10Z").toEpochMilli()
        val elapsed = end - start

        val slices = DailyReadingTimeSlicer.splitByLocalDate(
            ActiveReadingDelta(elapsed, start, end),
            zone
        )

        assertEquals(listOf("2026-03-07", "2026-03-08"), slices.map { it.dateKey })
        assertEquals(elapsed, slices.sumOf { it.durationMs })
    }
}
