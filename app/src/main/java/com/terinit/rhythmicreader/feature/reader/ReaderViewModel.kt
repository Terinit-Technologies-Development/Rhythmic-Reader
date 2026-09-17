package com.terinit.rhythmicreader.feature.reader

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.terinit.rhythmicreader.data.repository.BookRepository
import com.terinit.rhythmicreader.data.repository.PdfDocumentRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ReaderViewModel(
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val pdfDocumentRepository: PdfDocumentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val pageChangeFlow = MutableStateFlow(0)
    private var lastPersistedPage: Int = -1

    init {
        loadDocument()
        observePageChangesForPersistence()
    }

    private fun loadDocument() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isDocumentUnavailable = false, errorMessage = null) }
            val book = bookRepository.getBook(bookId)
            if (book == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isDocumentUnavailable = true,
                        errorMessage = "Book not found"
                    )
                }
                return@launch
            }

            try {
                val doc = pdfDocumentRepository.open(book.documentUri)
                val totalPages = doc.pageCount
                val initialPage = book.lastPageIndex.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                lastPersistedPage = initialPage
                pageChangeFlow.value = initialPage

                _uiState.update {
                    it.copy(
                        book = book,
                        pdfDocument = doc,
                        currentPage = initialPage,
                        totalPages = totalPages,
                        isLoading = false,
                        isDocumentUnavailable = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        book = book,
                        isLoading = false,
                        isDocumentUnavailable = true,
                        errorMessage = e.localizedMessage ?: "Failed to open document"
                    )
                }
            }
        }
    }

    private fun observePageChangesForPersistence() {
        viewModelScope.launch {
            pageChangeFlow
                .debounce(500)
                .collect { page ->
                    persistPage(page)
                }
        }
    }

    fun onPageChanged(page: Int) {
        if (page < 0) return
        _uiState.update { it.copy(currentPage = page) }
        pageChangeFlow.value = page
    }

    fun saveFinalProgress() {
        val current = _uiState.value.currentPage
        persistPage(current)
    }

    private fun persistPage(page: Int) {
        if (page == lastPersistedPage && lastPersistedPage != -1) return
        lastPersistedPage = page
        viewModelScope.launch {
            bookRepository.updateProgress(bookId, page)
        }
    }

    fun retryLoading() {
        loadDocument()
    }

    fun reassignDocumentUri(newUri: Uri) {
        viewModelScope.launch {
            loadDocument()
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveFinalProgress()
        pdfDocumentRepository.closeCurrent()
    }

    companion object {
        fun provideFactory(
            bookId: String,
            bookRepository: BookRepository,
            pdfDocumentRepository: PdfDocumentRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReaderViewModel(bookId, bookRepository, pdfDocumentRepository) as T
            }
        }
    }
}
