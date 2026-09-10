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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.soundboost.R

@Composable
fun OnboardingOverlayForWebView(
    currentStep: Int,
    onStepComplete: () -> Unit,
    onSkip: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    val context = LocalContext.current
    
    // Onboarding adımları
    val steps = listOf(
        Triple(
            context.getString(R.string.onboarding_step1_title),
            context.getString(R.string.onboarding_step1_desc),
            0.25f // Ekranın üst 25%'i (Play button yaklaşık burada)
        ),
        Triple(
            context.getString(R.string.onboarding_step2_title),
            context.getString(R.string.onboarding_step2_desc),
            0.40f // Ekranın 40%'ı (Sliders yaklaşık burada)
        ),
        Triple(
            context.getString(R.string.onboarding_step3_title),
            context.getString(R.string.onboarding_step3_desc),
            0.65f // Ekranın 65%'i (Themes yaklaşık burada)
        )
    )
    
    if (currentStep >= steps.size) {
        // Tüm adımlar tamamlandı
        LaunchedEffect(Unit) {
            onSkip() // Complete onboarding
        }
        return
    }
    
    val (title, description, spotlightPosition) = steps[currentStep]
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1000f)
    ) {
        // Karartma katmanı (Spotlight efekti)
        SimpleSpotlight(spotlightPosition = spotlightPosition)
        
        // Tooltip balonu
        SimpleTooltipCard(
            title = title,
            description = description,
            isLastStep = currentStep == steps.size - 1,
            onNext = onStepComplete,
            onSkip = onSkip,
            themeColors = themeColors
        )
    }
}

@Composable
fun SimpleSpotlight(spotlightPosition: Float) {
    val pulseAnimation = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulseAnimation.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Tam ekran karartma
        drawRect(
            color = Color.Black.copy(alpha = 0.85f),
            size = size
        )
        
        // Spotlight dairesel alan
        val centerX = canvasWidth / 2
        val centerY = canvasHeight * spotlightPosition
        val radius = 180.dp.toPx() * pulseScale
        
        drawCircle(
            color = Color.Transparent,
            radius = radius,
            center = Offset(centerX, centerY),
            blendMode = BlendMode.Clear
        )
    }
}

@Composable
fun SimpleTooltipCard(
    title: String,
    description: String,
    isLastStep: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeColors.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    IconButton(
                        onClick = onSkip,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = context.getString(R.string.onboarding_skip),
                            tint = themeColors.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = description,
                    fontSize = 15.sp,
                    color = themeColors.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Skip butonu
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = themeColors.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = context.getString(R.string.onboarding_skip),
                            fontSize = 14.sp
                        )
                    }
                    
                    // Next/Start butonu
                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.accent1
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLastStep) {
                                context.getString(R.string.onboarding_start)
                            } else {
                                context.getString(R.string.onboarding_next)
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColors.background
                        )
                    }
                }
            }
        }
    }
}
