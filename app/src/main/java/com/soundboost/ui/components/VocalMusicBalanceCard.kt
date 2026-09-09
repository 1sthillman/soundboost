package com.soundboost.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.soundboost.ui.theme.*

@Composable
fun VocalMusicBalanceCard(
    value: Float,  // 0.0 = Music only, 0.5 = Balanced, 1.0 = Vocal only
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color,
    onValueChange: (Float) -> Unit
) {
    val isActive = kotlin.math.abs(value - 0.5f) > 0.05f
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.98f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "vocalBalanceScale"
    )
    
    val isLightTheme = isSystemInDarkTheme().not()
    
    // Calculate labels
    val modeLabel = when {
        value < 0.35f -> "Music Enhanced"
        value < 0.48f -> "Music Focus"
        value in 0.48f..0.52f -> "Balanced"
        value < 0.65f -> "Vocal Focus"
        else -> "Vocal Enhanced"
    }
    
    val vocalPercent = (value * 100).toInt()
    val musicPercent = ((1f - value) * 100).toInt()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(Corners.xl),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) {
                Color.Black.copy(alpha = 0.08f)
            } else {
                surfaceColor.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) Elevation.level2 else Elevation.level1
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "VOCAL / MUSIC BALANCE",
                        fontSize = TextStyles.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        letterSpacing = TextStyles.spacingWide
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        modeLabel,
                        fontSize = TextStyles.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isActive) accentColor else textColor.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(Modifier.height(Spacing.xs))
            
            // Percentage Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Vocal percentage
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "Vocal",
                        fontSize = TextStyles.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        "$vocalPercent%",
                        fontSize = TextStyles.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = if (value > 0.52f) accentColor else textColor.copy(alpha = 0.5f)
                    )
                }
                
                // Music percentage
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Music",
                        fontSize = TextStyles.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        "$musicPercent%",
                        fontSize = TextStyles.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = if (value < 0.48f) accentColor else textColor.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Balance Slider
            ModernSlider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..1f,
                valueLabel = modeLabel,
                accentColor = accentColor,
                surfaceColor = if (isLightTheme) {
                    Color.Black.copy(alpha = 0.1f)
                } else {
                    surfaceColor.copy(alpha = 0.3f)
                }
            )
            
            // Description
            Text(
                "Adjust the balance between vocals and background music using frequency-based EQ. " +
                "Move left for music emphasis, right for vocal clarity.",
                fontSize = TextStyles.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = textColor.copy(alpha = 0.6f),
                lineHeight = TextStyles.bodyMedium * 1.4f
            )
        }
    }
}
