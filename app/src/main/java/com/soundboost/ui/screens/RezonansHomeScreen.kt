package com.soundboost.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.data.BoostSettings
import com.soundboost.ui.components.RezonansVisualizer
import com.soundboost.ui.components.OnboardingStep
import com.soundboost.ui.components.OnboardingOverlay
import com.soundboost.ui.components.TooltipPosition
import com.soundboost.ui.components.onboardingTarget
import com.soundboost.ui.theme.AppTheme
import com.soundboost.ui.theme.getThemeColors

/**
 * Rezonans Home Screen - Award-winning design from awwardstheme.html
 * Clean, art-directed interface with 6 themed worlds
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RezonansHomeScreen(
    state: BoostSettings,
    audioLevels: FloatArray?,
    onVolumeChange: (Int) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onToggleBoost: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onOpenLanguage: () -> Unit,
    // YENİ: Onboarding parametreleri
    showOnboarding: Boolean = false,
    onboardingStep: Int = 0,
    onOnboardingStepComplete: () -> Unit = {},
    onSkipOnboarding: () -> Unit = {}
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    // Target bounds for onboarding
    var targetBounds by remember { mutableStateOf<Map<String, androidx.compose.ui.geometry.Rect>>(emptyMap()) }
    
    val onBoundsChanged: (String, androidx.compose.ui.geometry.Rect) -> Unit = { id, bounds ->
        targetBounds = targetBounds + (id to bounds)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        themeColors.background,
                        themeColors.surface.copy(alpha = 0.3f),
                        themeColors.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Topbar: Brand + Meter + Clock
            RezonansTopBar(themeColors, audioLevels, state.theme)
            
            // Stage: Canvas Visualizer
            RezonansStage(
                theme = state.theme,
                audioLevels = audioLevels,
                isActive = state.isBoostEnabled,
                sensitivity = state.sensitivity,
                themeColors = themeColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(294.dp)
                    .then(
                        if (showOnboarding && onboardingStep == 0) {
                            Modifier.onboardingTarget("visualizer", onBoundsChanged)
                        } else Modifier
                    )
            )
            
            // Track Info
            RezonansTrackInfo(state.theme, themeColors)
            
            // Transport: Play button + Sliders
            RezonansTransport(
                volumePercent = state.masterGainPercent,
                sensitivity = state.sensitivity,
                isActive = state.isBoostEnabled,
                onVolumeChange = onVolumeChange,
                onSensitivityChange = { onSensitivityChange(it.toInt()) },
                onToggle = onToggleBoost,
                themeColors = themeColors,
                showOnboarding = showOnboarding,
                onboardingStep = onboardingStep,
                onBoundsChanged = onBoundsChanged
            )
            
            // Theme Chips
            RezonansThemeChips(
                currentTheme = state.theme,
                onThemeChanged = onThemeChanged,
                themeColors = themeColors,
                showOnboarding = showOnboarding,
                onboardingStep = onboardingStep,
                onBoundsChanged = onBoundsChanged
            )
            
            Spacer(Modifier.weight(1f))
            
            // Bottom Navbar
            RezonansNavBar(
                onOpenSettings = onOpenSettings,
                onOpenEqualizer = onOpenEqualizer,
                onOpenLanguage = onOpenLanguage,
                themeColors = themeColors
            )
        }
        
        // YENİ: Onboarding Overlay
        if (showOnboarding) {
            val steps = listOf(
                OnboardingStep(
                    title = stringResource(R.string.onboarding_step1_title),
                    description = stringResource(R.string.onboarding_step1_desc),
                    targetId = "play_button",
                    tooltipPosition = TooltipPosition.BOTTOM,
                    pulseEffect = true
                ),
                OnboardingStep(
                    title = stringResource(R.string.onboarding_step2_title),
                    description = stringResource(R.string.onboarding_step2_desc),
                    targetId = "sliders",
                    tooltipPosition = TooltipPosition.BOTTOM,
                    pulseEffect = false
                ),
                OnboardingStep(
                    title = stringResource(R.string.onboarding_step3_title),
                    description = stringResource(R.string.onboarding_step3_desc),
                    targetId = "themes",
                    tooltipPosition = TooltipPosition.TOP,
                    pulseEffect = false
                )
            )
            
            OnboardingOverlay(
                steps = steps,
                currentStep = onboardingStep,
                onStepComplete = onOnboardingStepComplete,
                onSkip = onSkipOnboarding,
                targetBounds = targetBounds
            )
        }
    }
}

@Composable
private fun RezonansTopBar(
    themeColors: com.soundboost.ui.theme.ThemeColors,
    audioLevels: FloatArray?,
    theme: AppTheme
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand: "rezonans" with styled parts
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "re",
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = themeColors.onSurface,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "zo",
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = themeColors.accent1,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "nans",
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = themeColors.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio Meter
            AudioMeter(audioLevels, themeColors)
            
            // Clock
            val currentTime = remember {
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date())
            }
            Text(
                text = currentTime,
                fontSize = 11.sp,
                color = themeColors.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AudioMeter(audioLevels: FloatArray?, themeColors: com.soundboost.ui.theme.ThemeColors) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(14.dp)
    ) {
        repeat(5) { index ->
            val level = audioLevels?.getOrNull(3 + index * 8) ?: 0f
            val height = (2 + level * 12).dp
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(height)
                    .background(
                        color = themeColors.accent1.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
private fun RezonansStage(
    theme: AppTheme,
    audioLevels: FloatArray?,
    isActive: Boolean,
    sensitivity: Int,
    themeColors: com.soundboost.ui.theme.ThemeColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
    ) {
        // Visualizer Canvas
        RezonansVisualizer(
            theme = theme,
            audioLevels = audioLevels,
            isActive = isActive,
            sensitivity = sensitivity,
            accent1 = themeColors.accent1,
            accent2 = themeColors.accent2,
            backgroundColor = themeColors.background,
            modifier = Modifier.fillMaxSize()
        )
        
        // Hint text when not playing
        if (!isActive) {
            Text(
                text = stringResource(R.string.instruction_double_tap),
                fontSize = 11.sp,
                color = themeColors.onSurfaceVariant.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 13.dp)
            )
        }
    }
}

@Composable
private fun RezonansTrackInfo(theme: AppTheme, themeColors: com.soundboost.ui.theme.ThemeColors) {
    val (title, subtitle) = when (theme) {
        AppTheme.MEHTAP -> "Ay Balıkçısı" to "Durgun suda sabırla bekleyen bir sandalın hikâyesi"
        AppTheme.SUMI -> "Mürekkep Nefesi" to "Fırça darbeleriyle beliren tek çizgi"
        AppTheme.AURORA -> "Kutup Şafağı" to "Gökyüzünde süzülen ışık şeritleri"
        AppTheme.FENER -> "Fenerin Türküsü" to "Dünyanın ucunda, sisli denize yol gösteren bekçi"
        AppTheme.ORMAN -> "Göl Kenarında Ateş" to "Bir gezgin, bir köpek, közlenen bir ateş ve basın ritmiyle nefes alan mavi bir göl"
        AppTheme.EYES -> "Derin Bakış" to "Okyanusun derinliklerinde açılan göz — bazen yaşar, bazen kızarır"
        AppTheme.MYCEL -> "Yeraltı Fısıltısı" to "Kökler arasında yayılan ışık sinyali"
        AppTheme.REEF -> "Derin Işıltı" to "Karanlıkta parıldayan biyolüminesans"
        AppTheme.MONSOON -> "Fırtına Öncesi" to "Şimşek ve yağmurun ritmi"
        else -> "Ay Balıkçısı" to "Durgun suda sabırla bekleyen bir sandalın hikâyesi"
    }
    
    Column(
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = title,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = themeColors.onSurface,
            letterSpacing = (-0.5).sp
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            color = themeColors.onSurfaceVariant
        )
    }
}

@Composable
private fun RezonansTransport(
    volumePercent: Int,
    sensitivity: Int,
    isActive: Boolean,
    onVolumeChange: (Int) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onToggle: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors,
    showOnboarding: Boolean = false,
    onboardingStep: Int = 0,
    onBoundsChanged: ((String, androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    // Stable callbacks to prevent recomposition
    val stableOnVolumeChange = rememberUpdatedState(onVolumeChange)
    val stableOnSensitivityChange = rememberUpdatedState(onSensitivityChange)
    val stableOnToggle = rememberUpdatedState(onToggle)
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause Button
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            themeColors.accent1.copy(alpha = 1.1f),
                            themeColors.accent1
                        )
                    )
                )
                .clickable(onClick = { stableOnToggle.value() })
                .then(
                    if (showOnboarding && onboardingStep == 0 && onBoundsChanged != null) {
                        Modifier.onboardingTarget("play_button", onBoundsChanged)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = themeColors.background,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Sliders
        Column(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (showOnboarding && onboardingStep == 1 && onBoundsChanged != null) {
                        Modifier.onboardingTarget("sliders", onBoundsChanged)
                    } else Modifier
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Volume Slider - KEYED to prevent recreation on other state changes
            key("volume_slider") {
                ThrottledSliderRow(
                    label = "Yükseltme",
                    value = volumePercent,
                    onValueChange = { stableOnVolumeChange.value(it.toInt()) },
                    valueRange = 60f..200f,
                    themeColors = themeColors
                )
            }
            
            // Sensitivity Slider - KEYED to prevent recreation on other state changes
            key("sensitivity_slider") {
                ThrottledSliderRow(
                    label = "Hassasiyet",
                    value = sensitivity,
                    onValueChange = { stableOnSensitivityChange.value(it) },
                    valueRange = 0f..100f,
                    themeColors = themeColors
                )
            }
        }
    }
}

@Composable
private fun ThrottledSliderRow(
    label: String,
    value: Int,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    // ULTRA-SIMPLE: Pure local state, ONE callback on finger lift
    var localValue by remember { mutableFloatStateOf(value.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Sync external value ONLY when not dragging
    LaunchedEffect(value) {
        if (!isDragging) {
            localValue = value.toFloat()
        }
    }
    
    // Create stable callback
    val stableOnValueChange = rememberUpdatedState(onValueChange)
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = themeColors.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        
        // PURE local UI - ZERO external updates during drag
        Slider(
            value = localValue,
            onValueChange = { newValue ->
                // Pure UI update - no side effects
                isDragging = true
                localValue = newValue
            },
            onValueChangeFinished = {
                // SINGLE callback when finger lifted
                isDragging = false
                stableOnValueChange.value(localValue)
            },
            valueRange = valueRange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = themeColors.accent1,
                activeTrackColor = themeColors.accent1.copy(alpha = 0.6f),
                inactiveTrackColor = themeColors.onSurface.copy(alpha = 0.18f)
            )
        )
        
        // Display updates instantly from local state
        Text(
            text = localValue.toInt().toString(),
            fontSize = 10.sp,
            color = themeColors.onSurfaceVariant,
            modifier = Modifier.width(30.dp)
        )
    }
}

@Composable
private fun RezonansThemeChips(
    currentTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors,
    showOnboarding: Boolean = false,
    onboardingStep: Int = 0,
    onBoundsChanged: ((String, androidx.compose.ui.geometry.Rect) -> Unit)? = null
) {
    val themes = listOf(
        AppTheme.MEHTAP to "Mehtap" to Color(0xFFf2b155),
        AppTheme.SUMI to "Sumi-e" to Color(0xFFc1442c),
        AppTheme.AURORA to "Kutup Şafağı" to Color(0xFF4fd8b0),
        AppTheme.FENER to "Son Işık" to Color(0xFFf2b155),
        AppTheme.ORMAN to "Ay Kampı" to Color(0xFFffb35c),
        AppTheme.EYES to "Derin Göz" to Color(0xFF3fe0d0),
        AppTheme.MYCEL to "Miselyum" to Color(0xFF6dffb0),
        AppTheme.REEF to "Derin Işıltı" to Color(0xFF12e0bd),
        AppTheme.MONSOON to "Muson" to Color(0xFF9cc2ff)
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 6.dp)
            .then(
                if (showOnboarding && onboardingStep == 2 && onBoundsChanged != null) {
                    Modifier.onboardingTarget("themes", onBoundsChanged)
                } else Modifier
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        themes.forEach { (themeData, color) ->
            val (theme, label) = themeData
            val isActive = currentTheme == theme
            
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onThemeChanged(theme) },
                color = if (isActive) {
                    themeColors.accent1.copy(alpha = 0.15f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Color Dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) themeColors.onSurface else themeColors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RezonansNavBar(
    onOpenSettings: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLanguage: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        NavItem(
            icon = Icons.Default.Home,
            label = "Ana Sayfa",
            isActive = true,
            onClick = { },
            themeColors = themeColors
        )
        
        NavItem(
            icon = Icons.Default.Settings,
            label = "Ayarlar",
            isActive = false,
            onClick = onOpenSettings,
            themeColors = themeColors
        )
        
        NavItem(
            icon = Icons.Default.Equalizer,
            label = "Ekolayzer",
            isActive = false,
            onClick = onOpenEqualizer,
            themeColors = themeColors
        )
    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    themeColors: com.soundboost.ui.theme.ThemeColors
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) themeColors.onSurface else themeColors.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isActive) themeColors.onSurface else themeColors.onSurfaceVariant
        )
    }
}
