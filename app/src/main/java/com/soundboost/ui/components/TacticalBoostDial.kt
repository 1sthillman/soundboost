package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.soundboost.ui.theme.MonoTypography
import com.soundboost.ui.theme.NeonOrange
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tactical Boost Dial - CS2-inspired HUD control
 * 
 * Design DNA:
 * - Segmented ring (10 segments, military HUD)
 * - Animated fill with spring physics
 * - Rotating outer ring when active
 * - Center power button with glow
 * - Haptic feedback on toggle
 * 
 * taste-skill compliance:
 * - Motion motivated (visual feedback for boost level)
 * - No centered hero violation (this IS the hero)
 * - Spring physics (Section 3.B)
 */
@Composable
fun TacticalBoostDial(
    boostLevel: Float,
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    // Spring-animated progress
    val animatedProgress by animateFloatAsState(
        targetValue = if (isActive) boostLevel / 100f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "boostProgress"
    )
    
    // Rotating outer ring (only when active)
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRingRotation"
    )
    
    // Scale feedback on press
    var isPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )
    
    // Glow intensity tied to boost level
    val glowAlpha = (animatedProgress * 0.6f).coerceIn(0f, 1f)
    
    Box(
        modifier = modifier
            .size(240.dp)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Tactical ring canvas
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = size.minDimension / 2f - 16.dp.toPx()
            val segmentCount = 10
            val segmentAngle = 360f / segmentCount
            val gapAngle = 4f
            
            // Rotate outer ring if active
            rotate(if (isActive) rotation else 0f, pivot = Offset(centerX, centerY)) {
                // Draw segments
                for (i in 0 until segmentCount) {
                    val startAngle = i * segmentAngle - 90f
                    val sweepAngle = segmentAngle - gapAngle
                    val segmentProgress = ((animatedProgress * segmentCount) - i).coerceIn(0f, 1f)
                    
                    // Background segment
                    drawArc(
                        color = NeonOrange.copy(alpha = 0.15f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(
                            width = 12.dp.toPx(),
                            cap = StrokeCap.Round
                        ),
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                    
                    // Active segment with glow
                    if (segmentProgress > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    NeonOrange.copy(alpha = segmentProgress * 0.8f),
                                    NeonOrange.copy(alpha = segmentProgress)
                                )
                            ),
                            startAngle = startAngle,
                            sweepAngle = sweepAngle * segmentProgress,
                            useCenter = false,
                            style = Stroke(
                                width = 12.dp.toPx(),
                                cap = StrokeCap.Round
                            ),
                            topLeft = Offset(centerX - radius, centerY - radius),
                            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                        )
                    }
                }
            }
        }
        
        // Center power button
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(buttonScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonOrange.copy(alpha = glowAlpha * 0.4f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .clickable(
                    onClick = onToggle,
                    indication = ripple(color = NeonOrange),
                    interactionSource = remember { MutableInteractionSource() }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Boost level display (monospace)
                Text(
                    text = "${boostLevel.toInt()}",
                    style = MonoTypography.statsLarge,
                    color = if (isActive) NeonOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isActive) "ACTIVE" else "STANDBY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActive) NeonOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
