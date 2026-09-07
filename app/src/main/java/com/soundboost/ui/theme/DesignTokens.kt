package com.soundboost.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * SoundSTBoost Design Tokens
 * 
 * Centralized design constants following taste-skill Section 4
 * Based on .kiro/steering/design-system.md
 * 
 * Three Dials Configuration:
 * - DESIGN_VARIANCE: 9 (bold gaming layouts)
 * - MOTION_INTENSITY: 8 (cinematic CS2 feel)
 * - VISUAL_DENSITY: 5 (cockpit-style for audio metrics)
 */

/**
 * Spacing System
 * 
 * taste-skill Section 4.6: Vertical rhythm
 * Base unit: 8dp (Material 3 standard)
 */
object Spacing {
    val xxxs = 2.dp   // Micro gaps
    val xxs = 4.dp    // Tight spacing
    val xs = 8.dp     // Base unit
    val sm = 12.dp    // Comfortable spacing
    val md = 16.dp    // Standard gap
    val lg = 24.dp    // Section separator
    val xl = 32.dp    // Major sections
    val xxl = 48.dp   // Hero spacing
    val xxxl = 64.dp  // Dramatic spacing
}

/**
 * Corner Radius System
 * 
 * CS2-inspired tactical aesthetic
 * Subtle rounding (not excessive)
 */
object Corners {
    val xs = 4.dp     // Tight corners (bars, indicators)
    val sm = 8.dp     // Small components
    val md = 12.dp    // Buttons, inputs
    val lg = 16.dp    // Cards, panels
    val xl = 20.dp    // Large cards
    val xxl = 24.dp   // Hero elements
    val full = 9999.dp // Circular (dials, avatars)
}

/**
 * Elevation Values
 * 
 * Restrained elevation (not excessive)
 * Glassmorphism preferred over heavy shadows
 */
object ElevationValues {
    val none = 0.dp
    val xs = 1.dp     // Subtle lift
    val sm = 2.dp     // Small cards
    val md = 4.dp     // Standard cards
    val lg = 8.dp     // Modals, dialogs
    val xl = 16.dp    // Floating action buttons
}

/**
 * Animation Durations
 * 
 * taste-skill Section 5: Motion choreography
 * Based on MOTION_INTENSITY: 8 (cinematic)
 */
object Duration {
    const val instant = 0        // No animation
    const val fast = 150         // Quick feedback
    const val normal = 300       // Standard transitions
    const val slow = 500         // Dramatic reveals
    const val cinematic = 800    // Hero animations
    const val lazy = 1200        // Slow, deliberate motion
}

/**
 * Spring Physics Presets
 * 
 * taste-skill Section 3.B: Use spring() instead of tween()
 */
object Springs {
    // Bouncy spring (playful interactions)
    val bouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    // Standard spring (most UI interactions)
    val standard = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    // Stiff spring (precise, technical feel)
    val stiff = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    // Smooth spring (glassmorphic, fluid motion)
    val smooth = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 400f
    )
}

/**
 * Touch Feedback Scale
 * 
 * taste-skill Section 4.5: Scale feedback on interactive elements
 */
object TouchScale {
    const val pressed = 0.92f     // Dramatic press (buttons)
    const val subtle = 0.96f      // Subtle press (cards)
    const val minimal = 0.98f     // Minimal press (sliders)
}

/**
 * Icon Sizes
 * 
 * Material 3 standard sizes + tactical HUD variants
 */
object IconSize {
    val xs = 16.dp    // Micro icons
    val sm = 20.dp    // Small icons
    val md = 24.dp    // Standard (Material default)
    val lg = 32.dp    // Large icons
    val xl = 48.dp    // Hero icons
    val xxl = 64.dp   // Dramatic icons
}

/**
 * Audio-Specific Constants
 * 
 * Domain-specific design tokens
 */
object Audio {
    // Visualizer
    const val visualizerBars = 32            // CS2-style spectrum
    const val visualizerFps = 60            // Smooth animation target
    const val visualizerLerpFactor = 0.15f  // Smooth interpolation
    
    // Dial
    const val dialSegments = 10             // Military HUD segments
    const val dialGapAngle = 4f             // Gap between segments
    const val dialRotationSpeed = 8000      // ms per full rotation
    
    // Glow
    const val glowPulseSpeed = 1000         // ms per pulse cycle
    const val glowMaxAlpha = 0.6f           // Maximum glow intensity
    const val peakThreshold = 0.7f          // Level for peak glow
}

/**
 * Layout Constants
 * 
 * taste-skill Section 4: Layout discipline
 */
object Layout {
    // Max widths
    val maxContentWidth = 600.dp            // Phone landscape
    val maxProseWidth = 520.dp              // 65ch equivalent
    
    // Safe areas
    val minTouchTarget = 48.dp              // Accessibility minimum
    val recommendedTouchTarget = 56.dp      // Comfortable size
    
    // Padding
    val screenPaddingHorizontal = 16.dp     // Standard screen edges
    val screenPaddingVertical = 16.dp       // Top/bottom padding
    val cardPadding = 16.dp                 // Internal card padding
    val cardPaddingLarge = 24.dp            // Large card internal
}

/**
 * Asymmetric Bento Ratios
 * 
 * taste-skill Section 4.7: NO three equal cards
 */
object BentoRatios {
    // Two-column asymmetric
    const val primary = 0.6f      // Dominant column
    const val secondary = 0.4f    // Supporting column
    
    // Three-column asymmetric
    const val hero = 0.5f         // Hero element
    const val support1 = 0.3f     // First support
    const val support2 = 0.2f     // Second support
}

/**
 * Typography Line Clamp
 * 
 * taste-skill Section 4.1: Line length discipline
 */
object Typography {
    const val maxLineLength = 65  // 65 characters max
    const val headlineLines = 2   // Hero headline max lines
    const val subtextWords = 20   // Subtext max words
}
