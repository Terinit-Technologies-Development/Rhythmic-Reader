package com.terinit.rhythmicreader.integration.rhythmic

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.terinit.rhythmicreader.data.db.QualifiedPageEntity
import com.terinit.rhythmicreader.data.db.ReaderDatabase
import com.terinit.rhythmicreader.data.db.RecoveryDao
import com.terinit.rhythmicreader.data.db.RecoverySessionEntity
import com.terinit.rhythmicreader.data.repository.DefaultRecoveryRepository
import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoverySession
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SW-2026-005 A3 / R3 reproduction and regression.
 *
 * Root reproduced the original check-then-insert race on
 * [simultaneousIdenticalAcceptCreatesOnePersistedBindingWithoutException] with
 * `SQLiteConstraintException` (SQLITE_CONSTRAINT_PRIMARYKEY). These tests now
 * exercise the fixed production path: real [DefaultRecoveryRepository], real
 * Room [RecoveryDao], and real in-memory SQLite under Robolectric, with
 * independent repository instances sharing one database.
 *
 * The fixture [BarrierRecoveryDao] delegates every call to the real Room DAO;
 * it only rendezvouses callers immediately before the real acceptance insert.
 * It never fabricates a row or injects an exception. The wait is bounded so a
 * serialized/transactional implementation cannot deadlock here.
 */
@RunWith(RobolectricTestRunner::class)
class RecoveryRequestConcurrencyTest {

    private lateinit var db: ReaderDatabase
    private lateinit var realDao: RecoveryDao
    private lateinit var serialRepository: DefaultRecoveryRepository

    private val sessionId = UUID.randomUUID().toString()
    private val createdAt = 1_760_000_000_000L
    private val expiresAt = createdAt + 3_600_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        realDao = db.recoveryDao()
        serialRepository = DefaultRecoveryRepository(
            recoveryDao = realDao,
            contentResolver = null
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun simultaneousIdenticalAcceptCreatesOnePersistedBindingWithoutException() =
        runBlocking(Dispatchers.IO) {
            val request = request()
            assertTrue("fixture must be a valid canonical request", request.isValid)

            val barrier = AcceptanceBarrier(parties = 2, timeoutMs = 5_000L)
            val firstRepository = concurrentRepository(barrier)
            val secondRepository = concurrentRepository(barrier)

            val (first, second) = withTimeout(30_000L) {
                race(firstRepository, secondRepository, request, request)
            }

            val firstSession = requireNotNull(first) {
                "first concurrent accept returned null"
            }
            val secondSession = requireNotNull(second) {
                "second concurrent accept returned null"
            }
            assertEquals(
                "both independent repositories must observe the same persisted binding",
                firstSession,
                secondSession
            )
            assertEquals(
                RecoverySession(
                    sessionId = sessionId,
                    requirement = RecoveryRequirement(
                        requiredActiveSeconds = 1_800L,
                        requiredQualifiedPages = 11
                    ),
                    accumulatedActiveMs = 0L,
                    qualifiedPages = 0,
                    status = RecoveryStatus.ACTIVE,
                    createdAtEpochMs = createdAt,
                    completedAtEpochMs = null,
                    expiresAtEpochMs = expiresAt,
                    protocolVersion = RecoveryProtocol.PROTOCOL_VERSION
                ),
                firstSession
            )

            val persisted = requireNotNull(realDao.getSession(sessionId)) {
                "accepted session must exist in real SQLite"
            }
            assertEquals(1_800L, persisted.requiredActiveSeconds)
            assertEquals(11, persisted.requiredQualifiedPages)
            assertEquals(0L, persisted.accumulatedActiveMs)
            assertEquals(RecoveryStatus.ACTIVE.name, persisted.status)
            assertEquals(createdAt, persisted.createdAtEpochMs)
            assertNull(persisted.completedAtEpochMs)
            assertEquals(expiresAt, persisted.expiresAtEpochMs ?: -1L)
            assertEquals(RecoveryProtocol.PROTOCOL_VERSION, persisted.protocolVersion)
            assertEquals(0, realDao.qualifiedPageCount(sessionId))
            assertEquals(
                "exactly one recovery_sessions row must exist",
                1,
                rowCount(sessionId)
            )
        }

    @Test
    fun conflictingInitialRaceAcrossIndependentRepositoriesChoosesOneImmutableWinner() =
        runBlocking(Dispatchers.IO) {
            val firstRequest = request(requiredSeconds = 1_800L)
            val secondRequest = request(requiredSeconds = 3_600L)
            val barrier = AcceptanceBarrier(parties = 2, timeoutMs = 5_000L)

            val (first, second) = withTimeout(30_000L) {
                race(
                    concurrentRepository(barrier),
                    concurrentRepository(barrier),
                    firstRequest,
                    secondRequest
                )
            }

            assertTrue(
                "exactly one concurrent initial accept must win",
                (first == null) != (second == null)
            )
            val winnerRequest = if (first != null) firstRequest else secondRequest
            val winnerSession = requireNotNull(first ?: second) {
                "one concurrent initial accept must return the accepted binding"
            }
            assertEquals(
                winnerRequest.requiredSeconds,
                winnerSession.requirement.requiredActiveSeconds
            )
            assertEquals(
                winnerRequest.requiredPages,
                winnerSession.requirement.requiredQualifiedPages
            )
            assertEquals(RecoveryStatus.ACTIVE, winnerSession.status)

            val persisted = requireNotNull(realDao.getSession(sessionId))
            assertEquals(winnerRequest.requiredSeconds, persisted.requiredActiveSeconds)
            assertEquals(winnerRequest.requiredPages, persisted.requiredQualifiedPages)
            assertEquals(winnerRequest.createdAt, persisted.createdAtEpochMs)
            assertEquals(winnerRequest.expiresAt, persisted.expiresAtEpochMs ?: -1L)
            assertEquals(winnerRequest.protocolVersion, persisted.protocolVersion)
            assertEquals(RecoveryStatus.ACTIVE.name, persisted.status)
            assertEquals(0L, persisted.accumulatedActiveMs)
            assertEquals(1, rowCount(sessionId))
        }

    @Test
    fun exactReplayAcrossIndependentRepositoriesPreservesProgress() =
        runBlocking(Dispatchers.IO) {
            val accepted = request()
            assertNotNull(serialRepository.acceptExternalRequest(accepted))
            serialRepository.updateActiveTime(sessionId, 45_000L)
            assertTrue(serialRepository.qualifyPage(sessionId, "book-A", 7))

            val barrier = AcceptanceBarrier(parties = 2, timeoutMs = 5_000L)
            val (first, second) = withTimeout(30_000L) {
                race(
                    concurrentRepository(barrier),
                    concurrentRepository(barrier),
                    accepted,
                    accepted
                )
            }

            val firstSession = requireNotNull(first)
            val secondSession = requireNotNull(second)
            assertEquals(firstSession, secondSession)
            assertEquals(45_000L, firstSession.accumulatedActiveMs)
            assertEquals(1, firstSession.qualifiedPages)
            assertEquals(RecoveryStatus.ACTIVE, firstSession.status)
            assertEquals(createdAt, firstSession.createdAtEpochMs)
            assertEquals(expiresAt, firstSession.expiresAtEpochMs ?: -1L)
            assertEquals(RecoveryProtocol.PROTOCOL_VERSION, firstSession.protocolVersion)

            val persisted = requireNotNull(realDao.getSession(sessionId))
            assertEquals(45_000L, persisted.accumulatedActiveMs)
            assertEquals(RecoveryStatus.ACTIVE.name, persisted.status)
            assertEquals(1, realDao.qualifiedPageCount(sessionId))
            assertEquals(1, rowCount(sessionId))
        }

    @Test
    fun completedReplayPreservesCompletionAndEvidence() =
        runBlocking(Dispatchers.IO) {
            val accepted = request()
            assertNotNull(serialRepository.acceptExternalRequest(accepted))
            serialRepository.updateActiveTime(sessionId, 1_800_000L)
            repeat(11) { page ->
                assertTrue(
                    "page $page must qualify for the bound session",
                    serialRepository.qualifyPage(sessionId, "book-A", page)
                )
            }
            serialRepository.markComplete(sessionId)

            val completed = requireNotNull(serialRepository.getSession(sessionId))
            assertEquals(RecoveryStatus.COMPLETE, completed.status)
            assertEquals(1_800L, completed.activeSeconds)
            assertEquals(11, completed.qualifiedPages)
            val completedAt = completed.completedAtEpochMs
            assertNotNull("completion timestamp must be persisted", completedAt)

            val conflicting = request(requiredSeconds = 3_600L)
            val barrier = AcceptanceBarrier(parties = 2, timeoutMs = 5_000L)
            val (replay, conflict) = withTimeout(30_000L) {
                race(
                    concurrentRepository(barrier),
                    concurrentRepository(barrier),
                    accepted,
                    conflicting
                )
            }

            val replaySession = requireNotNull(replay) {
                "completed identical replay must return the completed binding"
            }
            assertNull(
                "conflicting replay must not touch the completed winner",
                conflict
            )
            assertEquals(RecoveryStatus.COMPLETE, replaySession.status)
            assertEquals(1_800L, replaySession.activeSeconds)
            assertEquals(11, replaySession.qualifiedPages)
            assertEquals(completedAt, replaySession.completedAtEpochMs)

            val persisted = requireNotNull(realDao.getSession(sessionId))
            assertEquals(RecoveryStatus.COMPLETE.name, persisted.status)
            assertEquals(1_800_000L, persisted.accumulatedActiveMs)
            assertEquals(completedAt, persisted.completedAtEpochMs)
            assertEquals(11, realDao.qualifiedPageCount(sessionId))
            assertEquals(1, rowCount(sessionId))
        }

    @Test
    fun replayWithDifferentCreatedAtIsRejectedWithoutMutation() =
        runBlocking(Dispatchers.IO) {
            val accepted = request()
            assertNotNull(serialRepository.acceptExternalRequest(accepted))

            val differentCreatedAt = request(created = createdAt + 1_000L)
            assertNull(
                "createdAt is part of the immutable binding",
                serialRepository.acceptExternalRequest(differentCreatedAt)
            )

            val persisted = requireNotNull(realDao.getSession(sessionId))
            assertEquals(createdAt, persisted.createdAtEpochMs)
            assertEquals(expiresAt, persisted.expiresAtEpochMs ?: -1L)
            assertEquals(1_800L, persisted.requiredActiveSeconds)
            assertEquals(11, persisted.requiredQualifiedPages)
            assertEquals(1, rowCount(sessionId))
        }

    @Test
    fun replayWithDifferentProtocolVersionIsRejectedWithoutMutation() =
        runBlocking(Dispatchers.IO) {
            realDao.insertSession(
                RecoverySessionEntity(
                    sessionId = sessionId,
                    requiredActiveSeconds = 1_800L,
                    requiredQualifiedPages = 11,
                    accumulatedActiveMs = 0L,
                    status = RecoveryStatus.ACTIVE.name,
                    createdAtEpochMs = createdAt,
                    completedAtEpochMs = null,
                    expiresAtEpochMs = expiresAt,
                    protocolVersion = 2
                )
            )

            assertNull(
                "protocolVersion is part of the immutable binding",
                serialRepository.acceptExternalRequest(request())
            )

            val persisted = requireNotNull(realDao.getSession(sessionId))
            assertEquals(2, persisted.protocolVersion)
            assertEquals(1_800L, persisted.requiredActiveSeconds)
            assertEquals(11, persisted.requiredQualifiedPages)
            assertEquals(createdAt, persisted.createdAtEpochMs)
            assertEquals(expiresAt, persisted.expiresAtEpochMs ?: -1L)
            assertEquals(1, rowCount(sessionId))
        }

    @Test
    fun invalidRequestDoesNotWrite() =
        runBlocking(Dispatchers.IO) {
            val invalid = request(protocol = 2)
            assertFalse(invalid.isValid)

            assertNull(serialRepository.acceptExternalRequest(invalid))

            assertNull(realDao.getSession(sessionId))
            assertEquals(0, rowCount(sessionId))
        }

    @Test
    fun databaseFailureIsNotSwallowed() {
        val repository = DefaultRecoveryRepository(
            recoveryDao = FailingAcceptanceDao(realDao),
            contentResolver = null
        )

        val error = assertThrows(SQLiteException::class.java) {
            runBlocking(Dispatchers.IO) {
                repository.acceptExternalRequest(request())
                Unit
            }
        }

        assertEquals("simulated disk I/O failure", error.message)
        runBlocking(Dispatchers.IO) {
            assertNull(realDao.getSession(sessionId))
            assertEquals(0, rowCount(sessionId))
        }
    }

    private fun concurrentRepository(barrier: AcceptanceBarrier): DefaultRecoveryRepository =
        DefaultRecoveryRepository(
            recoveryDao = BarrierRecoveryDao(realDao, barrier),
            contentResolver = null
        )

    private fun request(
        requiredSeconds: Long = 1_800L,
        requiredPages: Int = 11,
        created: Long = createdAt,
        expires: Long = expiresAt,
        protocol: Int = RecoveryProtocol.PROTOCOL_VERSION
    ) = RecoveryRequest(
        sessionId = sessionId,
        protocolVersion = protocol,
        requiredSeconds = requiredSeconds,
        requiredPages = requiredPages,
        createdAt = created,
        expiresAt = expires
    )

    /**
     * Starts both calls at the same instant and waits for both to settle.
     * Failures are collected and rethrown (first with suppressed siblings) so
     * the real stack trace reaches JUnit; nothing is swallowed.
     */
    private suspend fun race(
        firstRepository: DefaultRecoveryRepository,
        secondRepository: DefaultRecoveryRepository,
        first: RecoveryRequest,
        second: RecoveryRequest
    ): Pair<RecoverySession?, RecoverySession?> = supervisorScope {
        val start = CompletableDeferred<Unit>()
        val firstCall = async(Dispatchers.IO) {
            start.await()
            firstRepository.acceptExternalRequest(first)
        }
        val secondCall = async(Dispatchers.IO) {
            start.await()
            secondRepository.acceptExternalRequest(second)
        }
        start.complete(Unit)

        val failures = mutableListOf<Throwable>()
        val firstResult = try {
            firstCall.await()
        } catch (failure: Throwable) {
            failures += failure
            null
        }
        val secondResult = try {
            secondCall.await()
        } catch (failure: Throwable) {
            failures += failure
            null
        }
        if (failures.isNotEmpty()) {
            val primary = failures.first()
            failures.drop(1).forEach(primary::addSuppressed)
            throw primary
        }
        firstResult to secondResult
    }

    private fun rowCount(sessionId: String): Int {
        db.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM recovery_sessions WHERE sessionId = ?",
            arrayOf<Any?>(sessionId)
        ).use { cursor ->
            assertTrue("count query must return a row", cursor.moveToFirst())
            return cursor.getInt(0)
        }
    }
}

/**
 * Bounded, one-shot rendezvous placed immediately before the real acceptance
 * insert (`insertSessionIfAbsent`, or `insertSession` if a check-then-insert
 * regression returns). Both callers attempt the real Room insert together, so
 * the database conflict path is genuinely exercised. A serialized/transactional
 * implementation only lets one caller reach the insert at a time, so the
 * bounded wait expires and that caller proceeds instead of deadlocking.
 */
private class AcceptanceBarrier(
    parties: Int,
    private val timeoutMs: Long
) {
    private val barrier = CyclicBarrier(parties)
    private val released = AtomicBoolean(false)

    fun awaitBeforeInsert() {
        if (released.get()) return
        try {
            barrier.await(timeoutMs, TimeUnit.MILLISECONDS)
            released.set(true)
        } catch (_: TimeoutException) {
            released.set(true)
        } catch (_: BrokenBarrierException) {
            released.set(true)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            released.set(true)
        }
    }
}

/**
 * Delegating DAO fixture. Every operation reaches the real Room DAO; subclasses
 * override only the operation under test.
 */
private open class DelegatingRecoveryDao(
    protected val delegate: RecoveryDao
) : RecoveryDao {

    override suspend fun insertSession(session: RecoverySessionEntity) =
        delegate.insertSession(session)

    override suspend fun insertSessionIfAbsent(session: RecoverySessionEntity): Long =
        delegate.insertSessionIfAbsent(session)

    override suspend fun getSession(sessionId: String): RecoverySessionEntity? =
        delegate.getSession(sessionId)

    override fun observeSession(sessionId: String): Flow<RecoverySessionEntity?> =
        delegate.observeSession(sessionId)

    override suspend fun getLatestActiveSession(): RecoverySessionEntity? =
        delegate.getLatestActiveSession()

    override fun observeLatestActiveSession(): Flow<RecoverySessionEntity?> =
        delegate.observeLatestActiveSession()

    override suspend fun updateActiveTime(sessionId: String, activeMs: Long) =
        delegate.updateActiveTime(sessionId, activeMs)

    override suspend fun markComplete(
        sessionId: String,
        status: String,
        completedAt: Long
    ) = delegate.markComplete(sessionId, status, completedAt)

    override suspend fun insertQualifiedPage(page: QualifiedPageEntity): Long =
        delegate.insertQualifiedPage(page)

    override suspend fun qualifiedPageCount(sessionId: String): Int =
        delegate.qualifiedPageCount(sessionId)

    override fun observeQualifiedPageCount(sessionId: String): Flow<Int> =
        delegate.observeQualifiedPageCount(sessionId)

    override suspend fun getQualifiedPages(sessionId: String): List<QualifiedPageEntity> =
        delegate.getQualifiedPages(sessionId)
}

private class BarrierRecoveryDao(
    delegate: RecoveryDao,
    private val barrier: AcceptanceBarrier
) : DelegatingRecoveryDao(delegate) {

    override suspend fun insertSession(session: RecoverySessionEntity) {
        barrier.awaitBeforeInsert()
        delegate.insertSession(session)
    }

    override suspend fun insertSessionIfAbsent(session: RecoverySessionEntity): Long {
        barrier.awaitBeforeInsert()
        return delegate.insertSessionIfAbsent(session)
    }
}

private class FailingAcceptanceDao(
    delegate: RecoveryDao
) : DelegatingRecoveryDao(delegate) {

    override suspend fun insertSessionIfAbsent(session: RecoverySessionEntity): Long {
        throw SQLiteException("simulated disk I/O failure")
    }
}
