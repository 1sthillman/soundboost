package com.soundboost.ui.screens

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.ui.components.*
import com.soundboost.ui.theme.ThemeColors
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Modern Onboarding Experience with Smooth Transitions
 */
@Composable
fun OnboardingScreen(
    themeColors: ThemeColors,
    onComplete: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val haptic = rememberHapticManager()
    
    val pages = remember {
        listOf(
            OnboardingPage(
                title = "Profesyonel Ses Güçlendirme",
                description = "Cihazınızın ses limitlerini aşın. %300'e kadar ses artışı ile müziğinizi, videolarınızı ve oyunlarınızı daha yüksek sesle dinleyin.",
                icon = Icons.Default.VolumeUp,
                color = Color(0xFF00F0FF)
            ),
            OnboardingPage(
                title = "Gelişmiş Ses İşleme",
                description = "10-bantlı equalizer, bas güçlendirme ve 3D ses efektleri ile ses kalitenizi optimize edin.",
                icon = Icons.Default.GraphicEq,
                color = Color(0xFFFF2DAA)
            ),
            OnboardingPage(
                title = "Ses Görselleştirme",
                description = "Gerçek zamanlı ses görselleştirme ile müziğinizi görün. Modern, sanatsal ve akıcı animasyonlar.",
                icon = Icons.Default.WavingHand,
                color = Color(0xFF9D4DFF)
            ),
            OnboardingPage(
                title = "Dokunmatik Kontroller",
                description = "Hassas dokunmatik kontroller ve haptic geri bildirim ile sezgisel kullanım deneyimi.",
                icon = Icons.Default.TouchApp,
                color = Color(0xFF39FF88)
            )
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background)
    ) {
        // Animated background
        AnimatedOnboardingBackground(
            page = currentPage,
            colors = pages.map { it.color }
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            
            // Page content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                OnboardingPageContent(
                    page = pages[currentPage],
                    themeColors = themeColors
                )
            }
            
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                pages.indices.forEach { index ->
                    PageIndicator(
                        isActive = index == currentPage,
                        color = pages[index].color
                    )
                }
            }
            
            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            haptic.click()
                            currentPage--
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = pages[currentPage].color
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(pages[currentPage].color, pages[currentPage].color.copy(alpha = 0.6f))
                            )
                        )
                    ) {
                        Text(
                            text = "GERİ",
                            modifier = Modifier.padding(vertical = 8.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
                
                Button(
                    onClick = {
                        haptic.heavyClick()
                        if (currentPage < pages.size - 1) {
                            currentPage++
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.weight(if (currentPage > 0) 2f else 1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pages[currentPage].color
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (currentPage < pages.size - 1) "DEVAM" else "BAŞLA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentPage < pages.size - 1) 
                                Icons.Default.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    themeColors: ThemeColors
) {
    val scale by rememberInfiniteTransition(label = "scale").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Animated icon
        Surface(
            modifier = Modifier
                .size(120.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            color = page.color.copy(alpha = 0.2f),
            tonalElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                page.color.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = page.icon,
                    contentDescription = null,
                    tint = page.color,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        // Title and description
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = page.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = themeColors.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )
            
            Text(
                text = page.description,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = themeColors.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun AnimatedOnboardingBackground(
    page: Int,
    colors: List<Color>
) {
    val transition = updateTransition(targetState = page, label = "pageTransition")
    
    val color1 by transition.animateColor(
        transitionSpec = { tween(800) },
        label = "color1"
    ) { pageIndex ->
        colors.getOrElse(pageIndex) { colors.first() }
    }
    
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Gradient orbs
        repeat(3) { i ->
            val angle = (rotation + i * 120f) * PI / 180
            val distance = size.minDimension * 0.4f
            val x = size.width / 2 + (distance * cos(angle)).toFloat()
            val y = size.height / 2 + (distance * sin(angle)).toFloat()
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color1.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    radius = size.minDimension * 0.5f
                ),
                radius = size.minDimension * 0.5f,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun PageIndicator(
    isActive: Boolean,
    color: Color
) {
    val width by animateDpAsState(
        targetValue = if (isActive) 32.dp else 8.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "width"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.3f,
        animationSpec = tween(300),
        label = "alpha"
    )
    
    Box(
        modifier = Modifier
            .width(width)
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = alpha))
    )
}

private data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Animated Splash Screen
 */
@Composable
fun AnimatedSplashScreen(
    onTimeout: () -> Unit
) {
    var isAnimating by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(2500)
        isAnimating = false
        delay(300)
        onTimeout()
    }
    
    val logoScale by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 1.3f,
        animationSpec = if (isAnimating) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        } else {
            tween(300)
        },
        label = "logoScale"
    )
    
    val logoAlpha by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(300),
        label = "logoAlpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A12),
                        Color(0xFF15151F),
                        Color(0xFF0A0A12)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background particles
        AudioReactiveParticleSystem(
            isActive = true,
            audioLevels = FloatArray(20) { 0.5f },
            accentColor = Color(0xFF00F0FF),
            particleCount = 30,
            particleType = ParticleType.GLOW_DOTS
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                    alpha = logoAlpha
                }
        ) {
            // App logo/icon
            PulsatingLogo()
            
            // App name
            Text(
                text = "Sound'ST Boost",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00F0FF),
                letterSpacing = 2.sp
            )
            
            Text(
                text = "Profesyonel Ses Güçlendirme",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun PulsatingLogo() {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val rotation by rememberInfiniteTransition(label = "rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                    rotationZ = rotation
                }
        ) {
            // Outer ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF00F0FF),
                        Color(0xFFFF2DAA),
                        Color(0xFF9D4DFF),
                        Color(0xFF00F0FF)
                    )
                ),
                radius = size.minDimension / 2,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Inner glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00F0FF).copy(alpha = 0.6f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2
            )
        }
        
        // Center icon
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = Color(0xFF00F0FF),
            modifier = Modifier.size(60.dp)
        )
    }
}
