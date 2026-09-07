package com.soundboost.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.soundboost.ui.theme.MonoTypography
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Premium Neon Slider - taste-skill compliant
 * 
 * Features:
 * - Glassmorphism track (haze library)
 * - Spring-animated thumb (Section 3.B)
 * - Scale feedback on press (0.98f)
 * - Haptic feedback
 * - Audio-reactive glow (when boost active)
 * - Monospace stats typography
 */
@Composable
fun PremiumNeonSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    isAudioActive: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }
    
    // Spring physics for thumb scale (taste-skill Section 3.B)
    val thumbScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "thumbScale"
    )
    
    // Audio-reactive pulse intensity
    val pulseAlpha by animateFloatAsState(
        targetValue = if (isAudioActive) 0.6f else 0.3f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "pulseAlpha"
    )
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            // Monospace value display (tactical HUD style)
            Text(
                text = valueLabel,
                style = MonoTypography.statsMedium,
                color = accentColor
            )
        }
        
        Spacer(Modifier.height(12.dp))
        
        // Glassmorphic track container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .haze(state = hazeState)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = pulseAlpha * 0.2f),
                            accentColor.copy(alpha = pulseAlpha * 0.4f)
                        )
                    )
                )
                .hazeChild(
                    state = hazeState,
                    style = HazeMaterials.thin()
                ),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .scale(thumbScale)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    },
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor.copy(alpha = 0.8f),
                    inactiveTrackColor = accentColor.copy(alpha = 0.15f),
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                )
            )
        }
    }
}
