package com.terinit.rhythmicreader.integration.rhythmic

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import com.terinit.rhythmicreader.domain.model.RoutineReadingTargetPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface RoutineAttentionPreviewClient {
    suspend fun queryNextTarget(dateKey: String): RoutineReadingTargetPreview?
}

class AndroidRoutineAttentionPreviewClient(
    private val contentResolver: ContentResolver,
    private val authorities: List<String> = RoutineAttentionPreviewProtocol.AUTHORITIES,
) : RoutineAttentionPreviewClient {

    override suspend fun queryNextTarget(dateKey: String): RoutineReadingTargetPreview? =
        withContext(Dispatchers.IO) {
            if (!RoutineAttentionPreviewProtocol.isValidDateKey(dateKey)) return@withContext null

            for (authority in authorities) {
                val cursor = try {
                    contentResolver.query(
                        RoutineAttentionPreviewProtocol.nextTargetUri(authority, dateKey),
                        RoutineAttentionPreviewProtocol.ALL_COLUMNS,
                        null,
                        null,
                        null,
                    )
                } catch (_: RuntimeException) {
                    null
                } ?: continue

                if (!cursor.moveToFirst()) {
                    cursor.close()
                    continue
                }

                val preview = cursor.use { RoutineAttentionPreviewProtocol.parsePreview(it, dateKey) }
                if (preview != null) return@withContext preview
            }
            null
        }
}

object RoutineAttentionPreviewProtocol {
    const val PROTOCOL_VERSION = 1
    const val PATH_NEXT = "next"

    const val COLUMN_PROTOCOL_VERSION = "protocolVersion"
    const val COLUMN_DATE_KEY = "dateKey"
    const val COLUMN_NEXT_COOLDOWN_ORDINAL = "nextCooldownOrdinal"
    const val COLUMN_REQUIRED_ACTIVE_SECONDS = "requiredActiveSeconds"
    const val COLUMN_REQUIRED_QUALIFIED_PAGES = "requiredQualifiedPages"

    val ALL_COLUMNS = arrayOf(
        COLUMN_PROTOCOL_VERSION,
        COLUMN_DATE_KEY,
        COLUMN_NEXT_COOLDOWN_ORDINAL,
        COLUMN_REQUIRED_ACTIVE_SECONDS,
        COLUMN_REQUIRED_QUALIFIED_PAGES,
    )

    val AUTHORITIES = listOf(
        "com.terinit.rhythmicroutine.qa.attention-preview",
        "com.terinit.rhythmicroutine.attention-preview",
    )

    fun nextTargetUri(authority: String, dateKey: String): Uri =
        Uri.Builder()
            .scheme("content")
            .authority(authority)
            .appendPath(PATH_NEXT)
            .appendPath(dateKey)
            .build()

    fun isValidDateKey(dateKey: String): Boolean = runCatching {
        java.time.LocalDate.parse(dateKey).toString() == dateKey
    }.getOrDefault(false)

    fun parsePreview(cursor: Cursor, expectedDateKey: String): RoutineReadingTargetPreview? =
        runCatching {
            val protocolVersion = cursor.getInt(
                cursor.getColumnIndexOrThrow(COLUMN_PROTOCOL_VERSION)
            )
            val returnedDateKey = cursor.getString(
                cursor.getColumnIndexOrThrow(COLUMN_DATE_KEY)
            )
            val ordinal = cursor.getInt(
                cursor.getColumnIndexOrThrow(COLUMN_NEXT_COOLDOWN_ORDINAL)
            )
            val requiredSeconds = cursor.getLong(
                cursor.getColumnIndexOrThrow(COLUMN_REQUIRED_ACTIVE_SECONDS)
            )
            val requiredPages = cursor.getInt(
                cursor.getColumnIndexOrThrow(COLUMN_REQUIRED_QUALIFIED_PAGES)
            )

            if (protocolVersion != PROTOCOL_VERSION || returnedDateKey != expectedDateKey ||
                ordinal <= 0 || requiredSeconds < 0L || requiredPages < 0
            ) {
                null
            } else {
                RoutineReadingTargetPreview(
                    dateKey = returnedDateKey,
                    nextCooldownOrdinal = ordinal,
                    requiredActiveSeconds = requiredSeconds,
                    requiredQualifiedPages = requiredPages,
                )
            }
        }.getOrNull()
}
