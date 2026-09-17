package com.terinit.rhythmicreader.feature.library

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terinit.rhythmicreader.R
import com.terinit.rhythmicreader.domain.model.Book
import com.terinit.rhythmicreader.ui.theme.CharcoalMuted
import com.terinit.rhythmicreader.ui.theme.CharcoalPrimary
import com.terinit.rhythmicreader.ui.theme.CharcoalSecondary
import com.terinit.rhythmicreader.ui.theme.SageGreenContainer
import com.terinit.rhythmicreader.ui.theme.SageGreenLight
import com.terinit.rhythmicreader.ui.theme.SageGreenPrimary
import com.terinit.rhythmicreader.ui.theme.WarmBackground
import com.terinit.rhythmicreader.ui.theme.WarmOutline
import com.terinit.rhythmicreader.ui.theme.WarmSurface
import com.terinit.rhythmicreader.ui.theme.WarmSurfaceVariant

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onImportPdf: (android.net.Uri) -> Unit,
    onOpenBook: (String) -> Unit,
    onRemoveBook: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var bookPendingRemoval by remember { mutableStateOf<Book?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        onImportPdf(uri)
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LibraryHeader(
                onNavigateToSettings = onNavigateToSettings
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SageGreenPrimary
                    )
                }
            } else if (uiState.books.isEmpty()) {
                EmptyLibraryContent(
                    isImporting = uiState.isImporting,
                    onImportClick = {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    }
                )
            } else {
                PopulatedLibraryContent(
                    books = uiState.books,
                    recentBooks = uiState.recentBooks,
                    isImporting = uiState.isImporting,
                    onImportClick = {
                        pdfPicker.launch(arrayOf("application/pdf"))
                    },
                    onOpenBook = onOpenBook,
                    onRequestRemoveBook = { book ->
                        bookPendingRemoval = book
                    }
                )
            }
        }
    }

    // Removal confirmation dialog
    bookPendingRemoval?.let { book ->
        AlertDialog(
            onDismissRequest = { bookPendingRemoval = null },
            title = {
                Text(
                    text = stringResource(R.string.remove_confirm_title),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.remove_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveBook(book.id)
                        bookPendingRemoval = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { bookPendingRemoval = null }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = CharcoalSecondary
                    )
                }
            },
            containerColor = WarmSurface
        )
    }
}

@Composable
private fun LibraryHeader(
    onNavigateToSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_leaf),
                contentDescription = null,
                tint = SageGreenPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = CharcoalPrimary
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalSecondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.header_quote),
                style = MaterialTheme.typography.labelSmall,
                color = CharcoalMuted,
                modifier = Modifier.padding(end = 6.dp)
            )
            IconButton(
                onClick = onNavigateToSettings
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings_title),
                    tint = CharcoalSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryContent(
    isImporting: Boolean,
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Main import card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WarmOutline, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Calm illustration placeholder
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(SageGreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pdf_document),
                        contentDescription = null,
                        tint = SageGreenPrimary,
                        modifier = Modifier.size(54.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.empty_title),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = CharcoalSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onImportClick,
                    enabled = !isImporting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreenPrimary,
                        contentColor = WarmSurface
                    ),
                    shape = RoundedCornerShape(28.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            color = WarmSurface,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.import_pdf),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // Reassurance items
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HorizontalDivider(color = WarmOutline)

            FeatureTile(
                icon = Icons.Default.PhoneAndroid,
                title = stringResource(R.string.feature_device_title),
                description = stringResource(R.string.feature_device_desc)
            )

            FeatureTile(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.feature_private_title),
                description = stringResource(R.string.feature_private_desc)
            )
        }
    }
}

@Composable
private fun PopulatedLibraryContent(
    books: List<Book>,
    recentBooks: List<Book>,
    isImporting: Boolean,
    onImportClick: () -> Unit,
    onOpenBook: (String) -> Unit,
    onRequestRemoveBook: (Book) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Import Banner Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WarmOutline, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SageGreenContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.import_a_pdf),
                            style = MaterialTheme.typography.titleMedium,
                            color = CharcoalPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.import_card_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onImportClick,
                            enabled = !isImporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SageGreenPrimary,
                                contentColor = WarmSurface
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    color = WarmSurface,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = stringResource(R.string.import_pdf),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // Recent Books Row
        if (recentBooks.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.recent_books),
                            style = MaterialTheme.typography.titleMedium,
                            color = CharcoalPrimary
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recentBooks, key = { it.id }) { book ->
                            RecentBookCard(
                                book = book,
                                onClick = { onOpenBook(book.id) }
                            )
                        }
                    }
                }
            }
        }

        // All Books Section
        item {
            Text(
                text = stringResource(R.string.all_books),
                style = MaterialTheme.typography.titleMedium,
                color = CharcoalPrimary,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        items(books, key = { it.id }) { book ->
            LibraryBookRow(
                book = book,
                onClick = { onOpenBook(book.id) },
                onRemove = { onRequestRemoveBook(book) }
            )
        }
    }
}

@Composable
private fun RecentBookCard(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .border(1.dp, WarmOutline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Elegant cover representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(WarmSurfaceVariant)
                    .border(1.dp, WarmOutline, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = book.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = CharcoalPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = book.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = CharcoalPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = {
                    if (book.totalPages != null && book.totalPages > 0) {
                        (book.lastPageIndex + 1).toFloat() / book.totalPages
                    } else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = SageGreenPrimary,
                trackColor = WarmSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (book.totalPages != null && book.totalPages > 0) {
                        "${book.lastPageIndex + 1} of ${book.totalPages}"
                    } else {
                        "Page ${book.lastPageIndex + 1}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalMuted
                )
                Text(
                    text = "${book.progressPercentage}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LibraryBookRow(
    book: Book,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WarmOutline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SageGreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = SageGreenPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = book.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = CharcoalPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val pageText = if (book.totalPages != null) {
                    stringResource(R.string.pages_progress, book.lastPageIndex + 1, book.totalPages)
                } else {
                    "Page ${book.lastPageIndex + 1}"
                }
                Text(
                    text = "PDF • $pageText",
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalSecondary
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.action_open),
                        tint = CharcoalMuted
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(WarmSurface)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_open)) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.action_remove),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SageGreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SageGreenPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = CharcoalPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalSecondary
            )
        }
    }
}
