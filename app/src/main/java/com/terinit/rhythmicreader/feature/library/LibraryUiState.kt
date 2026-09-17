package com.terinit.rhythmicreader.feature.library

import com.terinit.rhythmicreader.domain.model.Book

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val userMessage: String? = null,
    val isImporting: Boolean = false
) {
    val recentBooks: List<Book>
        get() = books.take(4)

    val allBooks: List<Book>
        get() = books
}
