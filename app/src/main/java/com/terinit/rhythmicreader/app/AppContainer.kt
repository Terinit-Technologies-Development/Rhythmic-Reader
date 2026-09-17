package com.terinit.rhythmicreader.app

import android.content.Context
import androidx.room.Room
import com.terinit.rhythmicreader.data.db.ReaderDatabase
import com.terinit.rhythmicreader.data.repository.AndroidPdfDocumentRepository
import com.terinit.rhythmicreader.data.repository.BookRepository
import com.terinit.rhythmicreader.data.repository.DefaultBookRepository
import com.terinit.rhythmicreader.data.repository.PdfDocumentRepository

class AppContainer(
    context: Context
) {
    val database: ReaderDatabase =
        Room.databaseBuilder(
            context,
            ReaderDatabase::class.java,
            "rhythmic-reader.db"
        ).build()

    val pdfDocumentRepository: PdfDocumentRepository =
        AndroidPdfDocumentRepository(
            context = context
        )

    val bookRepository: BookRepository =
        DefaultBookRepository(
            bookDao = database.bookDao(),
            contentResolver = context.contentResolver,
            pdfDocumentRepository = pdfDocumentRepository
        )
}
