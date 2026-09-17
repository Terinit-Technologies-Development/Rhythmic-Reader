package com.terinit.rhythmicreader.feature.reader.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.pdf.PdfDocument
import androidx.pdf.compose.PdfViewer
import androidx.pdf.compose.PdfViewerState
import androidx.pdf.compose.rememberPdfViewerState
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(androidx.pdf.ExperimentalPdfApi::class)
@Composable
fun PdfReaderContent(
    document: PdfDocument?,
    initialPage: Int,
    onVisiblePageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onViewerStateReady: (PdfViewerState) -> Unit = {}
) {
    val viewerState = rememberPdfViewerState()

    var initialPositionApplied by remember(document) {
        mutableStateOf(false)
    }

    LaunchedEffect(viewerState) {
        onViewerStateReady(viewerState)
    }

    LaunchedEffect(document, initialPage) {
        if (document != null && !initialPositionApplied && initialPage > 0) {
            viewerState.scrollToPage(initialPage)
            initialPositionApplied = true
        }
    }

    LaunchedEffect(viewerState, document) {
        if (document == null) return@LaunchedEffect

        snapshotFlow {
            viewerState.firstVisiblePage
        }
            .distinctUntilChanged()
            .collect { page ->
                onVisiblePageChanged(page)
            }
    }

    if (document != null) {
        PdfViewer(
            pdfDocument = document,
            state = viewerState,
            modifier = modifier.fillMaxSize(),
            isFormFillingEnabled = false,
            isImageSelectionEnabled = false
        )
    }
}
