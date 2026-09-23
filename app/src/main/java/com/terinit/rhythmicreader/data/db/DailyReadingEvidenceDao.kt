package com.terinit.rhythmicreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.terinit.rhythmicreader.domain.model.DailyReadingEvidenceSnapshot
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReadingEvidenceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDayIfMissing(evidence: DailyReadingEvidenceEntity): Long

    @Query("SELECT * FROM daily_reading_evidence WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDay(dateKey: String): DailyReadingEvidenceEntity?

    @Query("SELECT COUNT(*) FROM daily_qualified_pages WHERE dateKey = :dateKey")
    suspend fun qualifiedPageCount(dateKey: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQualifiedPage(page: DailyQualifiedPageEntity): Long

    @Query(
        """
        UPDATE daily_reading_evidence
        SET verifiedActiveSeconds = verifiedActiveSeconds + :seconds,
            updatedAtEpochMs = :updatedAtEpochMs
        WHERE dateKey = :dateKey
        """
    )
    suspend fun incrementVerifiedActiveSeconds(
        dateKey: String,
        seconds: Long,
        updatedAtEpochMs: Long
    )

    @Query(
        """
        UPDATE daily_reading_evidence
        SET updatedAtEpochMs = :updatedAtEpochMs
        WHERE dateKey = :dateKey
        """
    )
    suspend fun updateEvidenceTimestamp(dateKey: String, updatedAtEpochMs: Long)

    @Transaction
    suspend fun ensureDay(dateKey: String, updatedAtEpochMs: Long) {
        insertDayIfMissing(
            DailyReadingEvidenceEntity(
                dateKey = dateKey,
                verifiedActiveSeconds = 0L,
                updatedAtEpochMs = updatedAtEpochMs
            )
        )
    }

    @Transaction
    suspend fun addVerifiedActiveSeconds(
        dateKey: String,
        seconds: Long,
        updatedAtEpochMs: Long
    ) {
        ensureDay(dateKey, updatedAtEpochMs)
        incrementVerifiedActiveSeconds(dateKey, seconds.coerceAtLeast(0L), updatedAtEpochMs)
    }

    @Transaction
    suspend fun recordQualifiedPage(page: DailyQualifiedPageEntity): Boolean {
        ensureDay(page.dateKey, page.qualifiedAtEpochMs)
        val inserted = insertQualifiedPage(page) != -1L
        if (inserted) {
            updateEvidenceTimestamp(page.dateKey, page.qualifiedAtEpochMs)
        }
        return inserted
    }

    @Transaction
    suspend fun readDailySnapshot(dateKey: String): DailyReadingEvidenceSnapshot? {
        val evidence = getDay(dateKey) ?: return null
        return DailyReadingEvidenceSnapshot(
            dateKey = evidence.dateKey,
            verifiedActiveSeconds = evidence.verifiedActiveSeconds,
            qualifiedPages = qualifiedPageCount(dateKey),
            updatedAtEpochMs = evidence.updatedAtEpochMs
        )
    }

    @Query(
        """
        SELECT evidence.dateKey AS dateKey,
               evidence.verifiedActiveSeconds AS verifiedActiveSeconds,
               (SELECT COUNT(*) FROM daily_qualified_pages pages
                WHERE pages.dateKey = evidence.dateKey) AS qualifiedPages,
               evidence.updatedAtEpochMs AS updatedAtEpochMs
        FROM daily_reading_evidence evidence
        WHERE evidence.dateKey = :dateKey
        LIMIT 1
        """
    )
    fun observeDailySnapshot(dateKey: String): Flow<DailyReadingEvidenceSnapshot?>
}
