package com.terinit.rhythmicreader.integration.rhythmic

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.terinit.rhythmicreader.app.RhythmicReaderApplication
import com.terinit.rhythmicreader.domain.model.DailyReadingEvidenceSnapshot
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

class DailyEvidenceProvider : ContentProvider() {

    companion object {
        private const val DAILY_BY_DATE = 1
        private val DATE_KEY_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}")
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(DailyEvidenceProtocol.AUTHORITY, "daily/*", DAILY_BY_DATE)
        }
        private val allowedColumns = DailyEvidenceProtocol.ALL_COLUMNS.toSet()
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val dateKey = validatedDateKey(uri) ?: return null
        if (uriMatcher.match(uri) != DAILY_BY_DATE ||
            selection != null || selectionArgs != null || sortOrder != null
        ) {
            return null
        }

        val columns = projection?.toList() ?: DailyEvidenceProtocol.ALL_COLUMNS.toList()
        if (columns.any { it !in allowedColumns } || columns.distinct().size != columns.size) {
            return null
        }

        val app = context?.applicationContext as? RhythmicReaderApplication ?: return null
        val snapshot = runCatching {
            runBlocking {
                app.container.dailyReadingEvidenceRepository.readDailySnapshot(dateKey)
            }
        }.getOrNull()

        val cursor = MatrixCursor(columns.toTypedArray())
        if (snapshot != null) {
            cursor.addRow(columns.map { column -> valueFor(column, snapshot) }.toTypedArray())
        }
        context?.contentResolver?.let { resolver -> cursor.setNotificationUri(resolver, uri) }
        return cursor
    }

    override fun getType(uri: Uri): String? =
        if (validatedDateKey(uri) != null && uriMatcher.match(uri) == DAILY_BY_DATE) {
            "vnd.android.cursor.item/vnd.${DailyEvidenceProtocol.AUTHORITY}.daily"
        } else {
            null
        }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("DailyEvidenceProvider is read-only")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("DailyEvidenceProvider is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("DailyEvidenceProvider is read-only")

    private fun validatedDateKey(uri: Uri): String? {
        if (uri.scheme != "content" || uri.authority != DailyEvidenceProtocol.AUTHORITY ||
            uri.query != null || uri.fragment != null
        ) {
            return null
        }
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != "daily") return null
        val dateKey = segments[1]
        if (!DATE_KEY_PATTERN.matcher(dateKey).matches()) return null
        return runCatching {
            val date = LocalDate.parse(dateKey, DateTimeFormatter.ISO_LOCAL_DATE)
            date.takeIf { it.toString() == dateKey }?.toString()
        }.getOrNull()
    }

    private fun valueFor(column: String, snapshot: DailyReadingEvidenceSnapshot): Any =
        when (column) {
            DailyEvidenceProtocol.COLUMN_PROTOCOL_VERSION -> DailyEvidenceProtocol.PROTOCOL_VERSION
            DailyEvidenceProtocol.COLUMN_DATE_KEY -> snapshot.dateKey
            DailyEvidenceProtocol.COLUMN_VERIFIED_ACTIVE_SECONDS -> snapshot.verifiedActiveSeconds
            DailyEvidenceProtocol.COLUMN_QUALIFIED_PAGES -> snapshot.qualifiedPages
            DailyEvidenceProtocol.COLUMN_UPDATED_AT_EPOCH_MS -> snapshot.updatedAtEpochMs
            else -> error("Column was validated before projection")
        }
}
