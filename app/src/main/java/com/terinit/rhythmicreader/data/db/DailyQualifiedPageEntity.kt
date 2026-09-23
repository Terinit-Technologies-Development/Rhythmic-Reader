package com.terinit.rhythmicreader.data.db

import androidx.room.Entity

@Entity(
    tableName = "daily_qualified_pages",
    primaryKeys = ["dateKey", "bookId", "pageIndex"]
)
data class DailyQualifiedPageEntity(
    val dateKey: String,
    val bookId: String,
    val pageIndex: Int,
    val qualifiedAtEpochMs: Long
)
