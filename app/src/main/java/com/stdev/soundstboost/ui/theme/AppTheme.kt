package com.stdev.soundstboost.ui.theme

import androidx.compose.ui.graphics.Color

enum class AppTheme {
    NEON_DARK,
    OCEAN_BLUE,
    SUNSET_ORANGE,
    FOREST_GREEN,
    ROYAL_PURPLE
}

enum class ColorAccent {
    CYAN,
    PINK,
    ORANGE,
    GREEN,
    PURPLE
}

data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val accent1: Color,
    val accent2: Color,
    val isDark: Boolean
)

fun getThemeColors(theme: AppTheme, accent: ColorAccent): ThemeColors {
    return when (theme) {
        AppTheme.NEON_DARK -> ThemeColors(
            primary = Color(0xFF1A1A2E),
            secondary = Color(0xFF16213E),
            background = Color(0xFF0F0F1E),
            surface = Color(0xFF1F1F35),
            onSurface = Color(0xFFE4E4E7),
            accent1 = getAccentColor1(accent),
            accent2 = getAccentColor2(accent),
            isDark = true
        )
        AppTheme.OCEAN_BLUE -> ThemeColors(
            primary = Color(0xFF1E3A5F),
            secondary = Color(0xFF2E5A8F),
            background = Color(0xFF0D1B2A),
            surface = Color(0xFF1B2838),
            onSurface = Color(0xFFE8F1F5),
            accent1 = Color(0xFF00D9FF),
            accent2 = Color(0xFF0099FF),
            isDark = true
        )
        AppTheme.SUNSET_ORANGE -> ThemeColors(
            primary = Color(0xFFFF6B35),
            secondary = Color(0xFFFF8C42),
            background = Color(0xFFFFF8F0),
            surface = Color(0xFFFFF5E6),
            onSurface = Color(0xFF2D2D2D),
            accent1 = Color(0xFFFF6B35),
            accent2 = Color(0xFFFFA500),
            isDark = false
        )
        AppTheme.FOREST_GREEN -> ThemeColors(
            primary = Color(0xFF1B4332),
            secondary = Color(0xFF2D6A4F),
            background = Color(0xFF081C15),
            surface = Color(0xFF1B2821),
            onSurface = Color(0xFFD8F3DC),
            accent1 = Color(0xFF40916C),
            accent2 = Color(0xFF52B788),
            isDark = true
        )
        AppTheme.ROYAL_PURPLE -> ThemeColors(
            primary = Color(0xFF4A148C),
            secondary = Color(0xFF6A1B9A),
            background = Color(0xFF1A0033),
            surface = Color(0xFF2D1B47),
            onSurface = Color(0xFFF3E5F5),
            accent1 = Color(0xFFAB47BC),
            accent2 = Color(0xFFCE93D8),
            isDark = true
        )
    }
}

private fun getAccentColor1(accent: ColorAccent): Color {
    return when (accent) {
        ColorAccent.CYAN -> Color(0xFF00F5FF)
        ColorAccent.PINK -> Color(0xFFFF1493)
        ColorAccent.ORANGE -> Color(0xFFFF6B35)
        ColorAccent.GREEN -> Color(0xFF00FF88)
        ColorAccent.PURPLE -> Color(0xFFAB47BC)
    }
}

private fun getAccentColor2(accent: ColorAccent): Color {
    return when (accent) {
        ColorAccent.CYAN -> Color(0xFF00CED1)
        ColorAccent.PINK -> Color(0xFFFF69B4)
        ColorAccent.ORANGE -> Color(0xFFFF8C42)
        ColorAccent.GREEN -> Color(0xFF52B788)
        ColorAccent.PURPLE -> Color(0xFFCE93D8)
    }
}
