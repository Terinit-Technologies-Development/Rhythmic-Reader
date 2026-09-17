package com.terinit.rhythmicreader.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query(
        """
        SELECT * FROM books
        ORDER BY
        COALESCE(lastOpenedAtEpochMs, importedAtEpochMs)
        DESC
        """
    )
    fun observeLibrary(): Flow<List<BookEntity>>

    @Query(
        "SELECT * FROM books WHERE id = :id LIMIT 1"
    )
    suspend fun getById(id: String): BookEntity?

    @Query("SELECT * FROM books")
    suspend fun getAll(): List<BookEntity>

    @Query(
        "SELECT * FROM books WHERE documentUri = :documentUri LIMIT 1"
    )
    suspend fun getByUri(documentUri: String): BookEntity?

    @Insert(
        onConflict = OnConflictStrategy.IGNORE
    )
    suspend fun insert(book: BookEntity): Long

    @Query(
        """
        UPDATE books
        SET lastPageIndex = :page,
            lastOpenedAtEpochMs = :openedAt
        WHERE id = :bookId
        """
    )
    suspend fun updateProgress(
        bookId: String,
        page: Int,
        openedAt: Long
    )

    @Query(
        "DELETE FROM books WHERE id = :bookId"
    )
    suspend fun delete(bookId: String)

    @Query(
        "DELETE FROM books"
    )
    suspend fun deleteAll()

    @Query(
        """
        UPDATE books
        SET lastPageIndex = 0
        """
    )
    suspend fun resetAllProgress()
}
