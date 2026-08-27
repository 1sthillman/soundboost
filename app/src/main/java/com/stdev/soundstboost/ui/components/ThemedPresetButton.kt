package com.stdev.soundstboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stdev.soundstboost.ui.theme.AppTheme
import kotlin.math.*

@Composable
fun ThemedPresetButton(
    label: String,
    isSelected: Boolean,
    theme: AppTheme,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (theme) {
        AppTheme.NEON_DARK -> NeonButton(label, isSelected, accent1, accent2, surfaceColor, onClick, modifier)
        AppTheme.OCEAN_BLUE -> WaveButton(label, isSelected, accent1, accent2, surfaceColor, onClick, modifier)
        AppTheme.SUNSET_ORANGE -> PillButton(label, isSelected, accent1, accent2, surfaceColor, onClick, modifier)
        AppTheme.FOREST_GREEN -> LeafButton(label, isSelected, accent1, accent2, surfaceColor, onClick, modifier)
        AppTheme.ROYAL_PURPLE -> GemButton(label, isSelected, accent1, accent2, surfaceColor, onClick, modifier)
    }
}

// NEON: Glowing rectangle with animated border
@Composable
private fun NeonButton(
    label: String,
    isSelected: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val glowAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
            .height(56.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val rect = androidx.compose.ui.geometry.Rect(
                0f, 0f, size.width, size.height
            )
            
            // Background
            drawRoundRect(
                color = if (isSelected) accent1.copy(alpha = 0.2f) else surfaceColor.copy(alpha = 0.3f),
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(12.dp.toPx())
            )
            
            // Glowing border when selected
            if (isSelected) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(accent1, accent2, accent1),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    topLeft = Offset.Zero,
                    size = size,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Outer glow
                drawRoundRect(
                    color = accent1.copy(alpha = glowAlpha * 0.3f),
                    topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                    size = Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()),
                    cornerRadius = CornerRadius(14.dp.toPx()),
                    blendMode = BlendMode.Plus
                )
            }
        }
        
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) accent1 else Color.Gray,
            letterSpacing = 1.sp
        )
    }
}

// OCEAN: Wave-shaped button
@Composable
private fun WaveButton(
    label: String,
    isSelected: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val waveOffset by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        )
    )
    
    Box(
        modifier = modifier
            .height(58.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, size.height / 2)
                
                // Top wave
                for (x in 0..size.width.toInt() step 4) {
                    val wave = if (isSelected) {
                        sin((x / size.width * 360 + waveOffset) * PI / 180) * 4.dp.toPx()
                    } else 0f
                    lineTo(x.toFloat(), size.height / 2 - 20.dp.toPx() + wave.toFloat())
                }
                
                lineTo(size.width, size.height / 2 + 20.dp.toPx())
                
                // Bottom wave
                for (x in size.width.toInt() downTo 0 step 4) {
                    val wave = if (isSelected) {
                        sin((x / size.width * 360 - waveOffset) * PI / 180) * 4.dp.toPx()
                    } else 0f
                    lineTo(x.toFloat(), size.height / 2 + 20.dp.toPx() + wave.toFloat())
                }
                
                close()
            }
            
            drawPath(
                path = path,
                brush = if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(accent1, accent2, accent1)
                    )
                } else Brush.horizontalGradient(
                    colors = listOf(surfaceColor, surfaceColor)
                )
            )
        }
        
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}

// SUNSET: Rounded pill with gradient
@Composable
private fun PillButton(
    label: String,
    isSelected: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    Box(
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                brush = if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(accent1, accent2),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                } else Brush.linearGradient(
                    colors = listOf(surfaceColor, surfaceColor)
                ),
                topLeft = Offset.Zero,
                size = size,
                cornerRadius = CornerRadius(27.dp.toPx())
            )
            
            // Highlight on top
            if (isSelected) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height / 2),
                    cornerRadius = CornerRadius(27.dp.toPx())
                )
            }
        }
        
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}

// FOREST: Organic leaf shape
@Composable
private fun LeafButton(
    label: String,
    isSelected: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val morphOffset by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        )
    )
    
    Box(
        modifier = modifier
            .height(56.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                val centerY = size.height / 2
                val amplitude = if (isSelected) 6.dp.toPx() else 2.dp.toPx()
                
                moveTo(0f, centerY)
                
                // Top organic curve
                for (x in 0..size.width.toInt() step 3) {
                    val wave = sin((x / size.width * 180 + morphOffset) * PI / 180) * amplitude
                    cubicTo(
                        x - 3f, centerY - 18.dp.toPx() + wave.toFloat(),
                        x.toFloat(), centerY - 18.dp.toPx() + wave.toFloat(),
                        x.toFloat(), centerY - 18.dp.toPx() + wave.toFloat()
                    )
                }
                
                lineTo(size.width, centerY)
                
                // Bottom organic curve
                for (x in size.width.toInt() downTo 0 step 3) {
                    val wave = sin((x / size.width * 180 - morphOffset) * PI / 180) * amplitude
                    cubicTo(
                        x + 3f, centerY + 18.dp.toPx() + wave.toFloat(),
                        x.toFloat(), centerY + 18.dp.toPx() + wave.toFloat(),
                        x.toFloat(), centerY + 18.dp.toPx() + wave.toFloat()
                    )
                }
                
                close()
            }
            
            drawPath(
                path = path,
                brush = if (isSelected) {
                    Brush.radialGradient(
                        colors = listOf(accent1, accent2),
                        center = Offset(size.width / 2, size.height / 2)
                    )
                } else Brush.radialGradient(
                    colors = listOf(surfaceColor, surfaceColor.copy(alpha = 0.8f))
                )
            )
        }
        
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}

// ROYAL: Diamond/Gem shaped button
@Composable
private fun GemButton(
    label: String,
    isSelected: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val shimmer by rememberInfiniteTransition().animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Box(
        modifier = modifier
            .height(52.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val width = size.width
            val height = size.height
            
            // Diamond path
            val diamondPath = Path().apply {
                moveTo(centerX, 0f)
                lineTo(width - 8.dp.toPx(), centerY)
                lineTo(centerX, height)
                lineTo(8.dp.toPx(), centerY)
                close()
            }
            
            // Fill
            drawPath(
                path = diamondPath,
                brush = if (isSelected) {
                    Brush.linearGradient(
                        colors = listOf(accent1, accent2, accent1),
                        start = Offset(0f, 0f),
                        end = Offset(width, height)
                    )
                } else Brush.linearGradient(
                    colors = listOf(surfaceColor, surfaceColor)
                )
            )
            
            // Shimmer effect when selected
            if (isSelected) {
                val shimmerPath = Path().apply {
                    val shimmerX = centerX + (shimmer * width / 2)
                    moveTo(shimmerX - 10.dp.toPx(), 0f)
                    lineTo(shimmerX + 10.dp.toPx(), 0f)
                    lineTo(shimmerX + 15.dp.toPx(), height)
                    lineTo(shimmerX - 15.dp.toPx(), height)
                    close()
                }
                
                drawPath(
                    path = shimmerPath,
                    color = Color.White.copy(alpha = 0.3f),
                    blendMode = BlendMode.Plus
                )
            }
            
            // Border
            drawPath(
                path = diamondPath,
                color = if (isSelected) accent1 else Color.Gray,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) Color.White else Color.Gray,
            letterSpacing = 1.sp
        )
    }
}
