package com.terinit.rhythmicreader.feature.reader

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.terinit.rhythmicreader.data.repository.BookRepository
import com.terinit.rhythmicreader.data.repository.PdfDocumentRepository
import com.terinit.rhythmicreader.data.system.ScreenStateReader
import com.terinit.rhythmicreader.domain.model.RecoveryRequirement
import com.terinit.rhythmicreader.domain.model.RecoverySession
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import com.terinit.rhythmicreader.domain.recovery.RecoveryCoordinator
import com.terinit.rhythmicreader.feature.recovery.RecoveryUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ReaderViewModel(
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val pdfDocumentRepository: PdfDocumentRepository,
    private val recoveryCoordinator: RecoveryCoordinator? = null,
    private val screenStateReader: ScreenStateReader? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private val _recoveryUiState = MutableStateFlow(RecoveryUiState())
    val recoveryUiState: StateFlow<RecoveryUiState> = _recoveryUiState.asStateFlow()

    private val pageChangeFlow = MutableStateFlow(0)
    private var lastPersistedPage: Int = -1

    init {
        loadDocument()
        observePageChangesForPersistence()
        initRecoveryCoordination()
    }

    private fun initRecoveryCoordination() {
        if (recoveryCoordinator == null) return

        viewModelScope.launch {
            recoveryCoordinator.currentSession.collect { session ->
                updateRecoveryUi(session)
            }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val session = recoveryCoordinator.currentSession.value
                if (session != null && session.status == RecoveryStatus.ACTIVE) {
                    updateRecoveryUi(session)
                }
            }
        }
    }

    private fun updateRecoveryUi(session: RecoverySession?) {
        if (session == null) {
            _recoveryUiState.value = RecoveryUiState()
            return
        }
        _recoveryUiState.update {
            it.copy(
                sessionId = session.sessionId,
                status = session.status,
                activeSeconds = recoveryCoordinator?.getActiveReadingSeconds() ?: session.activeSeconds,
                requiredActiveSeconds = session.requirement.requiredActiveSeconds,
                qualifiedPages = session.qualifiedPages,
                requiredQualifiedPages = session.requirement.requiredQualifiedPages
            )
        }
    }

    private fun loadDocument() {
        viewModelScope.launch {
            recoveryCoordinator?.updateReaderVisible(false)
            recoveryCoordinator?.updateDocumentLoaded(false, null)
            recoveryCoordinator?.performCheckpoint()
            _uiState.update { it.copy(isLoading = true, isDocumentUnavailable = false, errorMessage = null) }
            val book = bookRepository.getBook(bookId)
            if (book == null) {
                recoveryCoordinator?.updateDocumentLoaded(false, null)
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
                recoveryCoordinator?.updateDocumentLoaded(true, bookId)
                recoveryCoordinator?.onVisiblePageChanged(bookId, initialPage)
            } catch (e: Exception) {
                recoveryCoordinator?.updateDocumentLoaded(false, null)
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
        viewModelScope.launch {
            recoveryCoordinator?.onVisiblePageChanged(bookId, page)
        }
    }

    fun onAppForegroundChanged(isForeground: Boolean) {
        recoveryCoordinator?.updateAppForeground(isForeground)
        if (!isForeground) recoveryCoordinator?.flushPendingEvidenceAsync()
    }

    fun onScreenInteractiveChanged(isInteractive: Boolean) {
        recoveryCoordinator?.updateScreenInteractive(isInteractive)
        if (!isInteractive) recoveryCoordinator?.flushPendingEvidenceAsync()
    }

    fun refreshScreenInteractive() {
        onScreenInteractiveChanged(screenStateReader?.isInteractive() ?: false)
    }

    fun onReaderVisibleChanged(isVisible: Boolean) {
        recoveryCoordinator?.updateReaderVisible(isVisible)
        if (!isVisible) recoveryCoordinator?.flushPendingEvidenceAsync()
    }

    fun startTestRecovery(requiredMinutes: Long = 30, requiredPages: Int = 10) {
        viewModelScope.launch {
            recoveryCoordinator?.startSession(
                RecoveryRequirement(
                    requiredActiveSeconds = requiredMinutes * 60L,
                    requiredQualifiedPages = requiredPages
                )
            )
        }
    }

    fun saveFinalProgress() {
        val current = _uiState.value.currentPage
        persistPage(current)
        viewModelScope.launch {
            recoveryCoordinator?.performCheckpoint()
        }
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
        recoveryCoordinator?.updateReaderVisible(false)
        recoveryCoordinator?.updateDocumentLoaded(false, null)
        recoveryCoordinator?.flushPendingEvidenceAsync()
        saveFinalProgress()
        pdfDocumentRepository.closeCurrent()
    }

    companion object {
        fun provideFactory(
            bookId: String,
            bookRepository: BookRepository,
            pdfDocumentRepository: PdfDocumentRepository,
            recoveryCoordinator: RecoveryCoordinator? = null,
            screenStateReader: ScreenStateReader? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReaderViewModel(
                    bookId = bookId,
                    bookRepository = bookRepository,
                    pdfDocumentRepository = pdfDocumentRepository,
                    recoveryCoordinator = recoveryCoordinator,
                    screenStateReader = screenStateReader
                ) as T
            }
        }
    }
}
