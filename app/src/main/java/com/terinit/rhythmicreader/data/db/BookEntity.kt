package com.terinit.rhythmicreader.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "books",
    indices = [
        Index(
            value = ["documentUri"],
            unique = true
        )
    ]
)
data class BookEntity(
    @PrimaryKey
    val id: String,

    val displayName: String,

    val documentUri: String,

    val totalPages: Int?,

    val lastPageIndex: Int = 0,

    val importedAtEpochMs: Long,

    val lastOpenedAtEpochMs: Long? = null
)
