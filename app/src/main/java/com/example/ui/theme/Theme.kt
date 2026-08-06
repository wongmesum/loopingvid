package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ProCreatorStudioColorScheme = darkColorScheme(
    primary = ProPrimary,
    onPrimary = Color(0xFF1A0F3C),
    primaryContainer = ProPrimaryContainer,
    onPrimaryContainer = ProOnPrimaryContainer,
    secondary = ProSecondary,
    onSecondary = Color(0xFF00252B),
    secondaryContainer = ProSecondaryContainer,
    onSecondaryContainer = ProOnSecondaryContainer,
    tertiary = ProSuccess,
    onTertiary = Color(0xFF00301E),
    tertiaryContainer = Color(0xFF0E4A34),
    onTertiaryContainer = Color(0xFFB8F5DC),
    background = ProBackground,
    onBackground = ProTextPrimary,
    surface = ProSurface,
    onSurface = ProTextPrimary,
    surfaceVariant = ProSurfaceElevated,
    onSurfaceVariant = ProTextSecondary,
    surfaceTint = ProPrimary,
    inverseSurface = ProTextPrimary,
    inverseOnSurface = ProBackground,
    outline = ProOutline,
    outlineVariant = ProOutlineVariant,
    error = ProLive,
    onError = Color(0xFF3D0009),
    errorContainer = Color(0xFF6B1420),
    onErrorContainer = Color(0xFFFFD9DE)
)

@Composable
fun LoopingVidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ProCreatorStudioColorScheme,
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
