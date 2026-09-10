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
 * JavaScript içindeki elementleri hedefler - GERÇEK KOORDİNATLAR
 */
@Composable
fun WebViewOnboardingOverlay(
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors,
    webView: android.webkit.WebView? = null
) {
    // JavaScript'ten gerçek koordinatları al
    var targetRect by remember { mutableStateOf<Rect?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Her adım değiştiğinde JavaScript'ten koordinatları al
    LaunchedEffect(currentStep, webView) {
        if (webView == null || currentStep !in 1..totalSteps) {
            targetRect = null
            return@LaunchedEffect
        }
        
        // Adıma göre metinleri ayarla
        when (currentStep) {
            1 -> {
                title = "Oynat Butonuna Bas"
                description = "Müziğin görselleşmesi için önce oynat butonuna dokun"
                
                // Play butonunun gerçek koordinatlarını al
                webView.evaluateJavascript(
                    "(function() { const pos = window.getElementPosition('play'); return pos ? JSON.stringify(pos) : null; })();"
                ) { result ->
                    try {
                        if (result != null && result != "null") {
                            val json = result.trim('"').replace("\\", "")
                            val coords = org.json.JSONObject(json)
                            targetRect = Rect(
                                left = coords.getDouble("left").toFloat(),
                                top = coords.getDouble("top").toFloat(),
                                right = coords.getDouble("right").toFloat(),
                                bottom = coords.getDouble("bottom").toFloat()
                            )
                            android.util.Log.d("Onboarding", "✅ Play button: $targetRect")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Onboarding", "❌ Failed to parse play button coords: $e")
                    }
                }
            }
            2 -> {
                title = "Ses ve Hassasiyeti Ayarla"
                description = "Sürgüleri hareket ettirerek sesi ve görsel duyarlılığını ayarlayabilirsin"
                
                // Sliders containerının koordinatlarını al
                webView.evaluateJavascript(
                    """(function() { 
                        const vol = document.getElementById('vol');
                        const sens = document.getElementById('sens');
                        if (vol && sens) {
                            const volRect = vol.getBoundingClientRect();
                            const sensRect = sens.getBoundingClientRect();
                            return JSON.stringify({
                                left: Math.min(volRect.left, sensRect.left) - 20,
                                top: volRect.top - 20,
                                right: Math.max(volRect.right, sensRect.right) + 20,
                                bottom: sensRect.bottom + 20
                            });
                        }
                        return null;
                    })();"""
                ) { result ->
                    try {
                        if (result != null && result != "null") {
                            val json = result.trim('"').replace("\\", "")
                            val coords = org.json.JSONObject(json)
                            targetRect = Rect(
                                left = coords.getDouble("left").toFloat(),
                                top = coords.getDouble("top").toFloat(),
                                right = coords.getDouble("right").toFloat(),
                                bottom = coords.getDouble("bottom").toFloat()
                            )
                            android.util.Log.d("Onboarding", "✅ Sliders: $targetRect")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Onboarding", "❌ Failed to parse sliders coords: $e")
                    }
                }
            }
            3 -> {
                title = "Tema Seç"
                description = "Aşağıdaki butonlardan moduna uygun bir görsel tema seçebilirsin"
                
                // Theme chips containerının koordinatlarını al
                webView.evaluateJavascript(
                    """(function() { 
                        const container = document.querySelector('.theme-chips');
                        if (container) {
                            const rect = container.getBoundingClientRect();
                            return JSON.stringify({
                                left: rect.left,
                                top: rect.top,
                                right: rect.right,
                                bottom: rect.bottom
                            });
                        }
                        return null;
                    })();"""
                ) { result ->
                    try {
                        if (result != null && result != "null") {
                            val json = result.trim('"').replace("\\", "")
                            val coords = org.json.JSONObject(json)
                            targetRect = Rect(
                                left = coords.getDouble("left").toFloat(),
                                top = coords.getDouble("top").toFloat(),
                                right = coords.getDouble("right").toFloat(),
                                bottom = coords.getDouble("bottom").toFloat()
                            )
                            android.util.Log.d("Onboarding", "✅ Theme chips: $targetRect")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Onboarding", "❌ Failed to parse theme chips coords: $e")
                    }
                }
            }
        }
    }
    
    // Koordinatlar hazır olduğunda tooltip göster
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
