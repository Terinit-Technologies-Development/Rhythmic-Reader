package com.terinit.rhythmicreader.data.documents

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

fun ContentResolver.getDisplayName(
    uri: Uri
): String {
    try {
        query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                val name = cursor.getString(index)
                if (!name.isNullOrBlank()) {
                    return name
                }
            }
        }
    } catch (_: Exception) {
        // Ignored, fallback below
    }

    return uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled PDF"
}

fun ContentResolver.getFileSize(
    uri: Uri
): Long? {
    return try {
        query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getLong(index)
            } else null
        }
    } catch (_: Exception) {
        null
    }
}

fun ContentResolver.isDocumentAccessible(
    uri: Uri
): Boolean {
    return try {
        openFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (_: Exception) {
        false
    }
}
