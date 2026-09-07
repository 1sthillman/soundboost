package com.soundboost.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * SoundSTBoost Typography System
 * 
 * Design DNA: Bold sans-serif display + monospace for stats
 * Based on .kiro/steering/design-system.md + taste-skill Section 4.1
 * 
 * Rules Applied:
 * - NO Inter font default (taste-skill: "Discouraged as default")
 * - Bold display fonts for gaming/competitive aesthetic
 * - Tight tracking for large text (taste-skill typography guidance)
 * - Monospace for audio stats/metrics (tactical HUD feel)
 * - Line length max 65ch for body text (taste-skill 4.1)
 * 
 * Font Stack (system fallback, upgrade to Geist via design-dna Phase 3):
 * - Display/Headlines: System Sans → Geist Display (900/800 weight)
 * - Body: System Sans → Geist (400/500 weight)
 * - Stats/Mono: Roboto Mono → Geist Mono (600 weight)
 */

val SoundBoostTypography = Typography(
    // Display styles - for hero elements (taste-skill: "Display / Headlines")
    // Default: text-4xl md:text-6xl tracking-tighter leading-none
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,     // 900 - gaming boldness
        fontSize = 64.sp,                  // ~4rem
        letterSpacing = (-1).sp,           // Tight tracking (taste-skill rule)
        lineHeight = 68.sp                 // Leading-none equivalent (1.06)
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Black,     // 900
        fontSize = 48.sp,                  // ~3rem
        letterSpacing = (-0.5).sp,
        lineHeight = 52.sp                 // 1.08
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 36.sp,                  // ~2.25rem
        letterSpacing = (-0.25).sp,
        lineHeight = 40.sp                 // 1.11
    ),
    
    // Headline styles - for section headers
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold, // 800
        fontSize = 32.sp,                  // ~2rem
        letterSpacing = 0.sp,
        lineHeight = 40.sp                 // 1.25 (Material 3 standard)
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,      // 700
        fontSize = 28.sp,                  // ~1.75rem
        letterSpacing = 0.sp,
        lineHeight = 36.sp                 // 1.29
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,      // 700
        fontSize = 24.sp,                  // ~1.5rem
        letterSpacing = 0.sp,
        lineHeight = 32.sp                 // 1.33
    ),
    
    // Title styles - for cards and list items
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,      // 700
        fontSize = 22.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp                 // 1.27
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,  // 600
        fontSize = 18.sp,
        letterSpacing = 0.15.sp,           // Slightly wider for readability
        lineHeight = 24.sp                 // 1.33
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,  // 600
        fontSize = 16.sp,
        letterSpacing = 0.1.sp,
        lineHeight = 20.sp                 // 1.25
    ),
    
    // Body styles - for content (taste-skill: max-w-[65ch])
    // Default: text-base text-gray-600 leading-relaxed
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,    // 400
        fontSize = 16.sp,                  // Base size
        letterSpacing = 0.5.sp,            // Wider for light-on-dark readability
        lineHeight = 26.sp                 // 1.625 (relaxed, taste-skill standard)
        // Note: max-width 65ch enforced in component layer
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,    // 400
        fontSize = 14.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 22.sp                 // 1.57
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,    // 400
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        lineHeight = 18.sp                 // 1.5
    ),
    
    // Label styles - for buttons and indicators
    // Uppercase + wide tracking for tactical HUD feel
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,      // 700
        fontSize = 14.sp,
        letterSpacing = 1.2.sp,            // Wide tracking for ALL CAPS
        lineHeight = 20.sp                 // 1.43
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,  // 600
        fontSize = 12.sp,
        letterSpacing = 0.8.sp,
        lineHeight = 16.sp                 // 1.33
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,    // 500
        fontSize = 10.sp,
        letterSpacing = 0.6.sp,
        lineHeight = 14.sp                 // 1.4
    )
)

/**
 * Monospace Typography (Audio Stats / Technical Displays)
 * 
 * Use for:
 * - dB levels (e.g., "160dB", "+20dB")
 * - Frequency values (e.g., "60Hz", "16kHz")
 * - Percentage displays (e.g., "75%", "100%")
 * - Technical metrics (sample rate, latency)
 * 
 * System font (Roboto Mono) → Upgrade to Geist Mono via design-dna
 */
object MonoTypography {
    val statsLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,  // 600
        fontSize = 32.sp,                  // Dial center number
        letterSpacing = 0.sp,              // Monospace needs no adjustment
        lineHeight = 40.sp
    )
    
    val statsMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,  // 600
        fontSize = 20.sp,                  // Slider value display
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    )
    
    val statsSmall = TextStyle(
        fontWeight = FontWeight.Medium,    // 500
        fontSize = 14.sp,                  // Unit labels (dB, Hz)
        letterSpacing = 0.5.sp,            // Slightly wider
        lineHeight = 20.sp
    )
    
    val statsTiny = TextStyle(
        fontWeight = FontWeight.Normal,    // 400
        fontSize = 10.sp,                  // Micro-labels
        letterSpacing = 0.5.sp,
        lineHeight = 14.sp
    )
}

