package com.terinit.rhythmicreader.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.terinit.rhythmicreader.app.Screen
import com.terinit.rhythmicreader.domain.model.DailyReadingEvidenceSnapshot
import com.terinit.rhythmicreader.domain.model.RoutineReadingTargetPreview
import com.terinit.rhythmicreader.feature.recovery.RecoveryCard
import com.terinit.rhythmicreader.feature.recovery.RecoveryUiState
import com.terinit.rhythmicreader.ui.components.RhythmicBottomBar
import com.terinit.rhythmicreader.ui.theme.CharcoalPrimary
import com.terinit.rhythmicreader.ui.theme.CharcoalSecondary
import com.terinit.rhythmicreader.ui.theme.SageGreenContainer
import com.terinit.rhythmicreader.ui.theme.SageGreenPrimary
import com.terinit.rhythmicreader.ui.theme.WarmBackground
import com.terinit.rhythmicreader.ui.theme.WarmOutline
import com.terinit.rhythmicreader.ui.theme.WarmSurface

@Composable
fun TodayReadingScreen(
    uiState: TodayReadingUiState,
    recoveryUiState: RecoveryUiState,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = WarmBackground,
        bottomBar = {
            RhythmicBottomBar(currentScreen = Screen.Today, onNavigate = onNavigate)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Reading",
                        style = MaterialTheme.typography.headlineMedium,
                        color = CharcoalPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = uiState.dateKey.takeIf { it.isNotBlank() }
                            ?.let { "Local day · $it" }
                            ?: "Your verified reading for today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = null,
                    tint = SageGreenPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SageGreenContainer)
                        .padding(11.dp)
                )
            }

            if (recoveryUiState.status != null) {
                RecoveryCard(uiState = recoveryUiState)
            }

            if (!recoveryUiState.isSessionActive) {
                NextRoutineTargetCard(
                    preview = uiState.nextRoutineTarget,
                    isLoaded = uiState.routineTargetLoaded,
                    todayDateKey = uiState.dateKey,
                    evidence = uiState.evidence,
                )
            }

            EvidenceMetricCard(
                icon = Icons.Default.Timer,
                label = "Verified active reading",
                value = formatActiveDuration(uiState.evidence.verifiedActiveSeconds),
                detail = "Time while Reader is visible and the screen is interactive"
            )

            EvidenceMetricCard(
                icon = Icons.Default.AutoStories,
                label = "Dwell-qualified pages",
                value = uiState.evidence.qualifiedPages.toString(),
                detail = if (uiState.evidence.qualifiedPages == 1) {
                    "1 unique page qualified today"
                } else {
                    "Unique pages qualified today"
                }
            )

            Text(
                text = "These totals come from Reader's existing reading-time and page-dwell checks.",
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun NextRoutineTargetCard(
    preview: RoutineReadingTargetPreview?,
    isLoaded: Boolean,
    todayDateKey: String,
    evidence: DailyReadingEvidenceSnapshot,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WarmOutline, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reading quota",
                        style = MaterialTheme.typography.titleMedium,
                        color = CharcoalPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Daily verified reading vs Routine’s next cooldown",
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSecondary,
                    )
                }
                Icon(
                    imageVector = Icons.Default.AutoStories,
                    contentDescription = null,
                    tint = SageGreenPrimary,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SageGreenContainer)
                        .padding(10.dp),
                )
            }

            when {
                !isLoaded -> Text(
                    text = "Checking Rhythmic Routine for the next target…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalSecondary,
                )
                preview == null -> Text(
                    text = "Routine hasn’t shared a target yet. Open Rhythmic Routine once to publish its current quota.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalSecondary,
                )
                preview.dateKey != todayDateKey -> Text(
                    text = "Refreshing Routine’s target for today…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalSecondary,
                )
                preview.requiredActiveSeconds == 0L && preview.requiredQualifiedPages == 0 -> {
                    Text(
                        text = "Cooldown #${preview.nextCooldownOrdinal} has no reading quota: 0 min and 0 pages.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CharcoalPrimary,
                        fontWeight = FontWeight.Medium,
                    )
                }
                else -> {
                    Text(
                        text = "Cooldown #${preview.nextCooldownOrdinal} · preview based on Routine’s current daily policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = CharcoalSecondary,
                    )
                    if (preview.requiredActiveSeconds > 0L) {
                        TargetProgressRow(
                            label = "Verified active reading",
                            value = "${formatActiveDuration(evidence.verifiedActiveSeconds)} / ${formatActiveDuration(preview.requiredActiveSeconds)}",
                            progress = (evidence.verifiedActiveSeconds.toFloat() /
                                preview.requiredActiveSeconds.toFloat()).coerceIn(0f, 1f),
                        )
                    }
                    if (preview.requiredQualifiedPages > 0) {
                        TargetProgressRow(
                            label = "Dwell-qualified pages",
                            value = "${evidence.qualifiedPages} / ${preview.requiredQualifiedPages}",
                            progress = (evidence.qualifiedPages.toFloat() /
                                preview.requiredQualifiedPages.toFloat()).coerceIn(0f, 1f),
                        )
                    }
                }
            }

            Text(
                text = "Preview only. Routine makes a quota binding when it starts a recovery session.",
                style = MaterialTheme.typography.bodySmall,
                color = CharcoalSecondary,
            )
        }
    }
}

@Composable
private fun TargetProgressRow(
    label: String,
    value: String,
    progress: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = CharcoalSecondary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = CharcoalPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = SageGreenPrimary,
            trackColor = SageGreenContainer,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun EvidenceMetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    detail: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WarmOutline, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = WarmSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SageGreenPrimary,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SageGreenContainer)
                    .padding(11.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = CharcoalSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = CharcoalPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = CharcoalSecondary
                )
            }
        }
    }
}

private fun formatActiveDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L && minutes > 0L -> "$hours hr $minutes min"
        hours > 0L -> "$hours hr"
        minutes > 0L && seconds > 0L -> "$minutes min $seconds sec"
        minutes > 0L -> "$minutes min"
        else -> "$seconds sec"
    }
}
