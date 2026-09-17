package com.terinit.rhythmicreader.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.terinit.rhythmicreader.feature.library.LibraryScreen
import com.terinit.rhythmicreader.feature.library.LibraryViewModel
import com.terinit.rhythmicreader.feature.reader.ReaderScreen
import com.terinit.rhythmicreader.feature.reader.ReaderViewModel
import com.terinit.rhythmicreader.feature.settings.SettingsScreen
import com.terinit.rhythmicreader.feature.settings.SettingsViewModel

sealed interface Screen {
    data object Library : Screen
    data class Reader(val bookId: String) : Screen
    data object Settings : Screen
}

@Composable
fun ReaderApp(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Library) }

    when (val screen = currentScreen) {
        is Screen.Library -> {
            val libraryViewModel: LibraryViewModel = viewModel(
                factory = LibraryViewModel.provideFactory(container.bookRepository)
            )
            val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()

            LibraryScreen(
                uiState = uiState,
                onImportPdf = { uri ->
                    libraryViewModel.importPdf(uri) { bookId ->
                        currentScreen = Screen.Reader(bookId)
                    }
                },
                onOpenBook = { bookId ->
                    currentScreen = Screen.Reader(bookId)
                },
                onRemoveBook = { bookId ->
                    libraryViewModel.removeBook(bookId)
                },
                onNavigateToSettings = {
                    currentScreen = Screen.Settings
                },
                onDismissMessage = {
                    libraryViewModel.dismissUserMessage()
                },
                modifier = modifier.fillMaxSize()
            )
        }

        is Screen.Reader -> {
            val readerViewModel: ReaderViewModel = viewModel(
                key = "reader_${screen.bookId}",
                factory = ReaderViewModel.provideFactory(
                    bookId = screen.bookId,
                    bookRepository = container.bookRepository,
                    pdfDocumentRepository = container.pdfDocumentRepository
                )
            )
            val uiState by readerViewModel.uiState.collectAsStateWithLifecycle()

            ReaderScreen(
                viewModel = readerViewModel,
                uiState = uiState,
                onBack = {
                    currentScreen = Screen.Library
                },
                modifier = modifier.fillMaxSize()
            )
        }

        is Screen.Settings -> {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(container.bookRepository)
            )
            val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

            SettingsScreen(
                uiState = uiState,
                onClearReadingHistory = {
                    settingsViewModel.clearReadingHistory()
                },
                onClearLibrary = {
                    settingsViewModel.clearEntireLibrary()
                },
                onDismissNotification = {
                    settingsViewModel.dismissNotification()
                },
                onBack = {
                    currentScreen = Screen.Library
                },
                modifier = modifier.fillMaxSize()
            )
        }
    }
}
