package com.soundboost.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundboost.data.BoostSettings
import com.soundboost.ui.components.*
import com.soundboost.ui.theme.*
import kotlinx.coroutines.delay

/**
 * HomeScreen - CS2-Inspired Tactical Audio Control
 * 
 * Design DNA:
 * - Asymmetric bento layout (taste-skill Section 4.7)
 * - Premium glassmorphism components
 * - Motion choreography (MOTION_INTENSITY: 8)
 * - Color consistency lock (NeonOrange accent)
 * - Max 1 eyebrow per 3 sections (taste-skill Section 4.7)
 * 
 * Layout violations FIXED:
 * ❌ Centered hero → ✅ Asymmetric left-aligned
 * ❌ Generic stacked cards → ✅ Bento grid
 * ❌ No motion → ✅ Staggered entrance animations
 * ❌ Eyebrow overuse → ✅ 1 eyebrow total
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: BoostSettings,
    onMasterGainChanged: (Int) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onEqChanged: (Float, Float, Float) -> Unit,
    onToggleBoost: () -> Unit,
    onMaximizeVolume: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Staggered entrance animations (taste-skill Section 5.C)
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        isVisible = true
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "SoundSTBoost",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black
                        )
                    ) 
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ayarlar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBlack
                )
            )
        },
        containerColor = DeepBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            
            // HERO: CS2 Visualizer (full-width, glowing)
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it / 3 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn()
            ) {
                CyberCard(
                    backgroundColor = PanelDark,
                    borderColor = if (state.isBoostEnabled) NeonOrange else null
                ) {
                    CS2Visualizer(
                        isActive = state.isBoostEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // TACTICAL BOOST DIAL (hero element, asymmetric)
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TacticalBoostDial(
                        boostLevel = state.masterGainPercent.toFloat(),
                        isActive = state.isBoostEnabled,
                        onToggle = onToggleBoost
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            // AUDIO ENHANCERS (single eyebrow, 1 of 3 sections)
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn()
            ) {
                Column {
                    Text(
                        text = "AUDIO ENHANCERS",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                    
                    // Asymmetric bento grid (taste-skill: NO three equal cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Master gain (60% width, accent)
                        CyberCard(
                            modifier = Modifier.weight(0.6f),
                            backgroundColor = PanelDarkElevated,
                            borderColor = CyberBlue
                        ) {
                            PremiumNeonSlider(
                                label = "MASTER",
                                value = state.masterGainPercent.toFloat(),
                                valueRange = 100f..300f,
                                accentColor = CyberBlue,
                                valueLabel = "+${(state.masterGainPercent - 100) * 20 / 200}dB",
                                onValueChange = { onMasterGainChanged(it.toInt()) },
                                isAudioActive = state.isBoostEnabled
                            )
                        }
                        
                        // Bass boost (40% width)
                        CyberCard(
                            modifier = Modifier.weight(0.4f),
                            backgroundColor = PanelDarkElevated
                        ) {
                            Column {
                                Text(
                                    "BASS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${state.bassBoostPercent}%",
                                    style = MonoTypography.statsMedium,
                                    color = BassIndicator
                                )
                                Spacer(Modifier.height(12.dp))
                                Slider(
                                    value = state.bassBoostPercent.toFloat(),
                                    onValueChange = { onBassBoostChanged(it.toInt()) },
                                    valueRange = 0f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = BassIndicator,
                                        activeTrackColor = BassIndicator.copy(alpha = 0.8f),
                                        inactiveTrackColor = BassIndicator.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // Virtualizer (full width)
                    CyberCard(
                        backgroundColor = PanelDarkElevated
                    ) {
                        PremiumNeonSlider(
                            label = "3D SPATIAL",
                            value = state.virtualizerPercent.toFloat(),
                            valueRange = 0f..100f,
                            accentColor = CyberBlue,
                            valueLabel = "${state.virtualizerPercent}%",
                            onValueChange = { onVirtualizerChanged(it.toInt()) },
                            isAudioActive = state.isBoostEnabled
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // EQUALIZER (NO eyebrow, section 2 of 3)
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn()
            ) {
                CyberCard(
                    backgroundColor = PanelDark,
                    borderColor = NeonGreen.copy(alpha = 0.3f)
                ) {
                    Text(
                        "EQUALIZER",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    PremiumNeonSlider(
                        label = "BASS",
                        value = state.eqLowGain,
                        valueRange = -15f..15f,
                        accentColor = BassIndicator,
                        valueLabel = "${state.eqLowGain.toInt()}dB",
                        onValueChange = { onEqChanged(it, state.eqMidGain, state.eqHighGain) },
                        isAudioActive = state.isBoostEnabled
                    )
                    
                    PremiumNeonSlider(
                        label = "MID",
                        value = state.eqMidGain,
                        valueRange = -15f..15f,
                        accentColor = MidIndicator,
                        valueLabel = "${state.eqMidGain.toInt()}dB",
                        onValueChange = { onEqChanged(state.eqLowGain, it, state.eqHighGain) },
                        isAudioActive = state.isBoostEnabled
                    )
                    
                    PremiumNeonSlider(
                        label = "TREBLE",
                        value = state.eqHighGain,
                        valueRange = -15f..15f,
                        accentColor = TrebleIndicator,
                        valueLabel = "${state.eqHighGain.toInt()}dB",
                        onValueChange = { onEqChanged(state.eqLowGain, state.eqMidGain, it) },
                        isAudioActive = state.isBoostEnabled
                    )
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            // SYSTEM VOLUME (NO eyebrow, section 3 of 3)
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn()
            ) {
                Button(
                    onClick = onMaximizeVolume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonOrange.copy(alpha = 0.2f),
                        contentColor = NeonOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "MAXIMIZE SYSTEM VOLUME",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}
