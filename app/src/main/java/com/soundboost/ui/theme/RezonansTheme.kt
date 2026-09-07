package com.soundboost.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Rezonans Theme System
 * Award-winning design themes from .agents/awwardtheme.md
 * 
 * Each theme has:
 * - bg0: Primary background (deepest)
 * - bg1: Surface background (elevated)
 * - ink: Primary text
 * - inkDim: Secondary text
 * - a1: Primary accent
 * - a2: Secondary accent (gradients)
 * - a3: Tertiary accent (depth)
 */

data class RezonansColors(
    val bg0: Color,
    val bg1: Color,
    val ink: Color,
    val inkDim: Color,
    val a1: Color,
    val a2: Color,
    val a3: Color,
    val accent: Color,
    val displayName: String,
    val trackTitle: String
)

enum class RezonansTheme {
    WATER,
    FOREST,
    SUN,
    ROYAL,
    AUTO_LIGHT,
    AUTO_DARK
}

fun getRezonansColors(theme: RezonansTheme): RezonansColors {
    return when (theme) {
        RezonansTheme.WATER -> RezonansColors(
            bg0 = Color(0xFF040f18),
            bg1 = Color(0xFF0a1f2e),
            ink = Color(0xFFeafcff),
            inkDim = Color(0xFF9fd6e0),
            a1 = Color(0xFF33d9e8),
            a2 = Color(0xFF7ff5ff),
            a3 = Color(0xFF0b6e7d),
            accent = Color(0xFF33d9e8),
            displayName = "Su",
            trackTitle = "Derin Akış"
        )
        
        RezonansTheme.FOREST -> RezonansColors(
            bg0 = Color(0xFF081208),
            bg1 = Color(0xFF0f2013),
            ink = Color(0xFFeafbe6),
            inkDim = Color(0xFFa9d6ac),
            a1 = Color(0xFF6fc24d),
            a2 = Color(0xFFc8e86a),
            a3 = Color(0xFF1f4a24),
            accent = Color(0xFF8fbf3f),
            displayName = "Orman",
            trackTitle = "Yeşil Yankı"
        )
        
        RezonansTheme.SUN -> RezonansColors(
            bg0 = Color(0xFF160c06),
            bg1 = Color(0xFF2a1408),
            ink = Color(0xFFfff3e6),
            inkDim = Color(0xFFf2c79a),
            a1 = Color(0xFFff7a29),
            a2 = Color(0xFFffd23f),
            a3 = Color(0xFF7a2e0e),
            accent = Color(0xFFff9636),
            displayName = "Güneş",
            trackTitle = "Kızıl Ufuk"
        )
        
        RezonansTheme.ROYAL -> RezonansColors(
            bg0 = Color(0xFF12061a),
            bg1 = Color(0xFF210b30),
            ink = Color(0xFFf6ecff),
            inkDim = Color(0xFFcbb2e6),
            a1 = Color(0xFFc9962f),
            a2 = Color(0xFF8e5cf7),
            a3 = Color(0xFF3d1a52),
            accent = Color(0xFFd4af37),
            displayName = "Kraliyet",
            trackTitle = "Altın Taht"
        )
        
        RezonansTheme.AUTO_LIGHT -> RezonansColors(
            bg0 = Color(0xFFf4f2ec),
            bg1 = Color(0xFFffffff),
            ink = Color(0xFF141210),
            inkDim = Color(0xFF6b665e),
            a1 = Color(0xFF1d2b4f),
            a2 = Color(0xFF405d99),
            a3 = Color(0xFFd9d5c9),
            accent = Color(0xFF1d2b4f),
            displayName = "Otomatik",
            trackTitle = "Sade Sinyal"
        )
        
        RezonansTheme.AUTO_DARK -> RezonansColors(
            bg0 = Color(0xFF0a0a0b),
            bg1 = Color(0xFF141416),
            ink = Color(0xFFf2f1ee),
            inkDim = Color(0xFF8a887f),
            a1 = Color(0xFFe7e3d8),
            a2 = Color(0xFF8a877c),
            a3 = Color(0xFF232326),
            accent = Color(0xFFe7e3d8),
            displayName = "Otomatik",
            trackTitle = "Sade Sinyal"
        )
    }
}
