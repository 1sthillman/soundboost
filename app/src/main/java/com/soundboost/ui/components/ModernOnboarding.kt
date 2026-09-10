package com.soundboost.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/**
 * Modern Onboarding Component
 * Tüm ekranı kaplamadan sadece hedef bölgeyi vurgular
 * Ok işareti ve minimal tooltip ile yönlendirir
 */
@Composable
fun ModernOnboardingTooltip(
    targetRect: Rect?,
    title: String,
    description: String,
    step: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    if (targetRect == null) return
    
    // Pulse animasyonu - hedef etrafında
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(999f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* Block clicks */ }
    ) {
        // Hafif karartma - SADECE hedef dışında
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fullPath = Path().apply {
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            }
            
            // Hedef etrafında "cutout" - padding ile genişletilmiş
            val padding = 16.dp.toPx()
            val targetPath = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        left = targetRect.left - padding,
                        top = targetRect.top - padding,
                        right = targetRect.right + padding,
                        bottom = targetRect.bottom + padding,
                        radiusX = 16.dp.toPx(),
                        radiusY = 16.dp.toPx()
                    )
                )
            }
            
            // Karartma (hedef hariç)
            drawPath(
                path = Path().apply {
                    op(fullPath, targetPath, androidx.compose.ui.graphics.PathOperation.Difference)
                },
                color = Color.Black.copy(alpha = 0.75f)
            )
            
            // Parlak border hedef etrafında
            drawRoundRect(
                color = themeColors.accent1.copy(alpha = pulseAlpha),
                topLeft = Offset(targetRect.left - padding, targetRect.top - padding),
                size = androidx.compose.ui.geometry.Size(
                    targetRect.width + padding * 2,
                    targetRect.height + padding * 2
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        
        // Tooltip kartı - hedefin altında veya üstünde
        TooltipCard(
            targetRect = targetRect,
            title = title,
            description = description,
            step = step,
            totalSteps = totalSteps,
            onNext = onNext,
            onSkip = onSkip,
            themeColors = themeColors
        )
    }
}

@Composable
private fun BoxScope.TooltipCard(
    targetRect: Rect,
    title: String,
    description: String,
    step: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    val density = LocalDensity.current
    val screenHeight = with(density) { 
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    
    // Tooltip'i hedefin altına mı yoksa üstüne mi koyacağız?
    val shouldPlaceBelow = targetRect.top < screenHeight / 2
    
    // Ok animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "arrow")
    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrowBounce"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .align(
                if (shouldPlaceBelow) Alignment.TopCenter else Alignment.BottomCenter
            )
            .offset(
                y = with(density) {
                    if (shouldPlaceBelow) {
                        (targetRect.bottom + 24.dp.toPx()).toDp()
                    } else {
                        -(screenHeight - targetRect.top + 24.dp.toPx()).toDp()
                    }
                }
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ok işareti (yukarı veya aşağı)
            if (!shouldPlaceBelow) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = themeColors.accent1,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(y = (-arrowOffset).dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            
            // Tooltip kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp),
                        spotColor = themeColors.accent1.copy(alpha = 0.3f)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Progress indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$step/$totalSteps",
                            fontSize = 12.sp,
                            color = themeColors.accent1,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        TextButton(onClick = onSkip) {
                            Text(
                                text = "Atla",
                                fontSize = 12.sp,
                                color = themeColors.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Title
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    // Description
                    Text(
                        text = description,
                        fontSize = 14.sp,
                        color = themeColors.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // Next button
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.accent1
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (step == totalSteps) "Başla!" else "Devam",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Ok işareti (aşağı)
            if (shouldPlaceBelow) {
                Spacer(Modifier.height(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = themeColors.accent1,
                    modifier = Modifier
                        .size(32.dp)
                        .offset(y = arrowOffset.dp)
                )
            }
        }
    }
}

/**
 * WebView için JavaScript tabanlı onboarding
 * JavaScript içindeki elementleri hedefler
 */
@Composable
fun WebViewOnboardingOverlay(
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    // WebView içindeki elementlerin pozisyonlarını JS'den alacağız
    // Şimdilik sabit koordinatlar kullanalım
    val density = LocalDensity.current
    val screenWidth = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp.toPx()
    }
    val screenHeight = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    
    // Her adım için hedef koordinatlar (WebView'deki elementlere göre)
    val targetRect = remember(currentStep, screenWidth, screenHeight) {
        when (currentStep) {
            1 -> Rect(
                // Play butonu (ortada, yukarıda)
                left = screenWidth * 0.35f,
                top = screenHeight * 0.25f,
                right = screenWidth * 0.65f,
                bottom = screenHeight * 0.35f
            )
            2 -> Rect(
                // Sliders (ortada)
                left = screenWidth * 0.1f,
                top = screenHeight * 0.42f,
                right = screenWidth * 0.9f,
                bottom = screenHeight * 0.58f
            )
            3 -> Rect(
                // Tema butonları (altta)
                left = screenWidth * 0.1f,
                top = screenHeight * 0.65f,
                right = screenWidth * 0.9f,
                bottom = screenHeight * 0.75f
            )
            else -> null
        }
    }
    
    val (title, description) = remember(currentStep) {
        when (currentStep) {
            1 -> "Oynat Butonuna Bas" to "Müziğin görselleşmesi için önce oynat butonuna bas"
            2 -> "Ayarları İncele" to "Yükseltme ve hassasiyet sürgüleriyle ses ve görsel duyarlılığını ayarla"
            3 -> "Tema Seç" to "Aşağıdaki butonlardan moduna uygun bir tema seç"
            else -> "" to ""
        }
    }
    
    if (currentStep in 1..totalSteps && targetRect != null) {
        ModernOnboardingTooltip(
            targetRect = targetRect,
            title = title,
            description = description,
            step = currentStep,
            totalSteps = totalSteps,
            onNext = onNext,
            onSkip = onSkip,
            themeColors = themeColors
        )
    }
}
