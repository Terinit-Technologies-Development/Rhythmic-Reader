package com.terinit.rhythmicreader.integration.rhythmic

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.terinit.rhythmicreader.data.db.ReaderDatabase
import com.terinit.rhythmicreader.data.repository.DefaultRecoveryRepository
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class DefaultRecoveryRepositoryIpcTest {

    private lateinit var db: ReaderDatabase
    private lateinit var repository: DefaultRecoveryRepository
    private lateinit var contentResolver: ContentResolver
    private val sessionId = UUID.randomUUID().toString()
    private val now = System.currentTimeMillis()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        contentResolver = mock(ContentResolver::class.java)
        repository = DefaultRecoveryRepository(
            recoveryDao = db.recoveryDao(),
            contentResolver = contentResolver
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun acceptExternalRequest_createsNewSession() = runBlocking {
        val request = RecoveryRequest(
            sessionId = sessionId,
            protocolVersion = 1,
            requiredSeconds = 1800L,
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )

        val session = repository.acceptExternalRequest(request)
        assertNotNull(session)
        assertEquals(sessionId, session?.sessionId)
        assertEquals(1800L, session?.requirement?.requiredActiveSeconds)
        assertEquals(10, session?.requirement?.requiredQualifiedPages)
        assertEquals(RecoveryStatus.ACTIVE, session?.status)
        assertEquals(1, session?.protocolVersion)

        // Verifies contentResolver was notified of creation
        verify(contentResolver).notifyChange(RecoveryProtocol.sessionUri(sessionId), null)
    }

    @Test
    fun acceptExternalRequest_idempotentResumeWithMatchingParams() = runBlocking {
        val request = RecoveryRequest(
            sessionId = sessionId,
            protocolVersion = 1,
            requiredSeconds = 1800L,
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )

        // First creation
        repository.acceptExternalRequest(request)
        // Simulate progress accumulated
        repository.updateActiveTime(sessionId, 45_000L)

        // Second request with same parameters
        val resumedSession = repository.acceptExternalRequest(request)
        assertNotNull(resumedSession)
        assertEquals(sessionId, resumedSession?.sessionId)
        assertEquals(45_000L, resumedSession?.accumulatedActiveMs)
        assertEquals(45L, resumedSession?.activeSeconds)
    }

    @Test
    fun acceptExternalRequest_rejectsConflictingParams() = runBlocking {
        val originalRequest = RecoveryRequest(
            sessionId = sessionId,
            protocolVersion = 1,
            requiredSeconds = 1800L,
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )
        repository.acceptExternalRequest(originalRequest)

        val conflictingRequest = RecoveryRequest(
            sessionId = sessionId,
            protocolVersion = 1,
            requiredSeconds = 3600L, // Different requiredSeconds
            requiredPages = 10,
            createdAt = now,
            expiresAt = now + 3600_000L
        )

        val result = repository.acceptExternalRequest(conflictingRequest)
        assertNull("Conflicting recovery request parameters must be rejected", result)

        // Verify original session was unmodified
        val stored = repository.getSession(sessionId)
        assertEquals(1800L, stored?.requirement?.requiredActiveSeconds)
    }
}
