package com.hfad.atomgit.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Indicator,
    onPrimaryContainer = Ink,
    secondary = InkMuted,
    onSecondary = Paper,
    secondaryContainer = PaperDim,
    onSecondaryContainer = Ink,
    tertiary = InkMuted,
    onTertiary = Paper,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDim,
    onSurfaceVariant = InkMuted,
    outline = Hairline,
    outlineVariant = Indicator,
    error = ErrorRed,
    onError = OnError,
)

private val DarkColorScheme = darkColorScheme(
    primary = InkDark,
    onPrimary = PaperDark,
    primaryContainer = IndicatorDark,
    onPrimaryContainer = InkDark,
    secondary = InkMutedDark,
    onSecondary = PaperDark,
    secondaryContainer = PaperDimDark,
    onSecondaryContainer = InkDark,
    tertiary = InkMutedDark,
    onTertiary = PaperDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = PaperDimDark,
    onSurfaceVariant = InkMutedDark,
    outline = HairlineDark,
    outlineVariant = IndicatorDark,
    error = ErrorRed,
    onError = OnError,
)

@Composable
fun AtomGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
