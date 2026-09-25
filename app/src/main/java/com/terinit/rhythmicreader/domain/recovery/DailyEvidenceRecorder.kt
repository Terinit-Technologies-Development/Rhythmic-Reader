package com.terinit.rhythmicreader.domain.recovery

import com.terinit.rhythmicreader.data.repository.DailyReadingEvidenceRepository
import com.terinit.rhythmicreader.domain.time.LocalDateClock
import com.terinit.rhythmicreader.domain.time.WallClock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ActiveReadingDelta(
    val durationMs: Long,
    val startedAtEpochMs: Long,
    val endedAtEpochMs: Long,
    val endedAtMonotonicMs: Long = 0L,
    val totalActiveMsAtEnd: Long = 0L
)

data class DailyReadingDurationSlice(
    val dateKey: String,
    val durationMs: Long
)

object DailyReadingTimeSlicer {
    fun splitByLocalDate(
        delta: ActiveReadingDelta,
        zoneId: ZoneId
    ): List<DailyReadingDurationSlice> {
        if (delta.durationMs <= 0L) return emptyList()

        val start = delta.startedAtEpochMs
        val end = delta.endedAtEpochMs
        if (end <= start) {
            return listOf(DailyReadingDurationSlice(dateKeyAt(end, zoneId), delta.durationMs))
        }

        val wallDuration = end - start
        val slices = mutableListOf<DailyReadingDurationSlice>()
        var cursor = start
        var assignedDuration = 0L

        while (cursor < end) {
            val date = Instant.ofEpochMilli(cursor).atZone(zoneId).toLocalDate()
            val nextMidnight = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val sliceEnd = minOf(end, nextMidnight)
            val isLastSlice = sliceEnd == end
            val duration = if (isLastSlice) {
                delta.durationMs - assignedDuration
            } else {
                delta.durationMs * (sliceEnd - cursor) / wallDuration
            }

            slices += DailyReadingDurationSlice(
                dateKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                durationMs = duration
            )
            assignedDuration += duration
            cursor = sliceEnd
        }

        return slices
    }

    private fun dateKeyAt(epochMs: Long, zoneId: ZoneId): String =
        Instant.ofEpochMilli(epochMs)
            .atZone(zoneId)
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
}

class DailyEvidenceRecorder(
    private val repository: DailyReadingEvidenceRepository,
    private val localDateClock: LocalDateClock,
    private val wallClock: WallClock
) {
    private val subsecondRemaindersMs = mutableMapOf<String, Long>()

    suspend fun recordVerifiedReading(delta: ActiveReadingDelta) {
        val slices = DailyReadingTimeSlicer.splitByLocalDate(delta, localDateClock.zoneId())
        for (slice in slices) {
            val combinedMs = subsecondRemaindersMs.getOrDefault(slice.dateKey, 0L) + slice.durationMs
            val wholeSeconds = combinedMs / 1_000L
            subsecondRemaindersMs[slice.dateKey] = combinedMs % 1_000L
            repository.addVerifiedActiveSeconds(
                dateKey = slice.dateKey,
                seconds = wholeSeconds,
                updatedAtEpochMs = delta.endedAtEpochMs
            )
        }
    }

    suspend fun recordQualifiedPage(
        bookId: String,
        pageIndex: Int,
        qualifiedAtEpochMs: Long
    ): Boolean {
        return repository.recordQualifiedPage(
            dateKey = localDateClock.dateKeyAt(qualifiedAtEpochMs),
            bookId = bookId,
            pageIndex = pageIndex,
            qualifiedAtEpochMs = qualifiedAtEpochMs
        )
    }

    suspend fun ensureToday() {
        val now = localDateClock.nowEpochMs()
        repository.ensureDay(localDateClock.dateKeyAt(now), now)
    }
}
