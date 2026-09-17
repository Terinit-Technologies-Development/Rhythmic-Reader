package com.terinit.rhythmicreader.feature.reader

import androidx.pdf.PdfDocument
import com.terinit.rhythmicreader.domain.model.Book

data class ReaderUiState(
    val book: Book? = null,
    val pdfDocument: PdfDocument? = null,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val isLoading: Boolean = true,
    val isDocumentUnavailable: Boolean = false,
    val errorMessage: String? = null
) {
    val displayTitle: String
        get() = book?.displayName ?: ""

    val progressFraction: Float
        get() = if (totalPages > 0) {
            ((currentPage + 1).toFloat() / totalPages).coerceIn(0f, 1f)
        } else 0f
}
