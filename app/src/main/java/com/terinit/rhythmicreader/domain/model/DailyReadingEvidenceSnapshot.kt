package com.terinit.rhythmicreader.domain.model

data class DailyReadingEvidenceSnapshot(
    val dateKey: String,
    val verifiedActiveSeconds: Long,
    val qualifiedPages: Int,
    val updatedAtEpochMs: Long
) {
    companion object {
        fun empty(dateKey: String) = DailyReadingEvidenceSnapshot(
            dateKey = dateKey,
            verifiedActiveSeconds = 0L,
            qualifiedPages = 0,
            updatedAtEpochMs = 0L
        )
    }
}
