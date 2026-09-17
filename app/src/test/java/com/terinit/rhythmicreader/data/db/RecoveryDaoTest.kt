package com.terinit.rhythmicreader.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecoveryDaoTest {

    private lateinit var db: ReaderDatabase
    private lateinit var dao: RecoveryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.recoveryDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetSession() = runBlocking {
        val session = RecoverySessionEntity(
            sessionId = "session-1",
            requiredActiveSeconds = 1800L,
            requiredQualifiedPages = 10,
            accumulatedActiveMs = 5000L,
            status = "ACTIVE",
            createdAtEpochMs = 1000L,
            completedAtEpochMs = null,
            expiresAtEpochMs = null
        )
        dao.insertSession(session)

        val retrieved = dao.getSession("session-1")
        assertNotNull(retrieved)
        assertEquals("session-1", retrieved?.sessionId)
        assertEquals(1800L, retrieved?.requiredActiveSeconds)
        assertEquals(10, retrieved?.requiredQualifiedPages)
        assertEquals(5000L, retrieved?.accumulatedActiveMs)
        assertEquals("ACTIVE", retrieved?.status)
    }

    @Test
    fun updateActiveTime() = runBlocking {
        val session = RecoverySessionEntity(
            sessionId = "session-2",
            requiredActiveSeconds = 600L,
            requiredQualifiedPages = 5,
            accumulatedActiveMs = 0L,
            status = "ACTIVE",
            createdAtEpochMs = 1000L,
            completedAtEpochMs = null,
            expiresAtEpochMs = null
        )
        dao.insertSession(session)

        dao.updateActiveTime("session-2", 45000L)

        val updated = dao.getSession("session-2")
        assertEquals(45000L, updated?.accumulatedActiveMs)
    }

    @Test
    fun markComplete() = runBlocking {
        val session = RecoverySessionEntity(
            sessionId = "session-3",
            requiredActiveSeconds = 600L,
            requiredQualifiedPages = 5,
            accumulatedActiveMs = 600000L,
            status = "ACTIVE",
            createdAtEpochMs = 1000L,
            completedAtEpochMs = null,
            expiresAtEpochMs = null
        )
        dao.insertSession(session)

        dao.markComplete("session-3", "COMPLETE", 25000L)

        val completed = dao.getSession("session-3")
        assertEquals("COMPLETE", completed?.status)
        assertEquals(25000L, completed?.completedAtEpochMs)
    }

    @Test
    fun compositePrimaryKey_preventsDuplicatePageCredit() = runBlocking {
        val session = RecoverySessionEntity(
            sessionId = "session-4",
            requiredActiveSeconds = 600L,
            requiredQualifiedPages = 5,
            accumulatedActiveMs = 0L,
            status = "ACTIVE",
            createdAtEpochMs = 1000L,
            completedAtEpochMs = null,
            expiresAtEpochMs = null
        )
        dao.insertSession(session)

        // First insert of (session-4, book-A, page 7)
        val r1 = dao.insertQualifiedPage(
            QualifiedPageEntity(
                sessionId = "session-4",
                bookId = "book-A",
                pageIndex = 7,
                qualifiedAtEpochMs = 2000L
            )
        )
        assertEquals(1, dao.qualifiedPageCount("session-4"))

        // Repeated insert of exact same page (repeated page farming)
        val r2 = dao.insertQualifiedPage(
            QualifiedPageEntity(
                sessionId = "session-4",
                bookId = "book-A",
                pageIndex = 7,
                qualifiedAtEpochMs = 5000L
            )
        )
        // Must be ignored by composite primary key and onConflict = IGNORE
        assertEquals(-1L, r2)
        assertEquals(1, dao.qualifiedPageCount("session-4"))

        // Different page in same book qualifies
        dao.insertQualifiedPage(
            QualifiedPageEntity(
                sessionId = "session-4",
                bookId = "book-A",
                pageIndex = 8,
                qualifiedAtEpochMs = 6000L
            )
        )
        assertEquals(2, dao.qualifiedPageCount("session-4"))

        // Same page index in different book qualifies
        dao.insertQualifiedPage(
            QualifiedPageEntity(
                sessionId = "session-4",
                bookId = "book-B",
                pageIndex = 7,
                qualifiedAtEpochMs = 7000L
            )
        )
        assertEquals(3, dao.qualifiedPageCount("session-4"))
    }
}
