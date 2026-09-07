package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.*
import kotlin.random.Random

/**
 * Advanced Audio-Reactive Particle System
 * Physics-based, GPU-optimized particle animations
 */
@Composable
fun AudioReactiveParticleSystem(
    isActive: Boolean,
    audioLevels: FloatArray?,
    accentColor: Color,
    modifier: Modifier = Modifier,
    particleCount: Int = 60,
    particleType: ParticleType = ParticleType.GLOW_DOTS
) {
    val particles = remember {
        List(particleCount) { index ->
            Particle(
                id = index,
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                velocityX = (Random.nextFloat() - 0.5f) * 0.002f,
                velocityY = (Random.nextFloat() - 0.5f) * 0.002f,
                size = Random.nextFloat() * 0.5f + 0.5f,
                hue = Random.nextFloat()
            )
        }
    }
    
    val time by rememberInfiniteTransition(label = "time").animateFloat(
        initialValue = 0f,
        targetValue = 10000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        if (isActive && audioLevels != null && audioLevels.isNotEmpty()) {
            particles.forEach { particle ->
                val audioIndex = particle.id % audioLevels.size
                val audioLevel = audioLevels[audioIndex].coerceIn(0f, 1f)
                
                if (audioLevel > 0.2f) {
                    // Update particle position
                    val newX = (particle.x + particle.velocityX * time) % 1f
                    val newY = (particle.y + particle.velocityY * time) % 1f
                    
                    val screenX = size.width * newX
                    val screenY = size.height * newY
                    
                    when (particleType) {
                        ParticleType.GLOW_DOTS -> drawGlowDot(
                            center = Offset(screenX, screenY),
                            size = particle.size * audioLevel,
                            color = accentColor,
                            intensity = audioLevel
                        )
                        ParticleType.SPARKLES -> drawSparkle(
                            center = Offset(screenX, screenY),
                            size = particle.size * audioLevel,
                            color = accentColor,
                            rotation = time + particle.id * 30f,
                            intensity = audioLevel
                        )
                        ParticleType.ENERGY_TRAILS -> drawEnergyTrail(
                            end = Offset(screenX, screenY),
                            particle = particle,
                            time = time,
                            color = accentColor,
                            intensity = audioLevel,
                            canvasSize = size
                        )
                        ParticleType.PLASMA_ORBS -> drawPlasmaOrb(
                            center = Offset(screenX, screenY),
                            size = particle.size * audioLevel,
                            color = accentColor,
                            time = time,
                            intensity = audioLevel
                        )
                    }
                }
            }
        }
    }
}

enum class ParticleType {
    GLOW_DOTS,
    SPARKLES,
    ENERGY_TRAILS,
    PLASMA_ORBS
}

private data class Particle(
    val id: Int,
    val x: Float,
    val y: Float,
    val velocityX: Float,
    val velocityY: Float,
    val size: Float,
    val hue: Float
)

private fun DrawScope.drawGlowDot(
    center: Offset,
    size: Float,
    color: Color,
    intensity: Float
) {
    val radius = (8f + size * 12f) * intensity
    
    // Outer glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = intensity * 0.4f),
                color.copy(alpha = intensity * 0.2f),
                Color.Transparent
            ),
            radius = radius * 2.5f
        ),
        radius = radius * 2.5f,
        center = center
    )
    
    // Core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = intensity * 0.8f),
                color.copy(alpha = intensity * 0.9f),
                color.copy(alpha = intensity * 0.5f)
            ),
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

private fun DrawScope.drawSparkle(
    center: Offset,
    size: Float,
    color: Color,
    rotation: Float,
    intensity: Float
) {
    val length = (10f + size * 15f) * intensity
    
    for (i in 0..3) {
        val angle = (rotation + i * 90f) * PI / 180
        val startX = center.x + (length * 0.3f * cos(angle)).toFloat()
        val startY = center.y + (length * 0.3f * sin(angle)).toFloat()
        val endX = center.x + (length * cos(angle)).toFloat()
        val endY = center.y + (length * sin(angle)).toFloat()
        
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = intensity * 0.9f),
                    color.copy(alpha = intensity * 0.6f),
                    Color.Transparent
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = (2f + intensity * 2f),
            cap = StrokeCap.Round
        )
    }
    
    // Center glow
    drawCircle(
        color = Color.White.copy(alpha = intensity * 0.9f),
        radius = 3f + intensity * 3f,
        center = center
    )
}

private fun DrawScope.drawEnergyTrail(
    end: Offset,
    particle: Particle,
    time: Float,
    color: Color,
    intensity: Float,
    canvasSize: androidx.compose.ui.geometry.Size
) {
    val trailLength = 5
    val points = mutableListOf<Offset>()
    
    for (i in 0..trailLength) {
        val timeOffset = time - i * 0.5f
        val trailX = (particle.x + particle.velocityX * timeOffset) % 1f
        val trailY = (particle.y + particle.velocityY * timeOffset) % 1f
        
        points.add(Offset(canvasSize.width * trailX, canvasSize.height * trailY))
    }
    
    // Draw trail segments
    for (i in 0 until points.size - 1) {
        val alpha = intensity * (1f - i.toFloat() / trailLength) * 0.6f
        val width = (3f + intensity * 4f) * (1f - i.toFloat() / trailLength)
        
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    color.copy(alpha = alpha),
                    color.copy(alpha = alpha * 0.5f)
                ),
                start = points[i],
                end = points[i + 1]
            ),
            start = points[i],
            end = points[i + 1],
            strokeWidth = width,
            cap = StrokeCap.Round
        )
    }
    
    // Head particle
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = intensity * 0.8f),
                color.copy(alpha = intensity * 0.6f),
                Color.Transparent
            ),
            radius = 8f + intensity * 6f
        ),
        radius = 8f + intensity * 6f,
        center = end
    )
}

private fun DrawScope.drawPlasmaOrb(
    center: Offset,
    size: Float,
    color: Color,
    time: Float,
    intensity: Float
) {
    val baseRadius = (12f + size * 18f) * intensity
    
    // Pulsating layers
    for (layer in 0..2) {
        val phaseOffset = layer * 120f
        val pulse = sin((time * 0.1f + phaseOffset) * PI / 180).toFloat()
        val layerRadius = baseRadius * (1f + pulse * 0.15f) * (1f + layer * 0.2f)
        val layerAlpha = intensity * (0.4f - layer * 0.1f)
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = layerAlpha),
                    color.copy(alpha = layerAlpha * 0.5f),
                    Color.Transparent
                ),
                radius = layerRadius
            ),
            radius = layerRadius,
            center = center
        )
    }
    
    // Electric arcs
    if (intensity > 0.7f) {
        for (arc in 0..2) {
            val arcAngle = (time * 0.3f + arc * 120f) * PI / 180
            val arcLength = baseRadius * 1.5f
            val arcEnd = Offset(
                center.x + (arcLength * cos(arcAngle)).toFloat(),
                center.y + (arcLength * sin(arcAngle)).toFloat()
            )
            
            drawLine(
                color = Color.White.copy(alpha = (intensity - 0.7f) * 0.6f),
                start = center,
                end = arcEnd,
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
        }
    }
    
    // Core
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = intensity),
                color.copy(alpha = intensity * 0.8f)
            ),
            radius = baseRadius * 0.4f
        ),
        radius = baseRadius * 0.4f,
        center = center
    )
}

/**
 * Confetti celebration effect
 */
@Composable
fun ConfettiExplosion(
    trigger: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {}
) {
    var isAnimating by remember { mutableStateOf(false) }
    
    val confettiPieces = remember {
        List(40) { index ->
            ConfettiPiece(
                angle = Random.nextFloat() * 360f,
                velocity = Random.nextFloat() * 0.5f + 0.3f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                color = if (index % 3 == 0) accentColor 
                       else if (index % 3 == 1) Color(0xFFFFD700)
                       else Color(0xFFFF69B4),
                size = Random.nextFloat() * 8f + 6f
            )
        }
    }
    
    LaunchedEffect(trigger) {
        if (trigger) {
            isAnimating = true
            kotlinx.coroutines.delay(2000)
            isAnimating = false
            onComplete()
        }
    }
    
    if (isAnimating) {
        val progress by rememberInfiniteTransition(label = "confetti").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )
        
        Canvas(modifier = modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            confettiPieces.forEach { piece ->
                val distance = progress * size.height * piece.velocity
                val angleRad = piece.angle * PI / 180
                
                val x = centerX + (distance * cos(angleRad)).toFloat()
                val y = centerY + (distance * sin(angleRad)).toFloat() + (progress * progress * 500f) // Gravity
                
                val rotation = progress * piece.rotationSpeed
                val alpha = (1f - progress).coerceIn(0f, 1f)
                
                rotate(rotation, Offset(x, y)) {
                    drawRect(
                        color = piece.color.copy(alpha = alpha),
                        topLeft = Offset(x - piece.size / 2, y - piece.size / 2),
                        size = androidx.compose.ui.geometry.Size(piece.size, piece.size * 1.5f)
                    )
                }
            }
        }
    }
}

private data class ConfettiPiece(
    val angle: Float,
    val velocity: Float,
    val rotationSpeed: Float,
    val color: Color,
    val size: Float
)

/**
 * Liquid/Fluid warp effect
 */
@Composable
fun LiquidWarpEffect(
    intensity: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val phase1 by rememberInfiniteTransition(label = "phase1").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "phase1"
    )
    
    val phase2 by rememberInfiniteTransition(label = "phase2").animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "phase2"
    )
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSize = 15
        val cellWidth = size.width / gridSize
        val cellHeight = size.height / gridSize
        
        for (row in 0..gridSize) {
            for (col in 0..gridSize) {
                val x = col * cellWidth
                val y = row * cellHeight
                
                // Warp calculation
                val dist = sqrt((x - size.width / 2).pow(2) + (y - size.height / 2).pow(2))
                val waveX = sin((dist * 0.05f + phase1) * PI / 180).toFloat() * intensity * 20f
                val waveY = cos((dist * 0.05f + phase2) * PI / 180).toFloat() * intensity * 20f
                
                val alpha = (0.3f * intensity * (1f - dist / (size.width / 2))).coerceIn(0f, 0.3f)
                
                drawCircle(
                    color = accentColor.copy(alpha = alpha),
                    radius = 3f + intensity * 2f,
                    center = Offset(x + waveX, y + waveY)
                )
            }
        }
    }
}
