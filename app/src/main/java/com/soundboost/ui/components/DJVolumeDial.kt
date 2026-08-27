package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun DJVolumeDial(
    volumePercent: Int,
    isActive: Boolean,
    accent1: Color,
    accent2: Color,
    surfaceColor: Color,
    backgroundColor: Color,
    onVolumeChange: (Int) -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableStateOf(volumePercent.toFloat()) }
    
    val animatedRotation by animateFloatAsState(
        targetValue = rotation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    val glowAlpha by rememberInfiniteTransition().animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
            .size(280.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    
                    val angle = atan2(
                        change.position.y - centerY,
                        change.position.x - centerX
                    ) * 180 / PI
                    
                    val deltaAngle = dragAmount.y * -0.5f + dragAmount.x * 0.5f
                    val newValue = (rotation + deltaAngle).coerceIn(60f, 200f)
                    
                    rotation = newValue
                    onVolumeChange(newValue.toInt())
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onToggle() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            val radius = minOf(canvasWidth, canvasHeight) / 2 - 40.dp.toPx()
            
            // Outer glow rings
            if (isActive) {
                for (i in 1..3) {
                    val glowRadius = radius + (i * 15.dp.toPx())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent1.copy(alpha = glowAlpha / (i * 2)),
                                Color.Transparent
                            ),
                            center = Offset(centerX, centerY),
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = Offset(centerX, centerY),
                        blendMode = BlendMode.Plus
                    )
                }
            }
            
            // Background circle with depth
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.3f),
                        surfaceColor,
                        surfaceColor.copy(alpha = 0.8f)
                    ),
                    center = Offset(centerX, centerY)
                ),
                radius = radius,
                center = Offset(centerX, centerY)
            )
            
            // Inner shadow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, centerY - 10.dp.toPx()),
                    radius = radius * 0.9f
                ),
                radius = radius * 0.9f,
                center = Offset(centerX, centerY)
            )
            
            // Track background
            val strokeWidth = 24.dp.toPx()
            drawArc(
                color = backgroundColor.copy(alpha = 0.3f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius + strokeWidth/2, centerY - radius + strokeWidth/2),
                size = Size((radius - strokeWidth/2) * 2, (radius - strokeWidth/2) * 2)
            )
            
            // Progress arc with gradient
            val progress = (animatedRotation - 60f) / 140f
            val sweepAngle = 270f * progress
            
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        accent1,
                        accent2,
                        accent1.copy(alpha = 0.8f),
                        accent2.copy(alpha = 0.9f),
                        accent1
                    ),
                    center = Offset(centerX, centerY)
                ),
                startAngle = 135f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(centerX - radius + strokeWidth/2, centerY - radius + strokeWidth/2),
                size = Size((radius - strokeWidth/2) * 2, (radius - strokeWidth/2) * 2)
            )
            
            // Highlight on progress
            if (sweepAngle > 0) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.White.copy(alpha = 0.2f)
                        ),
                        center = Offset(centerX, centerY)
                    ),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth * 0.5f, cap = StrokeCap.Round),
                    topLeft = Offset(centerX - radius + strokeWidth/2, centerY - radius + strokeWidth/2),
                    size = Size((radius - strokeWidth/2) * 2, (radius - strokeWidth/2) * 2),
                    blendMode = BlendMode.Plus
                )
            }
            
            // Notches
            for (i in 0..10) {
                val angle = 135f + (i * 27f)
                val radian = angle * PI / 180
                val notchStart = radius - strokeWidth - 10.dp.toPx()
                val notchEnd = radius - strokeWidth + 5.dp.toPx()
                
                val startX = centerX + (notchStart * cos(radian)).toFloat()
                val startY = centerY + (notchStart * sin(radian)).toFloat()
                val endX = centerX + (notchEnd * cos(radian)).toFloat()
                val endY = centerY + (notchEnd * sin(radian)).toFloat()
                
                drawLine(
                    color = if (i * 10 <= progress * 100) accent1.copy(alpha = 0.6f) 
                           else backgroundColor.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${volumePercent}%",
                fontSize = 56.sp,
                fontWeight = FontWeight.Black,
                color = if (isActive) accent1 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                style = MaterialTheme.typography.displayLarge.copy(
                    shadow = if (isActive) Shadow(
                        color = accent1.copy(alpha = 0.5f),
                        offset = Offset(0f, 4f),
                        blurRadius = 12f
                    ) else null
                )
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = if (isActive) "● ACTIVE" else "DOUBLE TAP",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = if (isActive) accent2 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
