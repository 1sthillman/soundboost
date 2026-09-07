package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.soundboost.R
import com.soundboost.ui.theme.ThemeColors
import kotlin.math.*

/**
 * AWARD-WINNING ARTISTIC RATING DIALOG
 * Mükemmel, sanatsal, awwards seviyesinde tasarım!
 */
@Composable
fun RatingDialog(
    themeColors: ThemeColors,
    onDismiss: () -> Unit,
    onRate: (Int) -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "rating")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "shimmer"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Artistic glow background
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp)
            ) {
                // Radial gradient glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.accent1.copy(alpha = 0.15f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.8f
                    )
                )
            }
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp),
                shape = RoundedCornerShape(32.dp),
                color = themeColors.surface.copy(alpha = 0.95f),
                shadowElevation = 24.dp
            ) {
                Box {
                    // Animated grain texture
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    themeColors.accent2.copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.3f),
                                radius = size.width * 0.6f
                            )
                        )
                        
                        // Pulsating orbital rings
                        for (i in 0..2) {
                            val radius = 60f + i * 25f + sin(shimmer * 0.02f + i) * 8f
                            drawCircle(
                                color = themeColors.accent1.copy(alpha = 0.08f - i * 0.02f),
                                radius = radius,
                                center = Offset(size.width * 0.5f, 120f),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(16.dp))
                        
                        // Animated star icon
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pulsating glow
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val glowSize = 40.dp.toPx() + sin(shimmer * 0.05f) * 6.dp.toPx()
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            themeColors.accent1.copy(alpha = 0.4f),
                                            Color.Transparent
                                        ),
                                        radius = glowSize
                                    ),
                                    radius = glowSize,
                                    center = center
                                )
                            }
                            
                            Text(
                                text = "⭐",
                                fontSize = 56.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.offset(y = (-2).dp)
                            )
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Title - more compact
                        Text(
                            text = stringResource(R.string.rate_app_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 28.sp,
                            maxLines = 2
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Description - more compact
                        Text(
                            text = stringResource(R.string.rate_app_description),
                            fontSize = 14.sp,
                            color = themeColors.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            maxLines = 2
                        )
                        
                        Spacer(Modifier.height(32.dp))
                        
                        // Artistic star rating - Fixed width to prevent overflow
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.height(64.dp)
                            ) {
                                (1..5).forEach { star ->
                                    ArtisticStarButton(
                                        isSelected = star <= selectedRating,
                                        onClick = { selectedRating = star },
                                        themeColors = themeColors,
                                        shimmer = shimmer,
                                        index = star
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(36.dp))
                        
                        // Modern action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Later button - minimalist
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = themeColors.onSurface.copy(alpha = 0.6f)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, 
                                    themeColors.onSurface.copy(alpha = 0.15f)
                                )
                            ) {
                                Text(
                                    stringResource(R.string.later),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            
                            // Rate button - premium gradient
                            Button(
                                onClick = { 
                                    if (selectedRating > 0) {
                                        onRate(selectedRating)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                enabled = selectedRating > 0,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeColors.accent1,
                                    contentColor = Color.White,
                                    disabledContainerColor = themeColors.surface.copy(alpha = 0.3f),
                                    disabledContentColor = themeColors.onSurface.copy(alpha = 0.3f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Text(
                                    stringResource(R.string.rate),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtisticStarButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    themeColors: ThemeColors,
    shimmer: Float,
    index: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 0.96f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "scale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = if (isSelected) 360f else 0f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = 200f
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier
            .size(50.dp)
            .scale(scale)
            .graphicsLayer {
                rotationZ = rotation * 0.3f
            }
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background with orbital effect
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (isSelected) {
                // Pulsating glow
                val glowRadius = 25.dp.toPx() + sin((shimmer + index * 72f) * 0.05f) * 3.dp.toPx()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeColors.accent1.copy(alpha = 0.4f),
                            themeColors.accent2.copy(alpha = 0.2f),
                            Color.Transparent
                        ),
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = center
                )
                
                // Rotating ring
                val ringRadius = 22.dp.toPx()
                val angleOffset = (shimmer + index * 72f) * 0.5f
                drawArc(
                    color = themeColors.accent2.copy(alpha = 0.3f),
                    startAngle = angleOffset,
                    sweepAngle = 240f,
                    useCenter = false,
                    style = Stroke(width = 1.8.dp.toPx()),
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2)
                )
            }
            
            // Background circle
            drawCircle(
                brush = if (isSelected) {
                    Brush.radialGradient(
                        colors = listOf(
                            themeColors.accent1.copy(alpha = 0.3f),
                            themeColors.accent1.copy(alpha = 0.15f)
                        )
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(
                            themeColors.surface.copy(alpha = 0.5f),
                            themeColors.surface.copy(alpha = 0.3f)
                        )
                    )
                },
                radius = 25.dp.toPx(),
                center = center
            )
        }
        
        // Star icon
        Text(
            text = if (isSelected) "★" else "☆",
            fontSize = 28.sp,
            color = if (isSelected) {
                themeColors.accent1
            } else {
                themeColors.onSurface.copy(alpha = 0.4f)
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
