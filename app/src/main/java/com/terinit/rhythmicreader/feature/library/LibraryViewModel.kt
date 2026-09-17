package com.terinit.rhythmicreader.feature.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.terinit.rhythmicreader.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(
    private val repository: BookRepository
) : ViewModel() {

    private val _isImporting = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.observeBooks(),
        _isImporting,
        _userMessage
    ) { books, isImporting, userMessage ->
        LibraryUiState(
            books = books,
            isLoading = false,
            userMessage = userMessage,
            isImporting = isImporting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState()
    )

    fun importPdf(uri: Uri, onImported: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isImporting.value = true
            try {
                val result = repository.importBook(uri)
                result.fold(
                    onSuccess = { book ->
                        onImported(book.id)
                    },
                    onFailure = { error ->
                        _userMessage.value = "Failed to import PDF: ${error.localizedMessage ?: "Invalid file"}"
                    }
                )
            } catch (e: Exception) {
                _userMessage.value = "Failed to import PDF: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    fun removeBook(bookId: String) {
        viewModelScope.launch {
            repository.removeBook(bookId)
        }
    }

    fun dismissUserMessage() {
        _userMessage.value = null
    }

    companion object {
        fun provideFactory(
            repository: BookRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(repository) as T
            }
        }
    }
}
