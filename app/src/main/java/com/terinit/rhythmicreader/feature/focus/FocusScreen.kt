package com.terinit.rhythmicreader.feature.focus

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.terinit.rhythmicreader.R
import com.terinit.rhythmicreader.app.Screen
import com.terinit.rhythmicreader.domain.model.Book
import com.terinit.rhythmicreader.domain.model.RecoveryStatus
import com.terinit.rhythmicreader.feature.recovery.RecoveryCard
import com.terinit.rhythmicreader.feature.recovery.RecoveryUiState
import com.terinit.rhythmicreader.ui.components.RhythmicBottomBar
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
fun FocusScreen(
    recoveryUiState: RecoveryUiState,
    recentBook: Book?,
    onOpenBook: (String) -> Unit,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        bottomBar = {
            RhythmicBottomBar(
                currentScreen = Screen.Focus,
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Reading Focus",
                            style = MaterialTheme.typography.headlineMedium,
                            color = CharcoalPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Calm progress, one page at a time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CharcoalSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SageGreenContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelfImprovement,
                            contentDescription = null,
                            tint = SageGreenPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Recovery Status Card
            item {
                if (recoveryUiState.status != null) {
                    RecoveryCard(uiState = recoveryUiState)
                } else {
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
                                .padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SageGreenLight.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_leaf),
                                        contentDescription = null,
                                        tint = SageGreenPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Mindful Reading",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CharcoalPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "No active cooldown recovery session",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CharcoalSecondary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "When Rhythmic Routine initiates a reading recovery cycle, your active reading time and qualified page progression will track here automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSecondary,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            // Now Reading Card (if a book exists)
            item {
                Text(
                    text = "Now Reading",
                    style = MaterialTheme.typography.titleMedium,
                    color = CharcoalPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (recentBook != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, WarmOutline, RoundedCornerShape(20.dp))
                            .clickable { onOpenBook(recentBook.id) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = SageGreenPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = recentBook.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = CharcoalPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val progressText = if (recentBook.totalPages != null && recentBook.totalPages > 0) {
                                    "Page ${recentBook.lastPageIndex + 1} of ${recentBook.totalPages}"
                                } else {
                                    "Page ${recentBook.lastPageIndex + 1}"
                                }
                                Text(
                                    text = progressText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CharcoalSecondary
                                )
                                if (recentBook.totalPages != null && recentBook.totalPages > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val ratio = (recentBook.lastPageIndex.toFloat() / recentBook.totalPages.toFloat()).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(CircleShape),
                                        color = SageGreenPrimary,
                                        trackColor = WarmOutline
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = CharcoalMuted
                            )
                        }
                    }
                } else {
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
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No books open yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = CharcoalPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Import or open a PDF from your Library to read in fullscreen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = CharcoalSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { onNavigate(Screen.Library) },
                                colors = ButtonDefaults.buttonColors(containerColor = SageGreenPrimary),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Go to Library", color = WarmBackground)
                            }
                        }
                    }
                }
            }

            // Rhythm Recovery Guide Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmOutline, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SageGreenContainer.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "How Reading Recovery Works",
                            style = MaterialTheme.typography.titleSmall,
                            color = CharcoalPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "1. Active reading time accumulates only while you are actively reading with screen on.\n" +
                                   "2. Pages qualify when you dwell for a credible reading threshold.\n" +
                                   "3. Reaching both targets automatically unlocks your Rhythmic Routine re-entry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalSecondary,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}
