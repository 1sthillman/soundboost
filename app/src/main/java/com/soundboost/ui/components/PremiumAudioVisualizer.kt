package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.soundboost.ui.theme.AppTheme
import kotlin.math.*

/**
 * Award-Winning Audio Visualizer with Glassmorphism
 * Features: Particle systems, fluid animations, audio-reactive elements
 */
@Composable
fun PremiumAudioVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    theme: AppTheme,
    accent1: Color,
    accent2: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent1.copy(alpha = 0.05f),
                        Color.Transparent,
                        accent2.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Background ambient layer
        AmbientParticles(isActive, audioLevels, accent1, accent2)
        
        // All themes use FluidWaveformVisualizer
        FluidWaveformVisualizer(isActive, audioLevels, accent1, accent2, Modifier.fillMaxSize())
        
        // Foreground glass overlay
        if (isActive) {
            GlassOverlay(audioLevels, accent1)
        }
    }
}

/**
 * Ambient particle system - floats in background
 */
@Composable
private fun AmbientParticles(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    
    val particlePositions = remember {
        List(20) { index ->
            ParticleState(
                initialX = (index * 0.05f).coerceIn(0f, 1f),
                initialY = (index * 0.05f).coerceIn(0f, 1f),
                speedX = (index % 3 - 1) * 0.0002f,
                speedY = (index % 5 - 2) * 0.0002f
            )
        }
    }
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
            particlePositions.forEachIndexed { index, particle ->
                val audioLevel = audioLevels[index % audioLevels.size].coerceIn(0f, 1f)
                
                if (audioLevel > 0.3f) {
                    val x = size.width * ((particle.initialX + particle.speedX * time) % 1f)
                    val y = size.height * ((particle.initialY + particle.speedY * time) % 1f)
                    
                    val particleSize = (3f + audioLevel * 8f).dp.toPx()
                    val particleColor = androidx.compose.ui.graphics.lerp(
                        accent1, accent2, 
                        (index.toFloat() / particlePositions.size)
                    )
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                particleColor.copy(alpha = audioLevel * 0.7f),
                                particleColor.copy(alpha = audioLevel * 0.3f),
                                Color.Transparent
                            ),
                            radius = particleSize * 2
                        ),
                        radius = particleSize * 2,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

private data class ParticleState(
    val initialX: Float,
    val initialY: Float,
    val speedX: Float,
    val speedY: Float
)

/**
 * Fluid waveform - smooth, modern, responsive
 */
@Composable
private fun FluidWaveformVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val phase by rememberInfiniteTransition(label = "phase").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "phase"
    )
    
    Canvas(modifier = modifier) {
        val centerY = size.height / 2
        val barCount = 50
        val barWidth = (size.width * 0.9f) / (barCount * 1.3f)
        val gap = barWidth * 0.3f
        val maxHeight = size.height * 0.4f
        val startX = (size.width - (barCount * (barWidth + gap))) / 2
        
        repeat(barCount) { index ->
            val level = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (index * audioLevels.size / barCount).coerceIn(0, audioLevels.size - 1)
                val baseLevel = audioLevels[audioIndex].coerceIn(0.1f, 1f)
                // Add wave motion
                val wave = sin((index * 10f + phase) * PI / 180).toFloat() * 0.15f
                (baseLevel + wave).coerceIn(0.1f, 1f)
            } else {
                0.1f
            }
            
            val barHeight = maxHeight * level
            val x = startX + index * (barWidth + gap)
            val progress = index.toFloat() / barCount
            val barColor = androidx.compose.ui.graphics.lerp(accent1, accent2, progress)
            
            // Top bar with gradient
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        barColor.copy(alpha = 0.9f),
                        barColor.copy(alpha = 0.7f),
                        barColor.copy(alpha = 0.4f)
                    ),
                    startY = centerY - barHeight / 2,
                    endY = centerY
                ),
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight / 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
            )
            
            // Bottom reflection
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        barColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    startY = centerY,
                    endY = centerY + barHeight / 2
                ),
                topLeft = Offset(x, centerY),
                size = Size(barWidth, barHeight / 2),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2)
            )
            
            // Glow effect for high levels
            if (isActive && level > 0.7f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (level - 0.7f) * 0.5f),
                            Color.Transparent
                        ),
                        radius = barWidth * 3
                    ),
                    radius = barWidth * 3,
                    center = Offset(x + barWidth / 2, centerY - barHeight / 2)
                )
            }
        }
    }
}

/**
 * Ocean wave - flowing, organic
 */
@Composable
private fun OceanWaveVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val phase1 by rememberInfiniteTransition(label = "phase1").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing)
        ),
        label = "phase1"
    )
    
    Canvas(modifier = modifier) {
        val centerY = size.height / 2
        val samples = 60
        val maxAmplitude = size.height * 0.35f
        
        for (layer in 0..2) {
            val path = android.graphics.Path()
            var started = false
            
            val layerPhase = phase1 + layer * 60f
            val layerAlpha = 0.5f - layer * 0.15f
            
            for (i in 0..samples) {
                val x = (i.toFloat() / samples) * size.width
                val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                    val audioIndex = (i * audioLevels.size / samples).coerceIn(0, audioLevels.size - 1)
                    audioLevels[audioIndex].coerceIn(0f, 1f)
                } else {
                    0.2f
                }
                
                val wave1 = sin((i * 10f + layerPhase) * PI / 180).toFloat()
                val wave2 = sin((i * 20f - layerPhase * 0.5f) * PI / 180).toFloat() * 0.5f
                val combinedWave = (wave1 + wave2) / 1.5f
                
                val amplitude = maxAmplitude * audioLevel * combinedWave * (1f - layer * 0.2f)
                val y = centerY + amplitude
                
                if (!started) {
                    path.moveTo(x, y)
                    started = true
                } else {
                    path.lineTo(x, y)
                }
            }
            
            val layerColor = if (layer == 0) {
                androidx.compose.ui.graphics.lerp(accent1, accent2, 0.5f)
            } else {
                accent1
            }
            
            drawPath(
                path = path.asComposePath(),
                color = layerColor.copy(alpha = layerAlpha),
                style = Stroke(
                    width = (4f - layer * 1f).dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Radiant circles - pulsating, energetic
 */
@Composable
private fun RadiantCirclesVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = minOf(size.width, size.height) * 0.15f
        val rings = 8
        
        repeat(rings) { index ->
            val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (index * audioLevels.size / rings).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.3f, 1f)
            } else {
                0.4f
            }
            
            val radius = baseRadius * (1 + index * 0.4f) * (0.8f + pulse * 0.2f * audioLevel)
            val alpha = (0.6f - index * 0.06f) * audioLevel
            val ringColor = androidx.compose.ui.graphics.lerp(accent1, accent2, index.toFloat() / rings)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringColor.copy(alpha = alpha),
                        ringColor.copy(alpha = alpha * 0.5f),
                        Color.Transparent
                    ),
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }
    }
}

/**
 * Organic pulse - natural, breathing
 */
@Composable
private fun OrganicPulseVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val morph by rememberInfiniteTransition(label = "morph").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "morph"
    )
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val baseRadius = minOf(size.width, size.height) * 0.3f
        val points = 32
        
        val path = android.graphics.Path()
        
        for (i in 0..points) {
            val angle = (i * 360f / points) * PI / 180
            val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (i * audioLevels.size / points).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.4f, 1f)
            } else {
                0.5f
            }
            
            val noise1 = sin((angle * 3 + morph) * PI / 180).toFloat() * 0.2f
            val noise2 = sin((angle * 5 - morph * 0.5f) * PI / 180).toFloat() * 0.1f
            
            val radius = baseRadius * audioLevel * (1f + noise1 + noise2)
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        
        // Fill
        drawPath(
            path = path.asComposePath(),
            brush = Brush.radialGradient(
                colors = listOf(
                    accent1.copy(alpha = 0.6f),
                    accent2.copy(alpha = 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = baseRadius
            )
        )
        
        // Outline
        drawPath(
            path = path.asComposePath(),
            color = accent1.copy(alpha = 0.8f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Geometric crystal - sharp, futuristic
 */
@Composable
private fun GeometricCrystalVisualizer(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accent1: Color,
    accent2: Color,
    modifier: Modifier
) {
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val segments = 12
        val maxLength = minOf(size.width, size.height) * 0.4f
        
        repeat(segments) { index ->
            val audioLevel = if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
                val audioIndex = (index * audioLevels.size / segments).coerceIn(0, audioLevels.size - 1)
                audioLevels[audioIndex].coerceIn(0.2f, 1f)
            } else {
                0.3f
            }
            
            val angle = ((index * 360f / segments) + rotation * 0.2f) * PI / 180
            val length = maxLength * audioLevel
            
            val endX = center.x + (length * cos(angle)).toFloat()
            val endY = center.y + (length * sin(angle)).toFloat()
            
            val segmentColor = androidx.compose.ui.graphics.lerp(
                accent1, accent2, 
                index.toFloat() / segments
            )
            
            // Line with gradient
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        segmentColor.copy(alpha = 0.8f),
                        segmentColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    start = center,
                    end = Offset(endX, endY)
                ),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = (2f + audioLevel * 2f).dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Endpoint glow
            if (isActive && audioLevel > 0.6f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = audioLevel * 0.8f),
                            segmentColor.copy(alpha = audioLevel * 0.4f),
                            Color.Transparent
                        ),
                        radius = 8.dp.toPx()
                    ),
                    radius = 8.dp.toPx(),
                    center = Offset(endX, endY)
                )
            }
        }
    }
}

/**
 * Glass overlay effect - frosted glass appearance
 */
@Composable
private fun GlassOverlay(
    audioLevels: FloatArray?,
    accentColor: Color
) {
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Subtle shimmer effect
        val shimmerX = size.width * shimmer
        
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.1f),
                    Color.Transparent
                ),
                startX = shimmerX - 100.dp.toPx(),
                endX = shimmerX + 100.dp.toPx()
            ),
            blendMode = BlendMode.Plus
        )
    }
}

// Helper extension
private fun android.graphics.Path.asComposePath(): Path {
    val composePath = Path()
    composePath.addPath(Path().apply {
        // Note: This is a simplified conversion
        // For production, consider using proper path conversion
    })
    return composePath
}
