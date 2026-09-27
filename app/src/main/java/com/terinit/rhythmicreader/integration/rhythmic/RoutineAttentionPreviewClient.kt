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

/**
 * Queries Rhythmic Routine's read-only preview provider.
 *
 * Protocol V2 (Pass 4) is additive: the V1 columns keep their exact meaning
 * and the restorative model arrives in new columns. A V1 Routine answers only
 * the V1 projection, so the client first asks for the full V2 column set and
 * falls back to the V1 projection when that yields nothing. Unknown protocol
 * versions are treated as "no target" — never a crash. Reader stays
 * policy-agnostic: it renders whatever requirement Routine publishes.
 */
class AndroidRoutineAttentionPreviewClient(
    private val contentResolver: ContentResolver,
    private val authorities: List<String> = RoutineAttentionPreviewProtocol.AUTHORITIES,
) : RoutineAttentionPreviewClient {

    override suspend fun queryNextTarget(dateKey: String): RoutineReadingTargetPreview? =
        withContext(Dispatchers.IO) {
            if (!RoutineAttentionPreviewProtocol.isValidDateKey(dateKey)) return@withContext null

            for (authority in authorities) {
                // Pass 4: full V2 projection first…
                val v2 = queryProjection(
                    authority,
                    dateKey,
                    RoutineAttentionPreviewProtocol.ALL_COLUMNS,
                )
                if (v2 != null) return@withContext v2

                // …then the V1 projection for a v1.2 Routine (graceful fallback).
                val v1 = queryProjection(
                    authority,
                    dateKey,
                    RoutineAttentionPreviewProtocol.V1_COLUMNS,
                )
                if (v1 != null) return@withContext v1
            }
            null
        }

    private fun queryProjection(
        authority: String,
        dateKey: String,
        projection: Array<String>,
    ): RoutineReadingTargetPreview? {
        val cursor = try {
            contentResolver.query(
                RoutineAttentionPreviewProtocol.nextTargetUri(authority, dateKey),
                projection,
                null,
                null,
                null,
            )
        } catch (_: RuntimeException) {
            null
        } ?: return null

        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        return cursor.use { RoutineAttentionPreviewProtocol.parsePreview(it, dateKey) }
    }
}

object RoutineAttentionPreviewProtocol {
    const val PROTOCOL_VERSION = 2
    const val SUPPORTED_V1_PROTOCOL_VERSION = 1
    const val PATH_NEXT = "next"

    const val COLUMN_PROTOCOL_VERSION = "protocolVersion"
    const val COLUMN_DATE_KEY = "dateKey"
    const val COLUMN_NEXT_COOLDOWN_ORDINAL = "nextCooldownOrdinal"
    const val COLUMN_REQUIRED_ACTIVE_SECONDS = "requiredActiveSeconds"
    const val COLUMN_REQUIRED_QUALIFIED_PAGES = "requiredQualifiedPages"

    // Pass 4 — restorative model projection (additive).
    const val COLUMN_REQUIREMENT_KIND = "requirementKind"
    const val COLUMN_GATE_STATUS = "gateStatus"
    const val COLUMN_SELECTED_PROVIDER = "selectedProvider"
    const val COLUMN_COOLDOWN_ACTIVE = "cooldownActive"
    const val COLUMN_RESTORATIVE_READING_SECONDS = "restorativeReadingSeconds"
    const val COLUMN_RESTORATIVE_QUALIFIED_PAGES = "restorativeQualifiedPages"

    val V1_COLUMNS = arrayOf(
        COLUMN_PROTOCOL_VERSION,
        COLUMN_DATE_KEY,
        COLUMN_NEXT_COOLDOWN_ORDINAL,
        COLUMN_REQUIRED_ACTIVE_SECONDS,
        COLUMN_REQUIRED_QUALIFIED_PAGES,
    )

    val ALL_COLUMNS = V1_COLUMNS + arrayOf(
        COLUMN_REQUIREMENT_KIND,
        COLUMN_GATE_STATUS,
        COLUMN_SELECTED_PROVIDER,
        COLUMN_COOLDOWN_ACTIVE,
        COLUMN_RESTORATIVE_READING_SECONDS,
        COLUMN_RESTORATIVE_QUALIFIED_PAGES,
    )

    val AUTHORITIES = listOf(
        "com.terinit.rhythmicroutine.qa.attention-preview",
        "com.terinit.rhythmicroutine.attention-preview",
    )

    /** Requirement kinds published by Routine's restorative model. */
    const val KIND_NONE = "none"
    const val KIND_BASELINE_READING = "baseline-reading"
    const val KIND_RESTORATIVE_CHOICE = "restorative-choice"
    const val KIND_LEGACY_READING = "legacy-reading"

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

    /**
     * Parses a preview row. Explicit version handling:
     * - protocol 2 -> full restorative model
     * - protocol 1 -> V1 fields with neutral restorative defaults
     * - anything else -> null (graceful "no target", never a crash)
     */
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

            if (returnedDateKey != expectedDateKey || ordinal <= 0 ||
                requiredSeconds < 0L || requiredPages < 0
            ) {
                return@runCatching null
            }

            when (protocolVersion) {
                PROTOCOL_VERSION -> RoutineReadingTargetPreview(
                    dateKey = returnedDateKey,
                    nextCooldownOrdinal = ordinal,
                    requiredActiveSeconds = requiredSeconds,
                    requiredQualifiedPages = requiredPages,
                    requirementKind = stringColumn(cursor, COLUMN_REQUIREMENT_KIND)
                        ?: KIND_NONE,
                    gateStatus = stringColumn(cursor, COLUMN_GATE_STATUS) ?: "none",
                    selectedProvider = stringColumn(cursor, COLUMN_SELECTED_PROVIDER) ?: "none",
                    cooldownActive = intColumn(cursor, COLUMN_COOLDOWN_ACTIVE) == 1,
                    restorativeReadingSeconds = longColumn(cursor, COLUMN_RESTORATIVE_READING_SECONDS)
                        ?: 0L,
                    restorativeQualifiedPages = intColumn(cursor, COLUMN_RESTORATIVE_QUALIFIED_PAGES)
                        ?: 0,
                    protocolVersion = PROTOCOL_VERSION,
                )
                SUPPORTED_V1_PROTOCOL_VERSION -> RoutineReadingTargetPreview(
                    dateKey = returnedDateKey,
                    nextCooldownOrdinal = ordinal,
                    requiredActiveSeconds = requiredSeconds,
                    requiredQualifiedPages = requiredPages,
                    protocolVersion = SUPPORTED_V1_PROTOCOL_VERSION,
                )
                else -> null
            }
        }.getOrNull()

    private fun stringColumn(cursor: Cursor, name: String): String? =
        cursor.getColumnIndex(name).takeIf { it >= 0 }?.let { cursor.getString(it) }

    private fun intColumn(cursor: Cursor, name: String): Int? =
        cursor.getColumnIndex(name).takeIf { it >= 0 }?.let { cursor.getInt(it) }

    private fun longColumn(cursor: Cursor, name: String): Long? =
        cursor.getColumnIndex(name).takeIf { it >= 0 }?.let { cursor.getLong(it) }
}
