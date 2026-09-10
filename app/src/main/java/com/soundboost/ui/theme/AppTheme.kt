package com.soundboost.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Award-Winning Themes from awwardstheme.html
 * 10 art-directed worlds, each with distinct material & mood
 */
enum class AppTheme {
    MEHTAP,          // 🌙 Mehtap - Moonlight on still water, patient fisherman
    SUMI,            // 🎨 Sumi-e - Japanese ink painting
    AURORA,          // 🌌 Kutup Şafağı - Northern lights  
    FENER,           // 🏮 Son Işık - Lighthouse at world's end guiding ships through fog
    ORMAN,           // 🏕️ Ay Kampı - Fireside by the lake with traveler, dog, and moon
    EYES,            // 👁️ Derin Göz - Ocean eye that weeps tears and reddens with sound
    MYCEL,           // 🍄 Miselyum - Bioluminescent network
    REEF,            // 🐠 Derin Işıltı - Deep sea bioluminescence
    MONSOON,         // ⛈️ Muson - Storm with lightning
    MUREKKEP,        // 🖋️ Mürekkep - Sumi ink bloomed in water, kintsugi gold
    COL,             // 🏜️ Çöl - Heat drifting over dunes of sand
    DIVIT            // 🖋️ Divit - Inkwell drop blooming in water with golden capillaries
}

enum class ColorAccent {
    MEHTAP_GOLD,     // #f2b155
    SUMI_RED,        // #c1442c
    AURORA_CYAN,     // #4fd8b0
    FENER_GOLD,      // #f2b155 (lighthouse beam gold)
    ORMAN_ORANGE,    // #ffb35c (campfire orange)
    EYES_CYAN,       // #3fe0d0
    MYCEL_GREEN,     // #6dffb0
    REEF_CYAN,       // #12e0bd
    MONSOON_BLUE,    // #9cc2ff
    MUREKKEP_GOLD,   // #c9a227
    COL_ORANGE,      // #ffb454
    DIVIT_GOLD       // #c9a35c
}

data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val accent1: Color,
    val accent2: Color,
    val isDark: Boolean,
    // Additional properties for UI components
    val surfaceElevated: Color = surface.copy(alpha = 0.95f),
    val onSurfaceVariant: Color = onSurface.copy(alpha = 0.7f),
    val outline: Color = onSurface.copy(alpha = 0.3f)
)

/**
 * Get theme colors matching awwardstheme.html CSS variables
 * Each theme has exact colors from the HTML implementation
 */
fun getThemeColors(theme: AppTheme, accent: ColorAccent): ThemeColors {
    return when (theme) {
        AppTheme.MEHTAP -> ThemeColors(
            primary = Color(0xFFf2b155),        // --a4 golden moonlight
            secondary = Color(0xFF4a6fa5),      // --a2 water blue
            background = Color(0xFF040910),     // --bg0
            surface = Color(0xFF0c1e30),        // --bg1
            onSurface = Color(0xFFeef2f5),      // --ink
            accent1 = Color(0xFFf2b155),
            accent2 = Color(0xFF4a6fa5),
            isDark = true
        )
        AppTheme.SUMI -> ThemeColors(
            primary = Color(0xFFc1442c),        // --a1
            secondary = Color(0xFFe9e2d0),      // --a2
            background = Color(0xFF080705),     // --bg0
            surface = Color(0xFF141210),        // --bg1
            onSurface = Color(0xFFf1ece0),      // --ink
            accent1 = Color(0xFFc1442c),
            accent2 = Color(0xFFe9e2d0),
            isDark = true
        )
        AppTheme.AURORA -> ThemeColors(
            primary = Color(0xFF4fd8b0),        // --accent
            secondary = Color(0xFF8a6bff),      // --a2
            background = Color(0xFF02040a),     // --bg0
            surface = Color(0xFF061024),        // --bg1
            onSurface = Color(0xFFeef5ff),      // --ink
            accent1 = Color(0xFF33e6a8),        // --a1
            accent2 = Color(0xFF8a6bff),
            isDark = true
        )
        AppTheme.FENER -> ThemeColors(
            primary = Color(0xFFf2b155),        // --accent (lighthouse beam)
            secondary = Color(0xFF4a6fa5),      // --a2 (water blue)
            background = Color(0xFF040910),     // --bg0
            surface = Color(0xFF0c1e30),        // --bg1
            onSurface = Color(0xFFeef2f5),      // --ink
            accent1 = Color(0xFFd9b46a),        // --a1
            accent2 = Color(0xFF4a6fa5),
            isDark = true
        )
        AppTheme.ORMAN -> ThemeColors(
            primary = Color(0xFFffb35c),        // --accent (campfire)
            secondary = Color(0xFF6d87a0),      // --ink-dim
            background = Color(0xFF020a16),     // --bg0
            surface = Color(0xFF0a2036),        // --bg1
            onSurface = Color(0xFFe8f1fa),      // --ink
            accent1 = Color(0xFFffb35c),
            accent2 = Color(0xFF6d87a0),
            isDark = true
        )
        AppTheme.EYES -> ThemeColors(
            primary = Color(0xFF3fe0d0),        // --accent (turquoise eye glow)
            secondary = Color(0xFF2fd9c9),      // --a1 (bright cyan)
            background = Color(0xFF020a10),     // --bg0 (deep ocean)
            surface = Color(0xFF04141e),        // --bg1
            onSurface = Color(0xFFdff6ff),      // --ink (light cyan)
            accent1 = Color(0xFF2fd9c9),
            accent2 = Color(0xFF0e5f7a),        // --a2 (deep teal)
            isDark = true
        )
        AppTheme.MYCEL -> ThemeColors(
            primary = Color(0xFF6dffb0),        // --a1
            secondary = Color(0xFFffd58a),      // --a2
            background = Color(0xFF020705),     // --bg0
            surface = Color(0xFF06140c),        // --bg1
            onSurface = Color(0xFFeafff0),      // --ink
            accent1 = Color(0xFF6dffb0),
            accent2 = Color(0xFFffd58a),
            isDark = true
        )
        AppTheme.REEF -> ThemeColors(
            primary = Color(0xFF12e0bd),        // --a1
            secondary = Color(0xFFff6bcf),      // --a2
            background = Color(0xFF010a10),     // --bg0
            surface = Color(0xFF03151f),        // --bg1
            onSurface = Color(0xFFd7fbff),      // --ink
            accent1 = Color(0xFF12e0bd),
            accent2 = Color(0xFFff6bcf),
            isDark = true
        )
        AppTheme.MONSOON -> ThemeColors(
            primary = Color(0xFF9cc2ff),        // --a1
            secondary = Color(0xFFb48bff),      // --a2
            background = Color(0xFF05070c),     // --bg0
            surface = Color(0xFF0e1420),        // --bg1
            onSurface = Color(0xFFe9eef7),      // --ink
            accent1 = Color(0xFF9cc2ff),
            accent2 = Color(0xFFb48bff),
            isDark = true
        )
        AppTheme.MUREKKEP -> ThemeColors(
            primary = Color(0xFFc9a227),        // --a1 (kintsugi gold)
            secondary = Color(0xFF8b1e3f),      // --a2 (deep red)
            background = Color(0xFF0a0806),     // --bg0
            surface = Color(0xFF17120d),        // --bg1
            onSurface = Color(0xFFf5ecd9),      // --ink
            accent1 = Color(0xFFc9a227),
            accent2 = Color(0xFF8b1e3f),
            isDark = true
        )
        AppTheme.COL -> ThemeColors(
            primary = Color(0xFFffb454),        // --a1 (sand/sun)
            secondary = Color(0xFFff7a3d),      // --a2 (heat)
            background = Color(0xFF0a0704),     // --bg0
            surface = Color(0xFF1a120a),        // --bg1
            onSurface = Color(0xFFffe9cf),      // --ink
            accent1 = Color(0xFFffb454),
            accent2 = Color(0xFFff7a3d),
            isDark = true
        )
        AppTheme.DIVIT -> ThemeColors(
            primary = Color(0xFFc9a35c),        // --a1 (golden ink)
            secondary = Color(0xFF8f98c9),      // --a2 (purple-blue)
            background = Color(0xFF0a0d16),     // --bg0
            surface = Color(0xFF141a2c),        // --bg1
            onSurface = Color(0xFFf2ede0),      // --ink
            accent1 = Color(0xFFc9a35c),
            accent2 = Color(0xFF8f98c9),
            isDark = true
        )
    }
}

private fun getAccentColor1(accent: ColorAccent): Color {
    return when (accent) {
        ColorAccent.MEHTAP_GOLD -> Color(0xFFf2b155)
        ColorAccent.SUMI_RED -> Color(0xFFc1442c)
        ColorAccent.AURORA_CYAN -> Color(0xFF4fd8b0)
        ColorAccent.FENER_GOLD -> Color(0xFFf2b155)
        ColorAccent.ORMAN_ORANGE -> Color(0xFFffb35c)
        ColorAccent.EYES_CYAN -> Color(0xFF3fe0d0)
        ColorAccent.MYCEL_GREEN -> Color(0xFF6dffb0)
        ColorAccent.REEF_CYAN -> Color(0xFF12e0bd)
        ColorAccent.MONSOON_BLUE -> Color(0xFF9cc2ff)
        ColorAccent.MUREKKEP_GOLD -> Color(0xFFc9a227)
        ColorAccent.COL_ORANGE -> Color(0xFFffb454)
        ColorAccent.DIVIT_GOLD -> Color(0xFFc9a35c)
    }
}

private fun getAccentColor2(accent: ColorAccent): Color {
    return when (accent) {
        ColorAccent.MEHTAP_GOLD -> Color(0xFF4a6fa5)
        ColorAccent.SUMI_RED -> Color(0xFFe9e2d0)
        ColorAccent.AURORA_CYAN -> Color(0xFF8a6bff)
        ColorAccent.FENER_GOLD -> Color(0xFF4a6fa5)
        ColorAccent.ORMAN_ORANGE -> Color(0xFF6d87a0)
        ColorAccent.EYES_CYAN -> Color(0xFF0e5f7a)
        ColorAccent.MYCEL_GREEN -> Color(0xFFffd58a)
        ColorAccent.REEF_CYAN -> Color(0xFFff6bcf)
        ColorAccent.MONSOON_BLUE -> Color(0xFFb48bff)
        ColorAccent.MUREKKEP_GOLD -> Color(0xFF8b1e3f)
        ColorAccent.COL_ORANGE -> Color(0xFFff7a3d)
        ColorAccent.DIVIT_GOLD -> Color(0xFF8f98c9)
    }
}
