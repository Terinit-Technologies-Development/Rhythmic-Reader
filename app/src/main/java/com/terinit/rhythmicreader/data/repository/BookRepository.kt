package com.terinit.rhythmicreader.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.terinit.rhythmicreader.data.db.BookDao
import com.terinit.rhythmicreader.data.db.BookEntity
import com.terinit.rhythmicreader.data.documents.getDisplayName
import com.terinit.rhythmicreader.data.documents.getFileSize
import com.terinit.rhythmicreader.domain.model.Book
import com.terinit.rhythmicreader.domain.model.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

interface BookRepository {
    fun observeBooks(): Flow<List<Book>>
    suspend fun getBook(id: String): Book?
    suspend fun importBook(uri: Uri): Result<Book>
    suspend fun updateProgress(id: String, pageIndex: Int, openedAtEpochMs: Long = System.currentTimeMillis())
    suspend fun removeBook(id: String)
    suspend fun clearAll()
    suspend fun resetAllProgress()
    suspend fun calculateLibraryStorageBytes(): Long
}

class DefaultBookRepository(
    private val bookDao: BookDao,
    private val contentResolver: ContentResolver,
    private val pdfDocumentRepository: PdfDocumentRepository? = null
) : BookRepository {

    override fun observeBooks(): Flow<List<Book>> {
        return bookDao.observeLibrary().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getBook(id: String): Book? = withContext(Dispatchers.IO) {
        bookDao.getById(id)?.toDomain()
    }

    override suspend fun importBook(uri: Uri): Result<Book> = withContext(Dispatchers.IO) {
        runCatching {
            // Check for duplicate import
            val existing = bookDao.getByUri(uri.toString())
            if (existing != null) {
                val now = System.currentTimeMillis()
                bookDao.updateProgress(existing.id, existing.lastPageIndex, now)
                return@runCatching existing.copy(lastOpenedAtEpochMs = now).toDomain()
            }

            val displayName = contentResolver.getDisplayName(uri)

            // Attempt to determine total pages by safely opening through pdfDocumentRepository
            var pageCount: Int? = null
            if (pdfDocumentRepository != null) {
                try {
                    val document = pdfDocumentRepository.open(uri)
                    pageCount = document.pageCount
                    pdfDocumentRepository.closeCurrent()
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to load PDF: ${e.message}", e)
                }
            }

            val now = System.currentTimeMillis()
            val entity = BookEntity(
                id = UUID.randomUUID().toString(),
                displayName = displayName,
                documentUri = uri.toString(),
                totalPages = pageCount,
                lastPageIndex = 0,
                importedAtEpochMs = now,
                lastOpenedAtEpochMs = now
            )

            bookDao.insert(entity)
            entity.toDomain()
        }
    }

    override suspend fun updateProgress(
        id: String,
        pageIndex: Int,
        openedAtEpochMs: Long
    ) = withContext(Dispatchers.IO) {
        bookDao.updateProgress(id, pageIndex, openedAtEpochMs)
    }

    override suspend fun removeBook(id: String) = withContext(Dispatchers.IO) {
        // Only removes reference from database, NEVER deletes source PDF
        bookDao.delete(id)
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        bookDao.deleteAll()
    }

    override suspend fun resetAllProgress() = withContext(Dispatchers.IO) {
        bookDao.resetAllProgress()
    }

    override suspend fun calculateLibraryStorageBytes(): Long = withContext(Dispatchers.IO) {
        val list = bookDao.getAll()
        var total = 0L
        for (book in list) {
            val size = contentResolver.getFileSize(Uri.parse(book.documentUri))
            if (size != null && size > 0) {
                total += size
            }
        }
        total
    }
}
