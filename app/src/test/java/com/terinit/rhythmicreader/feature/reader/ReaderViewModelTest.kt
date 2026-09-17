package com.terinit.rhythmicreader.feature.reader

import android.net.Uri
import androidx.pdf.PdfDocument
import com.terinit.rhythmicreader.data.repository.BookRepository
import com.terinit.rhythmicreader.data.repository.PdfDocumentRepository
import com.terinit.rhythmicreader.domain.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun missingBook_setsDocumentUnavailable() = runTest(testDispatcher) {
        val emptyBookRepo = object : BookRepository {
            override fun observeBooks(): Flow<List<Book>> = emptyFlow()
            override suspend fun getBook(id: String): Book? = null
            override suspend fun importBook(uri: Uri): Result<Book> = Result.failure(Exception())
            override suspend fun updateProgress(id: String, pageIndex: Int, openedAtEpochMs: Long) {}
            override suspend fun removeBook(id: String) {}
            override suspend fun clearAll() {}
            override suspend fun resetAllProgress() {}
            override suspend fun calculateLibraryStorageBytes(): Long = 0L
        }
        val fakePdfRepo = object : PdfDocumentRepository {
            override suspend fun open(uri: Uri): PdfDocument = throw IllegalStateException("Not found")
            override fun closeCurrent() {}
        }

        val viewModel = ReaderViewModel(
            bookId = "non-existent",
            bookRepository = emptyBookRepo,
            pdfDocumentRepository = fakePdfRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isDocumentUnavailable)
    }

    @Test
    fun loadDocument_resumesAtSavedPage() = runTest(testDispatcher) {
        val book = Book(
            id = "b-1",
            displayName = "Calm Reading.pdf",
            documentUri = Uri.parse("content://test/b-1"),
            totalPages = 250,
            lastPageIndex = 45,
            importedAtEpochMs = 1000L
        )
        val mockDoc = mock(PdfDocument::class.java)
        `when`(mockDoc.pageCount).thenReturn(250)

        val bookRepo = object : BookRepository {
            override fun observeBooks(): Flow<List<Book>> = emptyFlow()
            override suspend fun getBook(id: String): Book? = book
            override suspend fun importBook(uri: Uri): Result<Book> = Result.success(book)
            override suspend fun updateProgress(id: String, pageIndex: Int, openedAtEpochMs: Long) {}
            override suspend fun removeBook(id: String) {}
            override suspend fun clearAll() {}
            override suspend fun resetAllProgress() {}
            override suspend fun calculateLibraryStorageBytes(): Long = 0L
        }
        val fakePdfRepo = object : PdfDocumentRepository {
            override suspend fun open(uri: Uri): PdfDocument = mockDoc
            override fun closeCurrent() {}
        }

        val viewModel = ReaderViewModel(
            bookId = "b-1",
            bookRepository = bookRepo,
            pdfDocumentRepository = fakePdfRepo
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isDocumentUnavailable)
        assertEquals(45, state.currentPage)
        assertEquals(250, state.totalPages)
    }

    @Test
    fun pageChange_persistsDebouncedProgress() = runTest(testDispatcher) {
        val book = Book(
            id = "b-2",
            displayName = "Quiet.pdf",
            documentUri = Uri.parse("content://test/b-2"),
            totalPages = 100,
            lastPageIndex = 0,
            importedAtEpochMs = 1000L
        )
        val mockDoc = mock(PdfDocument::class.java)
        `when`(mockDoc.pageCount).thenReturn(100)

        var lastPersistedPage: Int? = null
        val bookRepo = object : BookRepository {
            override fun observeBooks(): Flow<List<Book>> = emptyFlow()
            override suspend fun getBook(id: String): Book? = book
            override suspend fun importBook(uri: Uri): Result<Book> = Result.success(book)
            override suspend fun updateProgress(id: String, pageIndex: Int, openedAtEpochMs: Long) {
                lastPersistedPage = pageIndex
            }
            override suspend fun removeBook(id: String) {}
            override suspend fun clearAll() {}
            override suspend fun resetAllProgress() {}
            override suspend fun calculateLibraryStorageBytes(): Long = 0L
        }
        val fakePdfRepo = object : PdfDocumentRepository {
            override suspend fun open(uri: Uri): PdfDocument = mockDoc
            override fun closeCurrent() {}
        }

        val viewModel = ReaderViewModel(
            bookId = "b-2",
            bookRepository = bookRepo,
            pdfDocumentRepository = fakePdfRepo
        )
        advanceUntilIdle()

        viewModel.onPageChanged(10)
        // Before 500ms debounce
        advanceTimeBy(200)
        // Then changed again before debounce expires
        viewModel.onPageChanged(12)
        advanceTimeBy(600)
        advanceUntilIdle()

        assertEquals(12, lastPersistedPage)
    }

    @Test
    fun saveFinalProgress_immediatelyPersistsCurrentPage() = runTest(testDispatcher) {
        val book = Book(
            id = "b-3",
            displayName = "Quiet.pdf",
            documentUri = Uri.parse("content://test/b-3"),
            totalPages = 100,
            lastPageIndex = 0,
            importedAtEpochMs = 1000L
        )
        val mockDoc = mock(PdfDocument::class.java)
        `when`(mockDoc.pageCount).thenReturn(100)

        var savedPage: Int? = null
        val bookRepo = object : BookRepository {
            override fun observeBooks(): Flow<List<Book>> = emptyFlow()
            override suspend fun getBook(id: String): Book? = book
            override suspend fun importBook(uri: Uri): Result<Book> = Result.success(book)
            override suspend fun updateProgress(id: String, pageIndex: Int, openedAtEpochMs: Long) {
                savedPage = pageIndex
            }
            override suspend fun removeBook(id: String) {}
            override suspend fun clearAll() {}
            override suspend fun resetAllProgress() {}
            override suspend fun calculateLibraryStorageBytes(): Long = 0L
        }
        val fakePdfRepo = object : PdfDocumentRepository {
            override suspend fun open(uri: Uri): PdfDocument = mockDoc
            override fun closeCurrent() {}
        }

        val viewModel = ReaderViewModel(
            bookId = "b-3",
            bookRepository = bookRepo,
            pdfDocumentRepository = fakePdfRepo
        )
        advanceUntilIdle()

        viewModel.onPageChanged(25)
        viewModel.saveFinalProgress()
        advanceUntilIdle()

        assertEquals(25, savedPage)
    }
}
