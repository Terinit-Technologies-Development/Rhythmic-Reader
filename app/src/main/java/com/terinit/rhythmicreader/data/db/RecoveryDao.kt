package com.terinit.rhythmicreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecoveryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: RecoverySessionEntity)

    @Query("SELECT * FROM recovery_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: String): RecoverySessionEntity?

    @Query("SELECT * FROM recovery_sessions WHERE sessionId = :sessionId LIMIT 1")
    fun observeSession(sessionId: String): Flow<RecoverySessionEntity?>

    @Query("SELECT * FROM recovery_sessions WHERE status = 'ACTIVE' ORDER BY createdAtEpochMs DESC LIMIT 1")
    suspend fun getLatestActiveSession(): RecoverySessionEntity?

    @Query("SELECT * FROM recovery_sessions WHERE status = 'ACTIVE' ORDER BY createdAtEpochMs DESC LIMIT 1")
    fun observeLatestActiveSession(): Flow<RecoverySessionEntity?>

    @Query(
        """
        UPDATE recovery_sessions
        SET accumulatedActiveMs = :activeMs
        WHERE sessionId = :sessionId
        """
    )
    suspend fun updateActiveTime(
        sessionId: String,
        activeMs: Long
    )

    @Query(
        """
        UPDATE recovery_sessions
        SET status = :status,
            completedAtEpochMs = :completedAt
        WHERE sessionId = :sessionId
        """
    )
    suspend fun markComplete(
        sessionId: String,
        status: String,
        completedAt: Long
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQualifiedPage(page: QualifiedPageEntity): Long

    @Query(
        """
        SELECT COUNT(*)
        FROM qualified_pages
        WHERE sessionId = :sessionId
        """
    )
    suspend fun qualifiedPageCount(sessionId: String): Int

    @Query(
        """
        SELECT COUNT(*)
        FROM qualified_pages
        WHERE sessionId = :sessionId
        """
    )
    fun observeQualifiedPageCount(sessionId: String): Flow<Int>

    @Query(
        """
        SELECT *
        FROM qualified_pages
        WHERE sessionId = :sessionId
        """
    )
    suspend fun getQualifiedPages(sessionId: String): List<QualifiedPageEntity>
}
