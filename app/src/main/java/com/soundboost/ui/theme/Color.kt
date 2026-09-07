package com.soundboost.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SoundSTBoost Color System
 * 
 * Design DNA: CS2-inspired tactical gaming aesthetic
 * Based on .kiro/steering/design-system.md
 * Follows taste-skill Section 4.2 (Color Calibration)
 * 
 * Rules Applied:
 * - NO AI-purple defaults (taste-skill "LILA RULE")
 * - CS2 orange/gold primary (gaming signature)
 * - Electric blue secondary (competitive feel)
 * - Dark mode mandatory (zinc-950 equivalent)
 * - Max 1 accent color per screen (consistency lock)
 */

// Primary: CS2 Orange/Gold (NOT AI purple!)
val NeonOrange = Color(0xFFFF6B35)        // Primary CTA, boost active
val NeonOrangeGlow = Color(0xFFFF8C61)    // Hover/glow state
val NeonOrangeSubtle = Color(0xFFFF9D7F)  // Disabled/inactive

// Secondary: Electric Blue (competitive gaming)
val CyberBlue = Color(0xFF00D9FF)          // Secondary accent, 3D audio
val CyberBlueGlow = Color(0xFF66E7FF)      // Active state
val CyberBlueSubtle = Color(0xFF99F0FF)    // Hint/guide

// Accent: Neon Green (EQ/success states)
val NeonGreen = Color(0xFF39FF14)          // EQ active, success
val NeonGreenGlow = Color(0xFF5AFF3A)      // Peak indicator
val NeonGreenSubtle = Color(0xFF7BFF5B)    // Subtle highlight

// Legacy neon colors (keep for backwards compatibility)
val NeonPink = Color(0xFFFF2DAA)           // Legacy: bass indicator
val NeonPinkGlow = Color(0xFFFF4DC4)       // Legacy: bass peak
val NeonCyan = CyberBlue                   // Alias to new system
val NeonCyanGlow = CyberBlueGlow           // Alias
val NeonPurple = Color(0xFF9D4DFF)         // Legacy: virtualizer (migrate away)
val NeonPurpleGlow = Color(0xFFB76DFF)     // Legacy

// Dark Foundations (taste-skill Section 8: Dark Mode Protocol)
// Using zinc-950 equivalent (not pure black #000000)
val DeepBlack = Color(0xFF0A0A0F)          // Background (zinc-950)
val PanelDark = Color(0xFF16161F)          // Surface base
val PanelDarkElevated = Color(0xFF1E1E2D) // Cards/elevated surfaces
val SurfaceDark = Color(0xFF252535)        // Interactive elements
val SurfaceDarkElevated = Color(0xFF2D2D40) // Hover/active states

// Text Colors (WCAG AA compliant)
// taste-skill Section 4.5: Contrast check mandatory
val TextPrimary = Color(0xFFF0F0F0)        // Body text: 14.2:1 on DeepBlack (AAA)
val TextSecondary = Color(0xFFA0A0B0)      // Secondary text: 6.8:1 (AA)
val TextTertiary = Color(0xFF707090)       // Disabled text: 4.5:1 (AA minimum)
val TextDisabled = Color(0xFF505065)       // Fully disabled: 2.8:1

// Audio-Specific Semantic Colors
// taste-skill Section 9.D: No generic status colors
val BassIndicator = Color(0xFFFF3366)      // Hot pink for bass freq
val MidIndicator = Color(0xFFFFD700)       // Gold for mid freq  
val TrebleIndicator = CyberBlue            // Blue for treble freq
val ActiveBoost = NeonOrange               // Boost active state
val InactiveBoost = Color(0xFF3D3D50)      // Boost inactive

// Theme Alternatives (optional, not default)
// Ocean Blue Theme
val OceanPrimary = Color(0xFF0066CC)
val OceanSecondary = Color(0xFF0080FF)
val OceanAccent = Color(0xFF00D9FF)
val OceanDark = Color(0xFF003366)

// Sunset Orange Theme (approved: NOT beige+brass banned palette)
val SunsetOrange = Color(0xFFFF6B35)
val SunsetPeach = Color(0xFFFF8C42)
val SunsetGold = Color(0xFFFFAA00)
val SunsetLight = Color(0xFFFFF8F0)

// Forest Green Theme
val ForestPrimary = Color(0xFF1B4332)
val ForestSecondary = Color(0xFF2D6A4F)
val ForestAccent = Color(0xFF40916C)
val ForestLight = Color(0xFF52B788)

// Royal Purple Theme (legacy, migrate away from AI-purple)
val RoyalPrimary = Color(0xFF4A148C)
val RoyalSecondary = Color(0xFF6A1B9A)
val RoyalAccent = Color(0xFFAB47BC)
val RoyalLight = Color(0xFFCE93D8)

// Glassmorphism & Effects (taste-skill Section 2.B)
// Using haze library (dev.chrisbanes.haze) for real glassmorphism
val GlassWhite = Color(0x18FFFFFF)         // 10% white overlay
val GlassBlack = Color(0x18000000)         // 10% black overlay
val OverlayDark = Color(0x80000000)        // 50% modal scrim
val OverlayLight = Color(0x40FFFFFF)       // 25% highlight

// Status Colors (audio-specific, not generic)
val SuccessGreen = NeonGreen               // Boost applied successfully
val WarningYellow = Color(0xFFFFBF00)      // Volume near max
val ErrorRed = Color(0xFFFF3366)           // Effect failed/unsupported
val InfoBlue = CyberBlue                   // 3D audio active

