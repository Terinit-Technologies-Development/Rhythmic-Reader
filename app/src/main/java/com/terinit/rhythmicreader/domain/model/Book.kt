package com.terinit.rhythmicreader.domain.model

import android.net.Uri
import com.terinit.rhythmicreader.data.db.BookEntity

data class Book(
    val id: String,
    val displayName: String,
    val documentUri: Uri,
    val totalPages: Int?,
    val lastPageIndex: Int = 0,
    val importedAtEpochMs: Long,
    val lastOpenedAtEpochMs: Long? = null
) {
    val progressPercentage: Int
        get() = if (totalPages != null && totalPages > 0) {
            ((lastPageIndex + 1).toFloat() / totalPages * 100).toInt().coerceIn(0, 100)
        } else 0
}

fun BookEntity.toDomain(): Book = Book(
    id = id,
    displayName = displayName,
    documentUri = Uri.parse(documentUri),
    totalPages = totalPages,
    lastPageIndex = lastPageIndex,
    importedAtEpochMs = importedAtEpochMs,
    lastOpenedAtEpochMs = lastOpenedAtEpochMs
)

fun Book.toEntity(): BookEntity = BookEntity(
    id = id,
    displayName = displayName,
    documentUri = documentUri.toString(),
    totalPages = totalPages,
    lastPageIndex = lastPageIndex,
    importedAtEpochMs = importedAtEpochMs,
    lastOpenedAtEpochMs = lastOpenedAtEpochMs
)
