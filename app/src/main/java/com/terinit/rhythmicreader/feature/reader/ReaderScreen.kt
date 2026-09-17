package com.terinit.rhythmicreader.feature.reader

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.pdf.compose.PdfViewerState
import com.terinit.rhythmicreader.R
import com.terinit.rhythmicreader.feature.reader.components.PdfReaderContent
import com.terinit.rhythmicreader.feature.recovery.RecoveryCard
import com.terinit.rhythmicreader.feature.recovery.RecoveryUiState
import com.terinit.rhythmicreader.ui.theme.CharcoalMuted
import com.terinit.rhythmicreader.ui.theme.CharcoalPrimary
import com.terinit.rhythmicreader.ui.theme.CharcoalSecondary
import com.terinit.rhythmicreader.ui.theme.SageGreenLight
import com.terinit.rhythmicreader.ui.theme.SageGreenPrimary
import com.terinit.rhythmicreader.ui.theme.WarmBackground
import com.terinit.rhythmicreader.ui.theme.WarmOutline
import com.terinit.rhythmicreader.ui.theme.WarmSurface
import com.terinit.rhythmicreader.ui.theme.WarmSurfaceVariant
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(androidx.pdf.ExperimentalPdfApi::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    uiState: ReaderUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var viewerState by remember { mutableStateOf<PdfViewerState?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.reassignDocumentUri(uri)
        }
    }

    val recoveryUiState by viewModel.recoveryUiState.collectAsStateWithLifecycle()
    var showRecoveryDevDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onAppForegroundChanged(true)
                    viewModel.onScreenInteractiveChanged(true)
                    viewModel.onReaderVisibleChanged(true)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.onAppForegroundChanged(false)
                    viewModel.onReaderVisibleChanged(false)
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(viewerState, recoveryUiState.sessionId) {
        if (recoveryUiState.sessionId == null) return@LaunchedEffect
        snapshotFlow { viewerState?.firstVisiblePage ?: -1 }
            .distinctUntilChanged()
            .collect { page ->
                if (page >= 0) {
                    viewModel.onPageChanged(page)
                }
            }
    }

    // Save final page upon back / exit
    BackHandler {
        viewModel.saveFinalProgress()
        onBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveFinalProgress()
        }
    }

    if (com.terinit.rhythmicreader.BuildConfig.DEBUG && showRecoveryDevDialog) {
        AlertDialog(
            onDismissRequest = { showRecoveryDevDialog = false },
            title = {
                Text(
                    text = "Recovery Session (Test Mode)",
                    style = MaterialTheme.typography.titleLarge,
                    color = CharcoalPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Pass 02 local verification. Start an internal reading session to test active time tracking and page qualification.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Current Status: ${recoveryUiState.status ?: "None"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CharcoalMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.startTestRecovery(requiredMinutes = 30, requiredPages = 10)
                        showRecoveryDevDialog = false
                    }
                ) {
                    Text("Standard (30m / 10p)", color = SageGreenPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.startTestRecovery(requiredMinutes = 1, requiredPages = 2)
                        showRecoveryDevDialog = false
                    }
                ) {
                    Text("Quick (1m / 2p)", color = CharcoalSecondary)
                }
            },
            containerColor = WarmSurface
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {
        // Top App Bar
        ReaderTopBar(
            title = uiState.displayTitle,
            recoveryUiState = recoveryUiState,
            onOpenRecoveryDevDialog = { showRecoveryDevDialog = true },
            onBack = {
                viewModel.saveFinalProgress()
                onBack()
            }
        )

        // Content area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = SageGreenPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading_document),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalSecondary
                        )
                    }
                }

                uiState.isDocumentUnavailable -> {
                    UnavailableDocumentView(
                        onChooseAgain = { filePicker.launch(arrayOf("application/pdf")) },
                        onBack = onBack
                    )
                }

                uiState.pdfDocument != null -> {
                    PdfReaderContent(
                        document = uiState.pdfDocument,
                        initialPage = uiState.currentPage,
                        onVisiblePageChanged = { page ->
                            viewModel.onPageChanged(page)
                        },
                        onViewerStateReady = { state ->
                            viewerState = state
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Recovery Card overlay when a session exists
            if (recoveryUiState.status != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.TopCenter)
                ) {
                    RecoveryCard(uiState = recoveryUiState)
                }
            }
        }

        // Bottom reading navigation bar
        if (uiState.pdfDocument != null && uiState.totalPages > 0) {
            ReaderBottomBar(
                currentPage = uiState.currentPage,
                totalPages = uiState.totalPages,
                onPreviousClick = {
                    if (uiState.currentPage > 0) {
                        coroutineScope.launch {
                            viewerState?.scrollToPage(uiState.currentPage - 1)
                        }
                    }
                },
                onNextClick = {
                    if (uiState.currentPage < uiState.totalPages - 1) {
                        coroutineScope.launch {
                            viewerState?.scrollToPage(uiState.currentPage + 1)
                        }
                    }
                },
                onPageSelected = { targetPage ->
                    coroutineScope.launch {
                        viewerState?.scrollToPage(targetPage)
                    }
                }
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    recoveryUiState: RecoveryUiState,
    onOpenRecoveryDevDialog: () -> Unit,
    onBack: () -> Unit
) {
    Surface(
        color = WarmSurface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.reader_back),
                    tint = CharcoalPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = CharcoalPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (recoveryUiState.isSessionActive) "Recovery Active" else "PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (recoveryUiState.isSessionActive) SageGreenPrimary else CharcoalMuted
                )
            }

            if (com.terinit.rhythmicreader.BuildConfig.DEBUG) {
                IconButton(onClick = onOpenRecoveryDevDialog) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = "Test Recovery Mode",
                        tint = if (recoveryUiState.isSessionActive) SageGreenPrimary else CharcoalMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPageSelected: (Int) -> Unit
) {
    Surface(
        color = WarmSurface,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.page_indicator, currentPage + 1, totalPages),
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousClick,
                    enabled = currentPage > 0,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WarmSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_page),
                        tint = if (currentPage > 0) CharcoalPrimary else CharcoalMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Slider(
                    value = (currentPage + 1).toFloat(),
                    onValueChange = { value ->
                        val target = (value.toInt() - 1).coerceIn(0, totalPages - 1)
                        onPageSelected(target)
                    },
                    valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                    steps = if (totalPages > 2) totalPages - 2 else 0,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = SageGreenPrimary,
                        activeTrackColor = SageGreenPrimary,
                        inactiveTrackColor = WarmOutline
                    )
                )

                IconButton(
                    onClick = onNextClick,
                    enabled = currentPage < totalPages - 1,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WarmSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_page),
                        tint = if (currentPage < totalPages - 1) CharcoalPrimary else CharcoalMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UnavailableDocumentView(
    onChooseAgain: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WarmOutline, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = WarmSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SageGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SageGreenPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.document_unavailable_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = CharcoalPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.document_unavailable_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = CharcoalSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onChooseAgain,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = WarmSurface
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(stringResource(R.string.choose_again))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CharcoalSecondary)
                ) {
                    Text(stringResource(R.string.reader_back))
                }
            }
        }
    }
}
