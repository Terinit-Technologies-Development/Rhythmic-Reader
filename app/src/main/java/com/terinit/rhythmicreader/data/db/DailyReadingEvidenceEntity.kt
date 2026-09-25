package com.terinit.rhythmicreader.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_reading_evidence")
data class DailyReadingEvidenceEntity(
    @PrimaryKey val dateKey: String,
    val verifiedActiveSeconds: Long,
    val updatedAtEpochMs: Long
)
