package com.terinit.rhythmicreader.integration.rhythmic

import android.net.Uri
import androidx.core.net.toUri

object DailyEvidenceProtocol {
    const val PROTOCOL_VERSION = 2
    const val AUTHORITY = "com.terinit.rhythmicreader.evidence"
    const val READ_PERMISSION = RecoveryProtocol.PERMISSION_RECOVERY

    const val COLUMN_PROTOCOL_VERSION = "protocolVersion"
    const val COLUMN_DATE_KEY = "dateKey"
    const val COLUMN_VERIFIED_ACTIVE_SECONDS = "verifiedActiveSeconds"
    const val COLUMN_QUALIFIED_PAGES = "qualifiedPages"
    const val COLUMN_UPDATED_AT_EPOCH_MS = "updatedAtEpochMs"

    val ALL_COLUMNS = arrayOf(
        COLUMN_PROTOCOL_VERSION,
        COLUMN_DATE_KEY,
        COLUMN_VERIFIED_ACTIVE_SECONDS,
        COLUMN_QUALIFIED_PAGES,
        COLUMN_UPDATED_AT_EPOCH_MS
    )

    val BASE_CONTENT_URI: Uri = "content://$AUTHORITY".toUri()

    fun dailyUri(dateKey: String): Uri = BASE_CONTENT_URI.buildUpon()
        .appendPath("daily")
        .appendPath(dateKey)
        .build()
}
