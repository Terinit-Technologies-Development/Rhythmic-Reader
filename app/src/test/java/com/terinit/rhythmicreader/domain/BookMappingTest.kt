package com.terinit.rhythmicreader.domain

import android.net.Uri
import com.terinit.rhythmicreader.data.db.BookEntity
import com.terinit.rhythmicreader.domain.model.Book
import com.terinit.rhythmicreader.domain.model.toDomain
import com.terinit.rhythmicreader.domain.model.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BookMappingTest {

    @Test
    fun entityToDomain_mapsAllFieldsCorrectly() {
        val entity = BookEntity(
            id = "test-id-1",
            displayName = "Calm Reading.pdf",
            documentUri = "content://com.android.providers.media.documents/document/123",
            totalPages = 120,
            lastPageIndex = 14,
            importedAtEpochMs = 1000L,
            lastOpenedAtEpochMs = 2000L
        )

        val domain = entity.toDomain()

        assertEquals("test-id-1", domain.id)
        assertEquals("Calm Reading.pdf", domain.displayName)
        assertEquals("content://com.android.providers.media.documents/document/123", domain.documentUri.toString())
        assertEquals(120, domain.totalPages)
        assertEquals(14, domain.lastPageIndex)
        assertEquals(1000L, domain.importedAtEpochMs)
        assertEquals(2000L, domain.lastOpenedAtEpochMs)
    }

    @Test
    fun domainToEntity_mapsAllFieldsCorrectly() {
        val domain = Book(
            id = "test-id-2",
            displayName = "Quiet Habits.pdf",
            documentUri = Uri.parse("content://com.android.providers.media.documents/document/456"),
            totalPages = 200,
            lastPageIndex = 50,
            importedAtEpochMs = 3000L,
            lastOpenedAtEpochMs = null
        )

        val entity = domain.toEntity()

        assertEquals("test-id-2", entity.id)
        assertEquals("Quiet Habits.pdf", entity.displayName)
        assertEquals("content://com.android.providers.media.documents/document/456", entity.documentUri)
        assertEquals(200, entity.totalPages)
        assertEquals(50, entity.lastPageIndex)
        assertEquals(3000L, entity.importedAtEpochMs)
        assertNull(entity.lastOpenedAtEpochMs)
    }

    @Test
    fun progressPercentage_computesAccurately() {
        val bookHalfway = Book(
            id = "1",
            displayName = "Book",
            documentUri = Uri.parse("content://test"),
            totalPages = 100,
            lastPageIndex = 49, // page 50 of 100
            importedAtEpochMs = 1L
        )
        assertEquals(50, bookHalfway.progressPercentage)

        val bookFinished = Book(
            id = "2",
            displayName = "Book",
            documentUri = Uri.parse("content://test"),
            totalPages = 100,
            lastPageIndex = 99, // page 100 of 100
            importedAtEpochMs = 1L
        )
        assertEquals(100, bookFinished.progressPercentage)

        val bookUnknownTotal = Book(
            id = "3",
            displayName = "Book",
            documentUri = Uri.parse("content://test"),
            totalPages = null,
            lastPageIndex = 5,
            importedAtEpochMs = 1L
        )
        assertEquals(0, bookUnknownTotal.progressPercentage)

        val bookZeroPages = Book(
            id = "4",
            displayName = "Book",
            documentUri = Uri.parse("content://test"),
            totalPages = 0,
            lastPageIndex = 0,
            importedAtEpochMs = 1L
        )
        assertEquals(0, bookZeroPages.progressPercentage)
    }
}
