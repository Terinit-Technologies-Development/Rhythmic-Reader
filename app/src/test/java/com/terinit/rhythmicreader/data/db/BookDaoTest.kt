package com.terinit.rhythmicreader.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class BookDaoTest {

    private lateinit var database: ReaderDatabase
    private lateinit var bookDao: BookDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        bookDao = database.bookDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndGetById() = runTest {
        val book = BookEntity(
            id = "book-1",
            displayName = "Calm Living.pdf",
            documentUri = "content://docs/1",
            totalPages = 150,
            lastPageIndex = 0,
            importedAtEpochMs = 1000L
        )

        bookDao.insert(book)
        val loaded = bookDao.getById("book-1")

        assertNotNull(loaded)
        assertEquals("Calm Living.pdf", loaded?.displayName)
        assertEquals("content://docs/1", loaded?.documentUri)
        assertEquals(150, loaded?.totalPages)
    }

    @Test
    fun delete_removesLibraryReferenceOnly() = runTest {
        val book = BookEntity(
            id = "book-2",
            displayName = "Focus.pdf",
            documentUri = "content://docs/2",
            totalPages = 80,
            lastPageIndex = 5,
            importedAtEpochMs = 2000L
        )
        bookDao.insert(book)

        bookDao.delete("book-2")

        val loaded = bookDao.getById("book-2")
        assertNull(loaded)
    }

    @Test
    fun updateProgress_updatesPageAndOpenedTimestamp() = runTest {
        val book = BookEntity(
            id = "book-3",
            displayName = "Quiet.pdf",
            documentUri = "content://docs/3",
            totalPages = 300,
            lastPageIndex = 0,
            importedAtEpochMs = 1000L,
            lastOpenedAtEpochMs = null
        )
        bookDao.insert(book)

        bookDao.updateProgress(bookId = "book-3", page = 42, openedAt = 5000L)

        val updated = bookDao.getById("book-3")
        assertEquals(42, updated?.lastPageIndex)
        assertEquals(5000L, updated?.lastOpenedAtEpochMs)
    }

    @Test
    fun libraryOrdering_prioritizesRecentlyOpenedThenImported() = runTest {
        val bookOld = BookEntity(
            id = "old",
            displayName = "Old Book",
            documentUri = "content://docs/old",
            totalPages = 10,
            importedAtEpochMs = 1000L,
            lastOpenedAtEpochMs = 1000L
        )
        val bookRecentImport = BookEntity(
            id = "recent-import",
            displayName = "Recently Imported",
            documentUri = "content://docs/recent",
            totalPages = 20,
            importedAtEpochMs = 5000L,
            lastOpenedAtEpochMs = null
        )
        val bookRecentlyRead = BookEntity(
            id = "recently-read",
            displayName = "Recently Read",
            documentUri = "content://docs/read",
            totalPages = 30,
            importedAtEpochMs = 2000L,
            lastOpenedAtEpochMs = 8000L
        )

        bookDao.insert(bookOld)
        bookDao.insert(bookRecentImport)
        bookDao.insert(bookRecentlyRead)

        val list = bookDao.observeLibrary().first()

        assertEquals(3, list.size)
        // 1st: recently-read (8000L)
        assertEquals("recently-read", list[0].id)
        // 2nd: recent-import (COALESCE falls back to importedAt = 5000L)
        assertEquals("recent-import", list[1].id)
        // 3rd: old (1000L)
        assertEquals("old", list[2].id)
    }

    @Test
    fun duplicateUri_isIgnoredByInsert() = runTest {
        val book1 = BookEntity(
            id = "id-1",
            displayName = "First Book.pdf",
            documentUri = "content://docs/duplicate",
            totalPages = 50,
            importedAtEpochMs = 1000L
        )
        val book2 = BookEntity(
            id = "id-2",
            displayName = "Duplicate Book.pdf",
            documentUri = "content://docs/duplicate",
            totalPages = 50,
            importedAtEpochMs = 2000L
        )

        val row1 = bookDao.insert(book1)
        val row2 = bookDao.insert(book2)

        // row1 succeeded (> 0), row2 ignored (-1)
        assertEquals(true, row1 > 0)
        assertEquals(-1L, row2)

        val loaded = bookDao.getByUri("content://docs/duplicate")
        assertEquals("id-1", loaded?.id)
        assertEquals("First Book.pdf", loaded?.displayName)
    }

    @Test
    fun resetAllProgress_resetsPagesToZero() = runTest {
        bookDao.insert(
            BookEntity(
                id = "b1",
                displayName = "B1",
                documentUri = "content://docs/b1",
                totalPages = 100,
                lastPageIndex = 45,
                importedAtEpochMs = 1000L
            )
        )
        bookDao.insert(
            BookEntity(
                id = "b2",
                displayName = "B2",
                documentUri = "content://docs/b2",
                totalPages = 100,
                lastPageIndex = 90,
                importedAtEpochMs = 1000L
            )
        )

        bookDao.resetAllProgress()

        val list = bookDao.getAll()
        for (b in list) {
            assertEquals(0, b.lastPageIndex)
        }
    }
}
