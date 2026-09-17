package com.terinit.rhythmicreader.feature.settings

import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.terinit.rhythmicreader.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshStorageInfo()
    }

    fun refreshStorageInfo() {
        viewModelScope.launch {
            val books = bookRepository.observeBooks().first()
            val usage = bookRepository.calculateLibraryStorageBytes()
            val available = getAvailableDeviceStorage()

            _uiState.update {
                it.copy(
                    bookCount = books.size,
                    libraryStorageBytes = usage,
                    deviceAvailableBytes = available
                )
            }
        }
    }

    private fun getAvailableDeviceStorage(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            0L
        }
    }

    fun clearReadingHistory() {
        viewModelScope.launch {
            bookRepository.resetAllProgress()
            _uiState.update { it.copy(userNotification = "Reading history reset.") }
        }
    }

    fun clearEntireLibrary() {
        viewModelScope.launch {
            bookRepository.clearAll()
            refreshStorageInfo()
            _uiState.update { it.copy(userNotification = "Library cleared.") }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }

    companion object {
        fun provideFactory(
            bookRepository: BookRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(bookRepository) as T
            }
        }
    }
}
