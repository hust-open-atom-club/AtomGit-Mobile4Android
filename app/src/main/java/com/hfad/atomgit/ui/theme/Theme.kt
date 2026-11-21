package com.hfad.atomgit.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Paper,
    primaryContainer = AccentBlueContainer,
    onPrimaryContainer = Ink,
    secondary = InkMuted,
    onSecondary = Paper,
    secondaryContainer = PaperDim,
    onSecondaryContainer = Ink,
    tertiary = InkMuted,
    onTertiary = Paper,
    tertiaryContainer = PaperDim,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = PaperDim,
    onSurfaceVariant = InkMuted,
    surfaceTint = Color.Transparent,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = AccentBlueDark,
    outline = Hairline,
    outlineVariant = Indicator,
    error = ErrorRed,
    onError = OnError,
    scrim = Color(0x99000000),
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlueDark,
    onPrimary = Paper,
    primaryContainer = AccentBlueContainerDark,
    onPrimaryContainer = AccentBlueBright,
    secondary = InkMutedDark,
    onSecondary = PaperDark,
    secondaryContainer = PaperDimDark,
    onSecondaryContainer = InkDark,
    tertiary = InkMutedDark,
    onTertiary = PaperDark,
    tertiaryContainer = PaperDimDark,
    onTertiaryContainer = InkDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = PaperDark,
    onSurface = InkDark,
    surfaceVariant = PaperDimDark,
    onSurfaceVariant = InkMutedDark,
    surfaceTint = Color.Transparent,
    inverseSurface = PaperDim,
    inverseOnSurface = Ink,
    inversePrimary = AccentBlue,
    outline = HairlineDark,
    outlineVariant = IndicatorDark,
    error = ErrorRed,
    onError = OnError,
    scrim = Color(0x99000000),
)

@Composable
fun AtomGitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
