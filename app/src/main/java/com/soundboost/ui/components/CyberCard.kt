package com.soundboost.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soundboost.ui.theme.GlassWhite
import com.soundboost.ui.theme.PanelDarkElevated
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * CyberCard - Glassmorphism container
 * 
 * Design DNA:
 * - Real glassmorphism using haze library
 * - Frosted glass effect (taste-skill Section 2.B)
 * - Subtle gradient overlay
 * - Elevated appearance without heavy shadows
 * 
 * taste-skill compliance:
 * - No generic Material cards (custom design)
 * - Restrained elevation (not excessive)
 * - Color consistency (uses theme colors)
 * 
 * Usage:
 * ```
 * CyberCard {
 *     Text("Premium content")
 * }
 * ```
 */
@Composable
fun CyberCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PanelDarkElevated,
    borderColor: Color? = null,
    cornerRadius: Dp = 16.dp,
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val hazeState = remember { HazeState() }
    
    Surface(
        modifier = modifier
            .haze(state = hazeState)
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor.copy(alpha = 0.7f),
        tonalElevation = 0.dp,
        shadowElevation = elevation
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .hazeChild(
                    state = hazeState,
                    style = HazeMaterials.thick()
                )
                .then(
                    if (borderColor != null) {
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    borderColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
            content = content
        )
    }
}

/**
 * CyberCardSmall - Compact glassmorphic card
 * 
 * For smaller UI elements like settings items, presets, etc.
 */
@Composable
fun CyberCardSmall(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PanelDarkElevated,
    content: @Composable ColumnScope.() -> Unit
) {
    CyberCard(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 12.dp,
        elevation = 1.dp,
        content = content
    )
}

/**
 * CyberCardHighlight - Glassmorphic card with accent border
 * 
 * For active/selected states or important CTAs
 */
@Composable
fun CyberCardHighlight(
    modifier: Modifier = Modifier,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    CyberCard(
        modifier = modifier,
        backgroundColor = PanelDarkElevated,
        borderColor = accentColor,
        cornerRadius = 16.dp,
        elevation = 4.dp,
        content = content
    )
}
