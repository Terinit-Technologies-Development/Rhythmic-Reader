package com.terinit.rhythmicreader.data.db

import androidx.room.Entity

@Entity(
    tableName = "qualified_pages",
    primaryKeys = [
        "sessionId",
        "bookId",
        "pageIndex"
    ]
)
data class QualifiedPageEntity(
    val sessionId: String,
    val bookId: String,
    val pageIndex: Int,
    val qualifiedAtEpochMs: Long
)
