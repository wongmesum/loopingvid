package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ElegantDarkColorScheme = darkColorScheme(
    primary = RoyalPurple,
    onPrimary = Color(0xFF260D5C),
    primaryContainer = RoyalPurpleContainer,
    onPrimaryContainer = OnRoyalPurpleContainer,
    secondary = ElegantGold,
    onSecondary = Color(0xFF2B2100),
    secondaryContainer = ElegantGoldContainer,
    onSecondaryContainer = OnElegantGoldContainer,
    tertiary = GoldAccent,
    onTertiary = Color(0xFF3E2D00),
    tertiaryContainer = Color(0xFF4F3B00),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = CharcoalBackground,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = RoyalPurple,
    inverseSurface = TextPrimaryDark,
    inverseOnSurface = CharcoalBackground,
    outline = OutlineDark,
    outlineVariant = CharcoalSurfaceHigh,
    error = StudioLiveRed,
    onError = Color(0xFF600004),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@Composable
fun LoopingVidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ElegantDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    LoopingVidTheme(darkTheme = darkTheme, content = content)
}


