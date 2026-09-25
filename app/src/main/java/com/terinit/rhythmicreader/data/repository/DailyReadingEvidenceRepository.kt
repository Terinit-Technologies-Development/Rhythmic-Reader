package com.terinit.rhythmicreader.data.repository

import com.terinit.rhythmicreader.data.db.DailyQualifiedPageEntity
import com.terinit.rhythmicreader.data.db.DailyReadingEvidenceDao
import com.terinit.rhythmicreader.data.db.DailyReadingEvidenceEntity
import com.terinit.rhythmicreader.domain.model.DailyReadingEvidenceSnapshot
import kotlinx.coroutines.flow.Flow

interface DailyReadingEvidenceRepository {
    suspend fun ensureDay(dateKey: String, updatedAtEpochMs: Long)
    suspend fun addVerifiedActiveSeconds(dateKey: String, seconds: Long, updatedAtEpochMs: Long)
    suspend fun recordQualifiedPage(
        dateKey: String,
        bookId: String,
        pageIndex: Int,
        qualifiedAtEpochMs: Long
    ): Boolean

    suspend fun readDailySnapshot(dateKey: String): DailyReadingEvidenceSnapshot?
    fun observeDailySnapshot(dateKey: String): Flow<DailyReadingEvidenceSnapshot?>
}

class DefaultDailyReadingEvidenceRepository(
    private val dao: DailyReadingEvidenceDao
) : DailyReadingEvidenceRepository {
    override suspend fun ensureDay(dateKey: String, updatedAtEpochMs: Long) {
        dao.ensureDay(dateKey, updatedAtEpochMs)
    }

    override suspend fun addVerifiedActiveSeconds(
        dateKey: String,
        seconds: Long,
        updatedAtEpochMs: Long
    ) {
        dao.addVerifiedActiveSeconds(dateKey, seconds, updatedAtEpochMs)
    }

    override suspend fun recordQualifiedPage(
        dateKey: String,
        bookId: String,
        pageIndex: Int,
        qualifiedAtEpochMs: Long
    ): Boolean = dao.recordQualifiedPage(
        DailyQualifiedPageEntity(
            dateKey = dateKey,
            bookId = bookId,
            pageIndex = pageIndex,
            qualifiedAtEpochMs = qualifiedAtEpochMs
        )
    )

    override suspend fun readDailySnapshot(dateKey: String): DailyReadingEvidenceSnapshot? =
        dao.readDailySnapshot(dateKey)

    override fun observeDailySnapshot(dateKey: String): Flow<DailyReadingEvidenceSnapshot?> =
        dao.observeDailySnapshot(dateKey)
}
