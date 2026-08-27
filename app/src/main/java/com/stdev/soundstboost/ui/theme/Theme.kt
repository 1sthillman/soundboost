package com.stdev.soundstboost.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPink,
    background = Color(0xFF0F0F1E),
    surface = Color(0xFF1F1F35),
    onSurface = Color(0xFFE4E4E7)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF6B35),
    secondary = Color(0xFFFF8C42),
    background = Color(0xFFFFF8F0),
    surface = Color(0xFFFFF5E6),
    onSurface = Color(0xFF2D2D2D)
)

@Composable
fun SoundSTBoostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
