package com.terinit.rhythmicreader.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.terinit.rhythmicreader.data.repository.DefaultDailyReadingEvidenceRepository
import com.terinit.rhythmicreader.domain.recovery.DailyEvidenceRecorder
import com.terinit.rhythmicreader.domain.time.DeviceLocalDateClock
import com.terinit.rhythmicreader.test.FakeMonotonicClock
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class DailyReadingEvidenceDaoTest {

    private lateinit var db: ReaderDatabase
    private lateinit var dao: DailyReadingEvidenceDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dailyReadingEvidenceDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun dailySnapshotCombinesAtomicTimeAndDistinctPageEvidence() = runBlocking {
        dao.addVerifiedActiveSeconds("2026-09-22", 42L, 100L)
        assertEquals(
            true,
            dao.recordQualifiedPage(
                DailyQualifiedPageEntity("2026-09-22", "book-a", 7, 120L)
            )
        )
        assertEquals(
            false,
            dao.recordQualifiedPage(
                DailyQualifiedPageEntity("2026-09-22", "book-a", 7, 140L)
            )
        )

        val snapshot = dao.readDailySnapshot("2026-09-22")
        assertNotNull(snapshot)
        assertEquals(42L, snapshot?.verifiedActiveSeconds)
        assertEquals(1, snapshot?.qualifiedPages)
        assertEquals(120L, snapshot?.updatedAtEpochMs)
        assertNull(dao.readDailySnapshot("2026-09-23"))
    }

    @Test
    fun samePageCanQualifyAgainOnAnotherLocalDateWithoutChangingHistory() = runBlocking {
        dao.recordQualifiedPage(DailyQualifiedPageEntity("2026-09-22", "book-a", 7, 100L))
        dao.recordQualifiedPage(DailyQualifiedPageEntity("2026-09-23", "book-a", 7, 200L))
        dao.addVerifiedActiveSeconds("2026-09-23", 1L, 220L)

        assertEquals(1, dao.readDailySnapshot("2026-09-22")?.qualifiedPages)
        assertEquals(1, dao.readDailySnapshot("2026-09-23")?.qualifiedPages)
        assertEquals(0L, dao.readDailySnapshot("2026-09-22")?.verifiedActiveSeconds)
        assertEquals(1L, dao.readDailySnapshot("2026-09-23")?.verifiedActiveSeconds)
    }

    @Test
    fun queuedPageQualificationUsesItsQualificationDateAfterMidnight() = runBlocking {
        val qualifiedAt = Instant.parse("2026-09-22T23:59:59Z").toEpochMilli()
        val nextDay = Instant.parse("2026-09-23T00:00:05Z").toEpochMilli()
        val clock = FakeMonotonicClock(nextDay)
        val repository = DefaultDailyReadingEvidenceRepository(dao)
        val recorder = DailyEvidenceRecorder(
            repository = repository,
            localDateClock = DeviceLocalDateClock(clock) { ZoneId.of("UTC") },
            wallClock = clock
        )

        assertEquals(true, recorder.recordQualifiedPage("book-a", 3, qualifiedAt))
        assertEquals(1, repository.readDailySnapshot("2026-09-22")?.qualifiedPages)
        assertNull(repository.readDailySnapshot("2026-09-23"))
    }
}
