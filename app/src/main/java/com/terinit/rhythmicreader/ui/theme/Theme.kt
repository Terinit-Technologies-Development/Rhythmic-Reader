package com.terinit.rhythmicreader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CalmColorScheme = lightColorScheme(
    primary = SageGreenPrimary,
    onPrimary = WarmSurface,
    primaryContainer = SageGreenContainer,
    onPrimaryContainer = SageGreenDeep,
    secondary = SageGreenSecondary,
    onSecondary = WarmSurface,
    secondaryContainer = SageGreenLight,
    onSecondaryContainer = SageGreenDeep,
    background = WarmBackground,
    onBackground = CharcoalPrimary,
    surface = WarmSurface,
    onSurface = CharcoalPrimary,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = CharcoalSecondary,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer
)

@Composable
fun RhythmicReaderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CalmColorScheme,
        typography = Typography,
        content = content
    )
}
