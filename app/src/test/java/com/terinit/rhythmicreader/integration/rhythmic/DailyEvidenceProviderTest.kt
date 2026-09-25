package com.terinit.rhythmicreader.integration.rhythmic

import android.content.ContentValues
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.terinit.rhythmicreader.app.RhythmicReaderApplication
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RhythmicReaderApplication::class)
class DailyEvidenceProviderTest {

    private lateinit var provider: DailyEvidenceProvider
    private lateinit var app: RhythmicReaderApplication

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        provider = Robolectric.setupContentProvider(
            DailyEvidenceProvider::class.java,
            DailyEvidenceProtocol.AUTHORITY
        )
    }

    @Test
    fun exactDateQueryReturnsOnlyTheV2EvidenceColumns() = runBlocking {
        val dateKey = "2097-01-18"
        app.container.dailyReadingEvidenceRepository.ensureDay(dateKey, 900L)
        app.container.dailyReadingEvidenceRepository.addVerifiedActiveSeconds(dateKey, 3_723L, 1_000L)
        app.container.dailyReadingEvidenceRepository.recordQualifiedPage(dateKey, "private-book-id", 9, 1_200L)

        val cursor = app.contentResolver.query(
            DailyEvidenceProtocol.dailyUri(dateKey),
            null,
            null,
            null,
            null
        )
        assertNotNull(cursor)
        cursor!!.use {
            assertEquals(1, it.count)
            assertEquals(DailyEvidenceProtocol.ALL_COLUMNS.toList(), it.columnNames.toList())
            assertTrue(it.moveToFirst())
            assertEquals(2, it.getInt(it.getColumnIndexOrThrow(DailyEvidenceProtocol.COLUMN_PROTOCOL_VERSION)))
            assertEquals(dateKey, it.getString(it.getColumnIndexOrThrow(DailyEvidenceProtocol.COLUMN_DATE_KEY)))
            assertEquals(3_723L, it.getLong(it.getColumnIndexOrThrow(DailyEvidenceProtocol.COLUMN_VERIFIED_ACTIVE_SECONDS)))
            assertEquals(1, it.getInt(it.getColumnIndexOrThrow(DailyEvidenceProtocol.COLUMN_QUALIFIED_PAGES)))
            assertEquals(1_200L, it.getLong(it.getColumnIndexOrThrow(DailyEvidenceProtocol.COLUMN_UPDATED_AT_EPOCH_MS)))
        }
    }

    @Test
    fun validMissingDateReturnsAnEmptyCursorAndMalformedUrisFailSafely() {
        val missing = provider.query(
            DailyEvidenceProtocol.dailyUri("2097-01-19"), null, null, null, null
        )
        assertNotNull(missing)
        assertEquals(0, missing?.count)
        missing?.close()

        assertNull(provider.query(Uri.parse("content://${DailyEvidenceProtocol.AUTHORITY}/daily/not-a-date"), null, null, null, null))
        assertNull(provider.query(Uri.parse("content://${DailyEvidenceProtocol.AUTHORITY}/daily/2026-02-30"), null, null, null, null))
        assertNull(provider.query(Uri.parse("content://${DailyEvidenceProtocol.AUTHORITY}/daily/2026-09-23/extra"), null, null, null, null))
        assertNull(provider.query(Uri.parse("https://${DailyEvidenceProtocol.AUTHORITY}/daily/2026-09-23"), null, null, null, null))
        assertNull(provider.query(DailyEvidenceProtocol.dailyUri("2026-09-23"), null, "1=1", null, null))
        assertNull(
            provider.query(
                DailyEvidenceProtocol.dailyUri("2026-09-23"),
                arrayOf("bookName"),
                null,
                null,
                null
            )
        )
    }

    @Test
    fun providerIsReadOnlyAndProtectedByTheSharedSignaturePermission() {
        val uri = DailyEvidenceProtocol.dailyUri("2026-09-23")
        assertThrows(UnsupportedOperationException::class.java) {
            provider.insert(uri, ContentValues())
        }
        assertThrows(UnsupportedOperationException::class.java) {
            provider.update(uri, ContentValues(), null, null)
        }
        assertThrows(UnsupportedOperationException::class.java) {
            provider.delete(uri, null, null)
        }

        assertEquals(RecoveryProtocol.PERMISSION_RECOVERY, DailyEvidenceProtocol.READ_PERMISSION)
    }
}
