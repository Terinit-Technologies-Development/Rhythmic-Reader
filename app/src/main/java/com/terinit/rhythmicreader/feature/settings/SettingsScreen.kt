package com.terinit.rhythmicreader.feature.settings

import androidx.compose.foundation.background
import com.terinit.rhythmicreader.app.Screen
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terinit.rhythmicreader.R
import com.terinit.rhythmicreader.ui.theme.CharcoalMuted
import com.terinit.rhythmicreader.ui.theme.CharcoalPrimary
import com.terinit.rhythmicreader.ui.theme.CharcoalSecondary
import com.terinit.rhythmicreader.ui.theme.SageGreenContainer
import com.terinit.rhythmicreader.ui.theme.SageGreenLight
import com.terinit.rhythmicreader.ui.theme.SageGreenPrimary
import com.terinit.rhythmicreader.ui.theme.WarmBackground
import com.terinit.rhythmicreader.ui.theme.WarmOutline
import com.terinit.rhythmicreader.ui.theme.WarmOutlineVariant
import com.terinit.rhythmicreader.ui.theme.WarmSurface

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onClearReadingHistory: () -> Unit,
    onClearLibrary: () -> Unit,
    onDismissNotification: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (Screen) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetHistoryDialog by remember { mutableStateOf(false) }
    var showClearLibraryDialog by remember { mutableStateOf(false) }
    var showIntegrationDialog by remember { mutableStateOf(false) }

    val isRoutineInstalled = remember(context) {
        val pm = context.packageManager
        try {
            pm.getPackageInfo("com.terinit.rhythmicroutine.qa", 0)
            true
        } catch (_: Exception) {
            try {
                pm.getPackageInfo("com.terinit.rhythmicroutine", 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    LaunchedEffect(uiState.userNotification) {
        val note = uiState.userNotification
        if (note != null) {
            snackbarHostState.showSnackbar(note)
            onDismissNotification()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SettingsTopBar(onBack = onBack)
        },
        bottomBar = {
            com.terinit.rhythmicreader.ui.components.RhythmicBottomBar(
                currentScreen = Screen.Settings,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Local storage section
            item {
                SettingsSectionHeader(title = stringResource(R.string.section_storage))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmOutline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface)
                ) {
                    Column {
                        SettingsItemRow(
                            icon = Icons.Default.Description,
                            title = stringResource(R.string.manage_documents),
                            subtitle = "${uiState.bookCount} books in library",
                            showChevron = false
                        )
                        HorizontalDivider(color = WarmOutlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItemRow(
                            icon = Icons.Default.Storage,
                            title = stringResource(R.string.device_storage),
                            subtitle = stringResource(
                                R.string.device_storage_desc,
                                uiState.formattedLibraryUsage,
                                uiState.formattedAvailableSpace
                            ),
                            showChevron = false
                        )
                    }
                }
            }

            // Reading history section
            item {
                SettingsSectionHeader(title = stringResource(R.string.section_history))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmOutline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface)
                ) {
                    Column {
                        SettingsItemRow(
                            icon = Icons.Default.DeleteOutline,
                            title = stringResource(R.string.clear_history),
                            subtitle = stringResource(R.string.clear_history_desc),
                            onClick = { showResetHistoryDialog = true }
                        )
                        HorizontalDivider(color = WarmOutlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItemRow(
                            icon = Icons.Default.DeleteOutline,
                            title = stringResource(R.string.clear_library),
                            subtitle = stringResource(R.string.clear_library_desc),
                            isDestructive = true,
                            onClick = { showClearLibraryDialog = true }
                        )
                    }
                }
            }

            // Recovery integration section
            item {
                SettingsSectionHeader(title = "Recovery integration")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmOutline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.terinit.rhythmicroutine.qa")
                                        ?: context.packageManager.getLaunchIntentForPackage("com.terinit.rhythmicroutine")
                                    if (launchIntent != null) {
                                        context.startActivity(launchIntent)
                                    } else {
                                        showIntegrationDialog = true
                                    }
                                }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SageGreenContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_leaf),
                                    contentDescription = null,
                                    tint = SageGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rhythmic Routine",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = CharcoalPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isRoutineInstalled) {
                                        "Connected via Protocol V1 • Tap to open Routine"
                                    } else {
                                        "Sync your reading with Rhythmic Routine"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CharcoalSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isRoutineInstalled) SageGreenLight.copy(alpha = 0.5f)
                                        else WarmOutlineVariant
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isRoutineInstalled) SageGreenPrimary
                                                else CharcoalMuted
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isRoutineInstalled) "Connected" else "Available",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isRoutineInstalled) SageGreenPrimary else CharcoalMuted
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = WarmOutlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                        // Integration Status Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SageGreenContainer.copy(alpha = 0.45f))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_leaf),
                                    contentDescription = null,
                                    tint = SageGreenPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Connected and syncing",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CharcoalPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Your reading activity helps build a calmer, more consistent routine.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CharcoalSecondary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = WarmOutlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItemRow(
                            icon = Icons.Default.Info,
                            title = "Manage integration",
                            subtitle = "View permissions, ContentProvider and IPC status",
                            onClick = { showIntegrationDialog = true }
                        )
                    }
                }
            }

            // About section
            item {
                SettingsSectionHeader(title = stringResource(R.string.section_about))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmOutline, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = WarmSurface)
                ) {
                    Column {
                        SettingsItemRow(
                            icon = Icons.Default.Info,
                            title = stringResource(R.string.app_version_title),
                            subtitle = stringResource(R.string.app_version_val),
                            showChevron = false
                        )
                        HorizontalDivider(color = WarmOutlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItemRow(
                            icon = Icons.Default.Lock,
                            title = stringResource(R.string.privacy_policy),
                            subtitle = stringResource(R.string.privacy_policy_desc),
                            showChevron = false
                        )
                        HorizontalDivider(color = WarmOutlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsItemRow(
                            icon = Icons.Default.VerifiedUser,
                            title = stringResource(R.string.terms_of_service),
                            subtitle = stringResource(R.string.terms_of_service_desc),
                            showChevron = false
                        )
                    }
                }
            }

            // Offline banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmOutline, RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SageGreenContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_leaf),
                            contentDescription = null,
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.offline_guarantee),
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showResetHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showResetHistoryDialog = false },
            title = {
                Text(stringResource(R.string.clear_history_confirm_title))
            },
            text = {
                Text(stringResource(R.string.clear_history_confirm_desc))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearReadingHistory()
                        showResetHistoryDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.reset_action),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetHistoryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = WarmSurface
        )
    }

    if (showClearLibraryDialog) {
        AlertDialog(
            onDismissRequest = { showClearLibraryDialog = false },
            title = {
                Text(stringResource(R.string.clear_library))
            },
            text = {
                Text(stringResource(R.string.remove_confirm_desc))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearLibrary()
                        showClearLibraryDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLibraryDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            containerColor = WarmSurface
        )
    }

    if (showIntegrationDialog) {
        AlertDialog(
            onDismissRequest = { showIntegrationDialog = false },
            title = {
                Text(
                    text = "Rhythmic Routine Integration",
                    style = MaterialTheme.typography.titleMedium,
                    color = CharcoalPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isRoutineInstalled) {
                            "Rhythmic Routine is detected and paired on this device.\n\nProtocol V1 ContentProvider IPC is active. When a Routine recovery cycle starts, Rhythmic Reader tracks active reading time and qualified page dwell, automatically fulfilling the recovery gate."
                        } else {
                            "Rhythmic Routine was not detected on this device.\n\nInstall the companion Rhythmic Routine application to automatically synchronize reading recovery sessions."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalSecondary,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntegrationDialog = false }) {
                    Text("OK", color = SageGreenPrimary)
                }
            },
            containerColor = WarmSurface
        )
    }
}

@Composable
private fun SettingsTopBar(
    onBack: () -> Unit
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

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            painter = painterResource(id = R.drawable.ic_leaf),
            contentDescription = null,
            tint = SageGreenPrimary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = CharcoalPrimary
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalMuted
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = CharcoalSecondary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isDestructive) MaterialTheme.colorScheme.errorContainer else SageGreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else SageGreenPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isDestructive) MaterialTheme.colorScheme.error else CharcoalPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalSecondary
            )
        }

        if (showChevron && onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = CharcoalMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
