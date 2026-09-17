package com.terinit.rhythmicreader.feature.library

import android.net.Uri
import com.terinit.rhythmicreader.data.repository.BookRepository
import com.terinit.rhythmicreader.domain.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val booksFlow = MutableStateFlow<List<Book>>(emptyList())
    private val fakeRepository = FakeBookRepository(booksFlow)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsBooksFromRepository() = runTest(testDispatcher) {
        val book = Book(
            id = "test-1",
            displayName = "Mindfulness.pdf",
            documentUri = Uri.parse("content://test/1"),
            totalPages = 100,
            lastPageIndex = 10,
            importedAtEpochMs = 1000L
        )
        booksFlow.value = listOf(book)

        val viewModel = LibraryViewModel(fakeRepository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.books.size)
        assertEquals("Mindfulness.pdf", state.books[0].displayName)
        assertEquals(1, state.recentBooks.size)
        collectJob.cancel()
    }

    @Test
    fun importFailure_setsUserMessage() = runTest(testDispatcher) {
        fakeRepository.failNextImport = true
        val viewModel = LibraryViewModel(fakeRepository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.importPdf(Uri.parse("content://corrupt.pdf"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.userMessage)
        assertTrue(state.userMessage!!.contains("Failed to import PDF"))

        viewModel.dismissUserMessage()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.userMessage)
        collectJob.cancel()
    }

    @Test
    fun removeBook_callsRepositoryRemove() = runTest(testDispatcher) {
        val book = Book(
            id = "test-remove",
            displayName = "Book to delete.pdf",
            documentUri = Uri.parse("content://test/delete"),
            totalPages = 50,
            lastPageIndex = 0,
            importedAtEpochMs = 1000L
        )
        booksFlow.value = listOf(book)
        val viewModel = LibraryViewModel(fakeRepository)
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        advanceUntilIdle()

        viewModel.removeBook("test-remove")
        advanceUntilIdle()

        assertEquals(listOf("test-remove"), fakeRepository.deletedIds)
        collectJob.cancel()
    }

    private class FakeBookRepository(
        private val flow: MutableStateFlow<List<Book>>
    ) : BookRepository {
        var failNextImport = false
        val deletedIds = mutableListOf<String>()

        override fun observeBooks(): Flow<List<Book>> = flow

        override suspend fun getBook(id: String): Book? = flow.value.find { it.id == id }

        override suspend fun importBook(uri: Uri): Result<Book> {
            if (failNextImport) {
                return Result.failure(IllegalArgumentException("Corrupted PDF file"))
            }
            val book = Book(
                id = "imported-id",
                displayName = "Imported Book.pdf",
                documentUri = uri,
                totalPages = 100,
                lastPageIndex = 0,
                importedAtEpochMs = System.currentTimeMillis()
            )
            flow.value = listOf(book) + flow.value
            return Result.success(book)
        }

        override suspend fun updateProgress(id: String, pageIndex: Int, openedAtEpochMs: Long) {}
        override suspend fun removeBook(id: String) {
            deletedIds.add(id)
            flow.value = flow.value.filterNot { it.id == id }
        }
        override suspend fun clearAll() { flow.value = emptyList() }
        override suspend fun resetAllProgress() {}
        override suspend fun calculateLibraryStorageBytes(): Long = 0L
    }
}
