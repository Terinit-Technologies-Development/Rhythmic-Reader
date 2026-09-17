package com.terinit.rhythmicreader.data.repository

import android.content.Context
import android.net.Uri
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface PdfDocumentRepository {
    suspend fun open(uri: Uri): PdfDocument
    fun closeCurrent()
}

class AndroidPdfDocumentRepository(
    context: Context
) : PdfDocumentRepository {

    private val loader = SandboxedPdfLoader(context.applicationContext)
    private val mutex = Mutex()
    private var currentDocument: PdfDocument? = null
    private var currentUri: Uri? = null

    override suspend fun open(uri: Uri): PdfDocument = mutex.withLock {
        val active = currentDocument
        if (active != null && currentUri == uri) {
            return active
        }

        closeInternal()

        val doc = loader.openDocument(
            uri = uri,
            password = null
        )
        currentDocument = doc
        currentUri = uri
        doc
    }

    override fun closeCurrent() {
        closeInternal()
    }

    private fun closeInternal() {
        try {
            currentDocument?.close()
        } catch (_: Exception) {
        } finally {
            currentDocument = null
            currentUri = null
        }
    }
}
