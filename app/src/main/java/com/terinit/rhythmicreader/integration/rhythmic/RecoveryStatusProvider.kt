package com.terinit.rhythmicreader.integration.rhythmic

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.terinit.rhythmicreader.app.RhythmicReaderApplication
import kotlinx.coroutines.runBlocking

class RecoveryStatusProvider : ContentProvider() {

    companion object {
        private const val SESSIONS_ID = 1

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(RecoveryProtocol.AUTHORITY, "sessions/*", SESSIONS_ID)
        }
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val match = uriMatcher.match(uri)
        if (match != SESSIONS_ID) {
            return null
        }

        val sessionId = uri.lastPathSegment ?: return null
        val app = context?.applicationContext as? RhythmicReaderApplication ?: return null
        val repo = app.container.recoveryRepository

        val session = runBlocking {
            repo.getSession(sessionId)
        }

        val requestedColumns = projection ?: RecoveryProtocol.ALL_COLUMNS
        val cursor = MatrixCursor(requestedColumns)

        if (session != null) {
            val rowValues: Array<Any?> = requestedColumns.map { col ->
                when (col) {
                    RecoveryProtocol.COLUMN_SESSION_ID -> session.sessionId
                    RecoveryProtocol.COLUMN_PROTOCOL_VERSION -> session.protocolVersion
                    RecoveryProtocol.COLUMN_STATUS -> session.status.name
                    RecoveryProtocol.COLUMN_ACTIVE_SECONDS -> session.activeSeconds.toInt()
                    RecoveryProtocol.COLUMN_QUALIFIED_PAGES -> session.qualifiedPages
                    RecoveryProtocol.COLUMN_COMPLETED_AT -> session.completedAtEpochMs ?: 0L
                    else -> null
                }
            }.toTypedArray<Any?>()
            cursor.addRow(rowValues)
        }

        context?.contentResolver?.let { cr ->
            cursor.setNotificationUri(cr, uri)
        }

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            SESSIONS_ID -> "vnd.android.cursor.item/vnd.${RecoveryProtocol.AUTHORITY}.session"
            else -> null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("RecoveryStatusProvider is read-only")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("RecoveryStatusProvider is read-only")
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("RecoveryStatusProvider is read-only")
    }
}
