package com.stdev.soundstboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.stdev.soundstboost.ui.theme.AppTheme
import kotlin.math.*

@Composable
fun ThemedVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    theme: AppTheme,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    when (theme) {
        AppTheme.NEON_DARK -> NeonBarsVisualizer(isActive, audioLevels, accent1, accent2, modifier)
        AppTheme.OCEAN_BLUE -> WaveRippleVisualizer(isActive, audioLevels, accent1, accent2, modifier)
        AppTheme.SUNSET_ORANGE -> CircularPulseVisualizer(isActive, audioLevels, accent1, accent2, modifier)
        AppTheme.FOREST_GREEN -> OrganicFlowVisualizer(isActive, audioLevels, accent1, accent2, modifier)
        AppTheme.ROYAL_PURPLE -> CrystalGeometricVisualizer(isActive, audioLevels, accent1, accent2, modifier)
    }
}

// NEON: Classic Bars with Glow
@Composable
private fun NeonBarsVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val barCount = 32
    val animatedLevels = remember { List(barCount) { Animatable(0f) } }
    
    val pulseAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    LaunchedEffect(audioLevels, isActive) {
        if (isActive && audioLevels != null) {
            audioLevels.take(barCount).forEachIndexed { index, level ->
                animatedLevels[index].animateTo(
                    targetValue = level.coerceIn(0.1f, 1f),
                    animationSpec = tween(30, easing = FastOutSlowInEasing)
                )
            }
        } else {
            animatedLevels.forEach { it.animateTo(0.1f, tween(300)) }
        }
    }
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val barWidth = size.width / (barCount * 2f)
        val maxHeight = size.height * 0.8f
        
        repeat(barCount) { index ->
            val level = animatedLevels[index].value
            val barHeight = maxHeight * level
            val x = index * (barWidth * 2) + barWidth / 2
            val color = lerp(accent1, accent2, index.toFloat() / barCount)
            
            // Glow effect
            if (isActive) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = pulseAlpha * 0.3f),
                            Color.Transparent
                        )
                    ),
                    topLeft = Offset(x - barWidth * 1.5f, size.height - barHeight - 10.dp.toPx()),
                    size = Size(barWidth * 3, barHeight + 20.dp.toPx())
                )
            }
            
            // Bar
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.5f))
                ),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

// OCEAN: Wave Ripples
@Composable
private fun WaveRippleVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val waveOffset by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        )
    )
    
    val avgLevel = audioLevels?.average()?.toFloat() ?: 0f
    val animatedLevel = remember { Animatable(0f) }
    
    LaunchedEffect(avgLevel, isActive) {
        if (isActive) {
            animatedLevel.animateTo(avgLevel, tween(35, easing = FastOutSlowInEasing))
        } else {
            animatedLevel.animateTo(0f, tween(500))
        }
    }
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val centerY = size.height / 2
        val amplitude = if (isActive) 20.dp.toPx() * (1 + animatedLevel.value) else 10.dp.toPx()
        
        // Draw multiple wave layers
        for (layer in 0..3) {
            val path = Path().apply {
                moveTo(0f, centerY)
                
                for (x in 0..size.width.toInt() step 4) {
                    val wave = sin((x / size.width * 360 * 2 + waveOffset + layer * 90) * PI / 180)
                    val y = centerY + (wave * amplitude * (1 - layer * 0.2f)).toFloat()
                    lineTo(x.toFloat(), y)
                }
            }
            
            drawPath(
                path = path,
                color = lerp(accent1, accent2, layer / 3f).copy(alpha = 0.3f - layer * 0.05f),
                style = Stroke(width = (3 - layer).dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        // Ripple circles when active
        if (isActive && animatedLevel.value > 0.3f) {
            for (i in 0..2) {
                val radius = 30.dp.toPx() + i * 20.dp.toPx() + (animatedLevel.value * 30.dp.toPx())
                drawCircle(
                    color = accent1.copy(alpha = 0.2f - i * 0.06f),
                    radius = radius,
                    center = Offset(size.width / 2, centerY),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

// SUNSET: Circular Pulse
@Composable
private fun CircularPulseVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        )
    )
    
    val circleCount = 12
    val animatedLevels = remember { List(circleCount) { Animatable(0f) } }
    
    LaunchedEffect(audioLevels, isActive) {
        if (isActive && audioLevels != null) {
            val step = audioLevels.size / circleCount
            repeat(circleCount) { index ->
                val level = audioLevels.getOrNull(index * step) ?: 0f
                animatedLevels[index].animateTo(
                    targetValue = level.coerceIn(0.2f, 1f),
                    animationSpec = tween(40, easing = FastOutSlowInEasing)
                )
            }
        } else {
            animatedLevels.forEach { it.animateTo(0.2f, tween(300)) }
        }
    }
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseRadius = minOf(size.width, size.height) * 0.35f
        
        rotate(rotation, pivot = Offset(centerX, centerY)) {
            repeat(circleCount) { index ->
                val angle = (index * 360f / circleCount) * PI / 180
                val level = animatedLevels[index].value
                val radius = baseRadius * level
                val distance = baseRadius * 0.7f
                
                val x = centerX + (distance * cos(angle)).toFloat()
                val y = centerY + (distance * sin(angle)).toFloat()
                
                val color = lerp(accent1, accent2, index.toFloat() / circleCount)
                
                // Outer glow
                drawCircle(
                    color = color.copy(alpha = 0.3f * level),
                    radius = radius * 1.3f,
                    center = Offset(x, y)
                )
                
                // Circle
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color, color.copy(alpha = 0.6f))
                    ),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }
    }
}

// FOREST: Organic Flow
@Composable
private fun OrganicFlowVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val flow1 by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        )
    )
    
    val flow2 by rememberInfiniteTransition().animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing)
        )
    )
    
    val avgLevel = audioLevels?.average()?.toFloat() ?: 0f
    val animatedLevel = remember { Animatable(0f) }
    
    LaunchedEffect(avgLevel, isActive) {
        if (isActive) {
            animatedLevel.animateTo(avgLevel, tween(40, easing = FastOutSlowInEasing))
        } else {
            animatedLevel.animateTo(0f, tween(400))
        }
    }
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val centerY = size.height / 2
        
        // Background flowing blobs
        for (blobIndex in 0..2) {
            val path = Path().apply {
                moveTo(0f, centerY)
                
                for (x in 0..size.width.toInt() step 5) {
                    val phase = flow1 + blobIndex * 120
                    val wave1 = sin((x / size.width * 180 + phase) * PI / 180)
                    val wave2 = cos((x / size.width * 120 + flow2) * PI / 180)
                    val amplitude = if (isActive) {
                        (20 + animatedLevel.value * 30) * (1 - blobIndex * 0.2f)
                    } else {
                        10f * (1 - blobIndex * 0.2f)
                    }
                    val y = centerY + ((wave1 + wave2) * amplitude / 2).toFloat()
                    
                    if (x == 0) moveTo(0f, y) else lineTo(x.toFloat(), y)
                }
            }
            
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accent1.copy(alpha = 0.2f - blobIndex * 0.05f),
                        accent2.copy(alpha = 0.25f - blobIndex * 0.05f),
                        accent1.copy(alpha = 0.2f - blobIndex * 0.05f)
                    )
                ),
                style = Stroke(width = (8 - blobIndex * 2).dp.toPx(), cap = StrokeCap.Round)
            )
        }
        
        // Particles when active
        if (isActive && animatedLevel.value > 0.2f) {
            repeat(8) { i ->
                val particleX = (i * size.width / 8) + (sin((flow1 + i * 45) * PI / 180) * 20).toFloat()
                val particleY = centerY + (cos((flow2 + i * 60) * PI / 180) * 30 * animatedLevel.value).toFloat()
                drawCircle(
                    color = accent2.copy(alpha = 0.4f),
                    radius = (3 + animatedLevel.value * 4).dp.toPx(),
                    center = Offset(particleX, particleY)
                )
            }
        }
    }
}

// ROYAL: Crystal Geometric
@Composable
private fun CrystalGeometricVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing)
        )
    )
    
    val shimmer by rememberInfiniteTransition().animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    val avgLevel = audioLevels?.average()?.toFloat() ?: 0f
    val animatedLevel = remember { Animatable(0f) }
    
    LaunchedEffect(avgLevel, isActive) {
        if (isActive) {
            animatedLevel.animateTo(avgLevel, tween(45, easing = FastOutSlowInEasing))
        } else {
            animatedLevel.animateTo(0f, tween(500))
        }
    }
    
    Canvas(modifier = modifier.fillMaxWidth()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val baseSize = minOf(size.width, size.height) * 0.4f
        
        rotate(rotation, pivot = Offset(centerX, centerY)) {
            // Draw rotating geometric shapes
            for (layer in 0..2) {
                val vertices = 6
                val radius = baseSize * (0.3f + layer * 0.2f) * (1 + animatedLevel.value * 0.3f)
                val path = Path()
                
                repeat(vertices) { i ->
                    val angle = (i * 360f / vertices) * PI / 180
                    val x = centerX + (radius * cos(angle)).toFloat()
                    val y = centerY + (radius * sin(angle)).toFloat()
                    
                    if (i == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }
                path.close()
                
                val color = lerp(accent1, accent2, layer / 2f)
                
                // Fill
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.1f + (if (isActive) animatedLevel.value * 0.2f else 0f))
                )
                
                // Border
                drawPath(
                    path = path,
                    color = color.copy(alpha = 0.5f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
        
        // Shimmer effect
        if (isActive) {
            val shimmerX = centerX + (shimmer * size.width / 2)
            val shimmerGradient = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accent1.copy(alpha = 0.3f),
                    Color.Transparent
                ),
                startX = shimmerX - 50.dp.toPx(),
                endX = shimmerX + 50.dp.toPx()
            )
            
            drawRect(
                brush = shimmerGradient,
                topLeft = Offset(0f, 0f),
                size = size
            )
        }
    }
}
