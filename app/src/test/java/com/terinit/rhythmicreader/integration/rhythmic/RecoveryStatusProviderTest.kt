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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = RhythmicReaderApplication::class)
class RecoveryStatusProviderTest {

    private lateinit var provider: RecoveryStatusProvider
    private lateinit var app: RhythmicReaderApplication
    private val sessionId = UUID.randomUUID().toString()
    private val now = System.currentTimeMillis()

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        provider = Robolectric.setupContentProvider(
            RecoveryStatusProvider::class.java,
            RecoveryProtocol.AUTHORITY
        )
    }

    @Test
    fun queryExistingSession_returnsExpectedData() {
        runBlocking {
            val request = RecoveryRequest(
                sessionId = sessionId,
                protocolVersion = 1,
                requiredSeconds = 1800L,
                requiredPages = 10,
                createdAt = now,
                expiresAt = now + 3600_000L
            )
            app.container.recoveryRepository.acceptExternalRequest(request)
            app.container.recoveryRepository.updateActiveTime(sessionId, 65_000L) // 65 seconds
            app.container.recoveryRepository.qualifyPage(sessionId, "book-1", 1)

            val uri = RecoveryProtocol.sessionUri(sessionId)
            val cursor = provider.query(uri, null, null, null, null)

            assertNotNull(cursor)
            assertEquals(1, cursor?.count)
            cursor?.use {
                it.moveToFirst()
                assertEquals(sessionId, it.getString(it.getColumnIndexOrThrow(RecoveryProtocol.COLUMN_SESSION_ID)))
                assertEquals(1, it.getInt(it.getColumnIndexOrThrow(RecoveryProtocol.COLUMN_PROTOCOL_VERSION)))
                assertEquals("ACTIVE", it.getString(it.getColumnIndexOrThrow(RecoveryProtocol.COLUMN_STATUS)))
                assertEquals(65, it.getInt(it.getColumnIndexOrThrow(RecoveryProtocol.COLUMN_ACTIVE_SECONDS)))
                assertEquals(1, it.getInt(it.getColumnIndexOrThrow(RecoveryProtocol.COLUMN_QUALIFIED_PAGES)))
            }
        }
    }

    @Test
    fun queryNonExistentSession_returnsEmptyCursor() {
        val unknownUri = RecoveryProtocol.sessionUri(UUID.randomUUID().toString())
        val cursor = provider.query(unknownUri, null, null, null, null)

        assertNotNull(cursor)
        assertEquals(0, cursor?.count)
        cursor?.close()
    }

    @Test
    fun queryInvalidUri_returnsNull() {
        val invalidUri = Uri.parse("content://${RecoveryProtocol.AUTHORITY}/other_table/123")
        val cursor = provider.query(invalidUri, null, null, null, null)
        assertNull(cursor)
    }

    @Test
    fun insert_throwsUnsupportedOperationException() {
        val uri = RecoveryProtocol.sessionUri(sessionId)
        assertThrows(UnsupportedOperationException::class.java) {
            provider.insert(uri, ContentValues())
        }
    }

    @Test
    fun update_throwsUnsupportedOperationException() {
        val uri = RecoveryProtocol.sessionUri(sessionId)
        assertThrows(UnsupportedOperationException::class.java) {
            provider.update(uri, ContentValues(), null, null)
        }
    }

    @Test
    fun delete_throwsUnsupportedOperationException() {
        val uri = RecoveryProtocol.sessionUri(sessionId)
        assertThrows(UnsupportedOperationException::class.java) {
            provider.delete(uri, null, null)
        }
    }
}
