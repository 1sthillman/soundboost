package com.soundboost.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

data class OnboardingStep(
    val title: String,
    val description: String,
    val targetId: String,
    val tooltipPosition: TooltipPosition = TooltipPosition.BOTTOM,
    val pulseEffect: Boolean = true
)

enum class TooltipPosition {
    TOP, BOTTOM, LEFT, RIGHT, CENTER
}

@Composable
fun OnboardingOverlay(
    steps: List<OnboardingStep>,
    currentStep: Int,
    onStepComplete: () -> Unit,
    onSkip: () -> Unit,
    targetBounds: Map<String, Rect>,
    modifier: Modifier = Modifier
) {
    if (currentStep >= steps.size) return
    
    val step = steps[currentStep]
    val bounds = targetBounds[step.targetId]
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1000f)
    ) {
        // Karartma katmanı (Spotlight efekti)
        SpotlightOverlay(
            targetBounds = bounds,
            pulseEffect = step.pulseEffect
        )
        
        // Tooltip balonu
        bounds?.let {
            TooltipBalloon(
                step = step,
                targetBounds = it,
                onNext = onStepComplete,
                onSkip = onSkip,
                isLastStep = currentStep == steps.size - 1
            )
        }
    }
}

@Composable
fun SpotlightOverlay(
    targetBounds: Rect?,
    pulseEffect: Boolean
) {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tam ekran karartma
        drawRect(
            color = Color.Black.copy(alpha = 0.85f),
            size = size
        )
        
        // Spotlight (açık alan)
        targetBounds?.let { bounds ->
            val padding = 16.dp.toPx()
            val scale = if (pulseEffect) pulseScale else 1f
            
            val expandedBounds = Rect(
                left = bounds.left - padding,
                top = bounds.top - padding,
                right = bounds.right + padding,
                bottom = bounds.bottom + padding
            )
            
            val centerX = expandedBounds.center.x
            val centerY = expandedBounds.center.y
            val width = expandedBounds.width * scale
            val height = expandedBounds.height * scale
            
            // Yuvarlak spotlight çiz
            drawCircle(
                color = Color.Transparent,
                radius = maxOf(width, height) / 2,
                center = Offset(centerX, centerY),
                blendMode = BlendMode.Clear
            )
        }
    }
}

@Composable
fun TooltipBalloon(
    step: OnboardingStep,
    targetBounds: Rect,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Tooltip pozisyonunu hesapla
    val (tooltipOffset, alignment) = remember(targetBounds, step.tooltipPosition) {
        calculateTooltipPosition(
            targetBounds = targetBounds,
            tooltipPosition = step.tooltipPosition,
            screenWidth = screenWidth.value,
            screenHeight = screenHeight.value
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .align(alignment)
                .offset(tooltipOffset.first, tooltipOffset.second)
                .widthIn(max = 320.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E2E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = step.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Skip",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = step.description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6C5CE7)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isLastStep) "Başla!" else "Devam →",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private fun calculateTooltipPosition(
    targetBounds: Rect,
    tooltipPosition: TooltipPosition,
    screenWidth: Float,
    screenHeight: Float
): Pair<Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp>, Alignment> {
    return when (tooltipPosition) {
        TooltipPosition.BOTTOM -> {
            Pair(
                Pair(0.dp, (targetBounds.bottom + 24).dp),
                Alignment.TopCenter
            )
        }
        TooltipPosition.TOP -> {
            Pair(
                Pair(0.dp, (targetBounds.top - 200).dp),
                Alignment.TopCenter
            )
        }
        TooltipPosition.CENTER -> {
            Pair(
                Pair(0.dp, 0.dp),
                Alignment.Center
            )
        }
        else -> {
            Pair(
                Pair(0.dp, (targetBounds.bottom + 24).dp),
                Alignment.TopCenter
            )
        }
    }
}

// Hedef bileşen için modifier
@Composable
fun Modifier.onboardingTarget(
    targetId: String,
    onBoundsChanged: (String, Rect) -> Unit
): Modifier {
    return this.onGloballyPositioned { layoutCoordinates ->
        val bounds = layoutCoordinates.boundsInRoot()
        onBoundsChanged(targetId, bounds)
    }
}
