package com.stdev.soundstboost.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.stdev.soundstboost.R
import com.stdev.soundstboost.data.BoostSettings
import com.stdev.soundstboost.ui.components.ModernVolumeControl
import com.stdev.soundstboost.ui.components.ThemedVisualizer
import com.stdev.soundstboost.ui.components.ThemedPresetButton
import com.stdev.soundstboost.ui.theme.AppTheme
import com.stdev.soundstboost.ui.theme.getThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeScreen(
    state: BoostSettings,
    audioLevels: FloatArray?,
    onVolumeChange: (Int) -> Unit,
    onToggleBoost: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onOpenLanguage: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isSmallScreen = screenHeight < 700.dp
    
    // System UI Controller - Navigation bar rengi
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = themeColors.background,
            darkIcons = !themeColors.isDark
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (state.theme) {
                    AppTheme.NEON_DARK -> Brush.verticalGradient(
                        colors = listOf(
                            themeColors.background,
                            themeColors.background.copy(alpha = 0.95f),
                            themeColors.primary.copy(alpha = 0.1f)
                        )
                    )
                    AppTheme.OCEAN_BLUE -> Brush.radialGradient(
                        colors = listOf(
                            themeColors.primary.copy(alpha = if (state.isBoostEnabled) 0.4f else 0.2f),
                            themeColors.background
                        )
                    )
                    AppTheme.SUNSET_ORANGE -> Brush.verticalGradient(
                        colors = listOf(
                            themeColors.accent1.copy(alpha = 0.2f),
                            themeColors.background,
                            themeColors.accent2.copy(alpha = 0.15f)
                        )
                    )
                    AppTheme.FOREST_GREEN -> Brush.verticalGradient(
                        colors = listOf(
                            themeColors.background,
                            themeColors.primary.copy(alpha = if (state.isBoostEnabled) 0.25f else 0.15f)
                        )
                    )
                    AppTheme.ROYAL_PURPLE -> Brush.linearGradient(
                        colors = listOf(
                            themeColors.background,
                            themeColors.primary.copy(alpha = 0.15f),
                            themeColors.background
                        )
                    )
                }
            )
    ) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.volume_boost),
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp,
                            color = themeColors.onSurface
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenLanguage) {
                            Icon(
                                Icons.Default.Language,
                                contentDescription = stringResource(R.string.language),
                                tint = themeColors.accent1
                            )
                        }
                        IconButton(onClick = onOpenEqualizer) {
                            Icon(
                                Icons.Default.Equalizer,
                                contentDescription = stringResource(R.string.equalizer),
                                tint = themeColors.accent1
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings),
                                tint = themeColors.accent2
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = themeColors.onSurface
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(if (isSmallScreen) 8.dp else 16.dp))
                
                // Animated Visualizer with Theme-specific design
                ThemedVisualizer(
                    isActive = state.isBoostEnabled,
                    audioLevels = audioLevels,
                    theme = state.theme,
                    accent1 = themeColors.accent1,
                    accent2 = themeColors.accent2,
                    backgroundColor = themeColors.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSmallScreen) 100.dp else 120.dp)
                )
                
                Spacer(Modifier.height(if (isSmallScreen) 12.dp else 20.dp))
                
                // Instruction Text
                Text(
                    text = if (state.isBoostEnabled) {
                        "● ${stringResource(R.string.boost_on)}"
                    } else {
                        stringResource(R.string.instruction_double_tap)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (state.isBoostEnabled) themeColors.accent1 else themeColors.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(if (isSmallScreen) 8.dp else 12.dp))
                
                // Quick Theme Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(Modifier.width(4.dp))
                    AppTheme.values().forEach { theme ->
                        val isSelected = state.theme == theme
                        val themeName = when (theme) {
                            AppTheme.NEON_DARK -> stringResource(R.string.theme_neon_dark)
                            AppTheme.OCEAN_BLUE -> stringResource(R.string.theme_ocean_blue)
                            AppTheme.SUNSET_ORANGE -> stringResource(R.string.theme_sunset_orange)
                            AppTheme.FOREST_GREEN -> stringResource(R.string.theme_forest_green)
                            AppTheme.ROYAL_PURPLE -> stringResource(R.string.theme_royal_purple)
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 68.dp else 60.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                getThemeColors(theme, state.colorAccent).accent1,
                                                getThemeColors(theme, state.colorAccent).accent2
                                            )
                                        )
                                    } else {
                                        Brush.radialGradient(
                                            colors = listOf(
                                                getThemeColors(theme, state.colorAccent).surface.copy(alpha = 0.9f),
                                                getThemeColors(theme, state.colorAccent).surface.copy(alpha = 0.7f)
                                            )
                                        )
                                    }
                                )
                                .clickable { onThemeChanged(theme) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = themeName.take(6),
                                    fontSize = if (isSelected) 10.sp else 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) Color.White else themeColors.onSurface.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                if (isSelected) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "●",
                                        fontSize = 8.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
                
                Spacer(Modifier.height(if (isSmallScreen) 16.dp else 24.dp))
                
                // Modern Volume Control
                ModernVolumeControl(
                    volumePercent = state.masterGainPercent,
                    isActive = state.isBoostEnabled,
                    theme = state.theme,
                    accent1 = themeColors.accent1,
                    accent2 = themeColors.accent2,
                    surfaceColor = themeColors.surface,
                    backgroundColor = themeColors.background,
                    onVolumeChange = onVolumeChange,
                    onToggle = onToggleBoost,
                    modifier = Modifier.size(if (isSmallScreen) 260.dp else 300.dp)
                )
                
                Spacer(Modifier.height(if (isSmallScreen) 16.dp else 24.dp))
                
                // Preset Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemedPresetButton(
                        label = "60%",
                        isSelected = state.masterGainPercent == 60,
                        theme = state.theme,
                        accent1 = themeColors.accent1,
                        accent2 = themeColors.accent2,
                        surfaceColor = themeColors.surface,
                        backgroundColor = themeColors.background,
                        onClick = { onVolumeChange(60) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemedPresetButton(
                        label = "100%",
                        isSelected = state.masterGainPercent == 100,
                        theme = state.theme,
                        accent1 = themeColors.accent1,
                        accent2 = themeColors.accent2,
                        surfaceColor = themeColors.surface,
                        backgroundColor = themeColors.background,
                        onClick = { onVolumeChange(100) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemedPresetButton(
                        label = "160%",
                        isSelected = state.masterGainPercent == 160,
                        theme = state.theme,
                        accent1 = themeColors.accent1,
                        accent2 = themeColors.accent2,
                        surfaceColor = themeColors.surface,
                        backgroundColor = themeColors.background,
                        onClick = { onVolumeChange(160) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemedPresetButton(
                        label = stringResource(R.string.preset_max),
                        isSelected = state.masterGainPercent == 200,
                        theme = state.theme,
                        accent1 = themeColors.accent1,
                        accent2 = themeColors.accent2,
                        surfaceColor = themeColors.surface,
                        backgroundColor = themeColors.background,
                        onClick = { onVolumeChange(200) },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(if (isSmallScreen) 24.dp else 32.dp))
            }
        }
    }
}
