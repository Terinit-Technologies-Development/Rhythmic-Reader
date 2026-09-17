package com.terinit.rhythmicreader.integration.rhythmic

import android.net.Uri

object RecoveryProtocol {
    const val PROTOCOL_VERSION = 1

    const val ACTION_START_RECOVERY = "com.terinit.rhythmicreader.action.START_RECOVERY"
    const val PERMISSION_RECOVERY = "com.terinit.rhythmicreader.permission.RECOVERY"

    const val EXTRA_SESSION_ID = "recovery.session_id"
    const val EXTRA_PROTOCOL_VERSION = "recovery.protocol_version"
    const val EXTRA_REQUIRED_SECONDS = "recovery.required_seconds"
    const val EXTRA_REQUIRED_PAGES = "recovery.required_pages"
    const val EXTRA_CREATED_AT = "recovery.created_at"
    const val EXTRA_EXPIRES_AT = "recovery.expires_at"

    const val AUTHORITY = "com.terinit.rhythmicreader.recovery"
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY")

    const val COLUMN_SESSION_ID = "sessionId"
    const val COLUMN_PROTOCOL_VERSION = "protocolVersion"
    const val COLUMN_STATUS = "status"
    const val COLUMN_ACTIVE_SECONDS = "activeSeconds"
    const val COLUMN_QUALIFIED_PAGES = "qualifiedPages"
    const val COLUMN_COMPLETED_AT = "completedAtEpochMs"

    val ALL_COLUMNS = arrayOf(
        COLUMN_SESSION_ID,
        COLUMN_PROTOCOL_VERSION,
        COLUMN_STATUS,
        COLUMN_ACTIVE_SECONDS,
        COLUMN_QUALIFIED_PAGES,
        COLUMN_COMPLETED_AT
    )

    fun sessionUri(sessionId: String): Uri {
        return BASE_CONTENT_URI.buildUpon()
            .appendPath("sessions")
            .appendPath(sessionId)
            .build()
    }
}
