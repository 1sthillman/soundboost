package com.soundboost.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * SoundSTBoost Theme System
 * 
 * Design DNA: CS2-inspired tactical gaming aesthetic
 * Based on .kiro/steering/design-system.md + taste-skill Section 8
 * 
 * Dark Mode: MANDATORY (gaming aesthetic)
 * Light Mode: Optional high-contrast accessibility variant (not default)
 * 
 * Color Consistency Lock (taste-skill 4.2):
 * - ONE accent per screen (NeonOrange primary)
 * - NO section-level color flips (taste-skill 4.11)
 * - WCAG AA contrast minimum (4.5:1 body, 3:1 large text)
 */

private val DarkColorScheme = darkColorScheme(
    // Primary: CS2 Orange (NOT AI purple!)
    primary = NeonOrange,
    onPrimary = Color.White,
    primaryContainer = NeonOrangeSubtle,
    onPrimaryContainer = DeepBlack,
    
    // Secondary: Electric Blue
    secondary = CyberBlue,
    onSecondary = Color.White,
    secondaryContainer = CyberBlueSubtle,
    onSecondaryContainer = DeepBlack,
    
    // Tertiary: Neon Green (EQ/success)
    tertiary = NeonGreen,
    onTertiary = DeepBlack,
    tertiaryContainer = NeonGreenSubtle,
    onTertiaryContainer = DeepBlack,
    
    // Backgrounds (zinc-950 equivalent, not pure black)
    background = DeepBlack,
    onBackground = TextPrimary,
    
    // Surfaces (elevated cards, panels)
    surface = PanelDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondary,
    surfaceTint = NeonOrange,  // Elevation tint
    
    // Containers
    surfaceContainer = PanelDark,
    surfaceContainerHigh = PanelDarkElevated,
    surfaceContainerHighest = SurfaceDarkElevated,
    surfaceContainerLow = DeepBlack,
    surfaceContainerLowest = DeepBlack,
    
    // Borders & Outlines
    outline = TextTertiary,
    outlineVariant = TextDisabled,
    
    // Semantic states
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF3D1F1F),
    onErrorContainer = ErrorRed,
    
    // Inverse (for snackbars, tooltips)
    inverseSurface = TextPrimary,
    inverseOnSurface = DeepBlack,
    inversePrimary = NeonOrange,
    
    // Scrim (modal overlays)
    scrim = OverlayDark
)

// Light Mode: High-contrast accessibility variant (optional, not default)
// taste-skill Section 8.D: "Respect prefers-color-scheme unless brand insists"
// SoundSTBoost insists on dark (gaming aesthetic), light is accessibility fallback
private val LightColorScheme = lightColorScheme(
    primary = SunsetOrange,
    secondary = OceanPrimary,
    tertiary = ForestAccent,
    background = SunsetLight,
    surface = Color(0xFFFFFBF5),
    surfaceVariant = Color(0xFFFFF5E6),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF2D2D2D),
    onSurface = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFF5D5D5D),
    outline = Color(0xFF8D8D8D),
    outlineVariant = Color(0xFFBDBDBD)
)

@Composable
fun SoundSTBoostTheme(
    darkTheme: Boolean = true,  // Force dark mode (gaming aesthetic)
    // darkTheme: Boolean = isSystemInDarkTheme(),  // Optional: respect system
    content: @Composable () -> Unit
) {
    // taste-skill Section 4.11: "Page Theme Lock - ONE theme, no section flips"
    // Dark mode is mandatory for SoundSTBoost (gaming/competitive aesthetic)
    // Light mode only for accessibility opt-in (not automatic)
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SoundBoostTypography,
        content = content
    )
}
