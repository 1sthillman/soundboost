package com.soundboost.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.soundboost.R
import com.soundboost.data.BoostSettings
import com.soundboost.ui.components.*
import com.soundboost.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    state: BoostSettings,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onEqChanged: (Float, Float, Float) -> Unit,
    onBack: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.equalizer),
                        fontWeight = FontWeight.Black,
                        fontSize = TextStyles.titleMedium,
                        letterSpacing = TextStyles.spacingWide
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = themeColors.background
                )
            )
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Spacer(Modifier.height(Spacing.xs))
            
            // Bass Boost Card
            EffectCard(
                title = stringResource(R.string.bass_boost),
                value = state.bassBoostPercent,
                accentColor = themeColors.accent1,
                surfaceColor = themeColors.surface,
                textColor = themeColors.onSurface,
                onValueChange = { onBassBoostChanged(it.toInt()) }
            )
            
            // Virtualizer Card
            EffectCard(
                title = stringResource(R.string.virtualizer),
                value = state.virtualizerPercent,
                accentColor = themeColors.accent2,
                surfaceColor = themeColors.surface,
                textColor = themeColors.onSurface,
                onValueChange = { onVirtualizerChanged(it.toInt()) }
            )
            
            // 3-Band EQ Card
            val isLightTheme = androidx.compose.foundation.isSystemInDarkTheme().not()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Corners.xl),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLightTheme) {
                        Color.Black.copy(alpha = 0.08f)
                    } else {
                        themeColors.surface.copy(alpha = 0.4f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level2)
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        "3-BAND EQUALIZER",
                        fontSize = TextStyles.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = themeColors.onSurface,
                        letterSpacing = TextStyles.spacingExtraWide
                    )
                    
                    EQBand(
                        label = stringResource(R.string.bass),
                        value = state.eqLowGain,
                        onValueChange = { onEqChanged(it, state.eqMidGain, state.eqHighGain) },
                        accentColor = themeColors.accent1,
                        surfaceColor = themeColors.surface.copy(alpha = 0.3f),
                        textColor = themeColors.onSurface
                    )
                    
                    EQBand(
                        label = stringResource(R.string.mid),
                        value = state.eqMidGain,
                        onValueChange = { onEqChanged(state.eqLowGain, it, state.eqHighGain) },
                        accentColor = themeColors.accent1,
                        surfaceColor = themeColors.surface.copy(alpha = 0.3f),
                        textColor = themeColors.onSurface
                    )
                    
                    EQBand(
                        label = stringResource(R.string.treble),
                        value = state.eqHighGain,
                        onValueChange = { onEqChanged(state.eqLowGain, state.eqMidGain, it) },
                        accentColor = themeColors.accent1,
                        surfaceColor = themeColors.surface.copy(alpha = 0.3f),
                        textColor = themeColors.onSurface
                    )
                }
            }
            
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun EffectCard(
    title: String,
    value: Int,
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color,
    onValueChange: (Float) -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (value > 0) 1f else 0.98f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "effectCardScale"
    )
    
    val isLightTheme = androidx.compose.foundation.isSystemInDarkTheme().not()
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(Corners.xl),
        colors = CardDefaults.cardColors(
            containerColor = if (isLightTheme) {
                Color.Black.copy(alpha = 0.08f)
            } else {
                surfaceColor.copy(alpha = 0.4f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (value > 0) Elevation.level2 else Elevation.level1
        )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    fontSize = TextStyles.titleSmall,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = TextStyles.spacingWide
                )
                Text(
                    "$value%",
                    fontSize = TextStyles.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }
            
            ModernSlider(
                value = value.toFloat(),
                onValueChange = onValueChange,
                valueRange = 0f..100f,
                valueLabel = "$value%",
                accentColor = accentColor,
                surfaceColor = if (isLightTheme) {
                    Color.Black.copy(alpha = 0.1f)
                } else {
                    surfaceColor.copy(alpha = 0.3f)
                }
            )
        }
    }
}

@Composable
private fun EQBand(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    surfaceColor: Color,
    textColor: Color
) {
    val isLightTheme = androidx.compose.foundation.isSystemInDarkTheme().not()
    
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                fontSize = TextStyles.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = TextStyles.spacingWide
            )
            Text(
                "${value.toInt()} dB",
                fontSize = TextStyles.titleMedium,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
        }
        ModernSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -15f..15f,
            valueLabel = "${value.toInt()} dB",
            accentColor = accentColor,
            surfaceColor = if (isLightTheme) {
                Color.Black.copy(alpha = 0.1f)
            } else {
                surfaceColor
            }
        )
    }
}
