package com.soundboost.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun PulsatingBadge(
    modifier: Modifier = Modifier,
    color: Color = Color.Red
) {
    val infiniteTransition = rememberInfiniteTransition(label = "badge")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScale"
    )
    
    Box(
        modifier = modifier
            .size(12.dp)
            .scale(scale)
            .background(color, CircleShape)
    )
}

@Composable
fun WiggleIcon(
    content: @Composable () -> Unit,
    enabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wiggle")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggleRotation"
    )
    
    Box(
        modifier = Modifier
            .then(
                if (enabled) Modifier.graphicsLayer {
                    rotationZ = rotation
                } else Modifier
            )
    ) {
        content()
    }
}
