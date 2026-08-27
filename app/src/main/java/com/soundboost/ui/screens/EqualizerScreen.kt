package com.soundboost.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.data.BoostSettings
import com.soundboost.ui.components.ModernSlider
import com.soundboost.ui.theme.getThemeColors

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
                        fontSize = 20.sp
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            // Bass Boost Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        stringResource(R.string.bass_boost),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    
                    ModernSlider(
                        value = state.bassBoostPercent.toFloat(),
                        onValueChange = { onBassBoostChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        valueLabel = "${state.bassBoostPercent}%",
                        accentColor = themeColors.accent1,
                        surfaceColor = themeColors.background
                    )
                }
            }
            
            // Virtualizer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        stringResource(R.string.virtualizer),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    
                    ModernSlider(
                        value = state.virtualizerPercent.toFloat(),
                        onValueChange = { onVirtualizerChanged(it.toInt()) },
                        valueRange = 0f..100f,
                        valueLabel = "${state.virtualizerPercent}%",
                        accentColor = themeColors.accent2,
                        surfaceColor = themeColors.background
                    )
                }
            }
            
            // 3-Band Equalizer Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = themeColors.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "3-BAND EQUALIZER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    
                    // Bass
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.bass),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = themeColors.onSurface.copy(alpha = 0.7f)
                        )
                        ModernSlider(
                            value = state.eqLowGain,
                            onValueChange = { onEqChanged(it, state.eqMidGain, state.eqHighGain) },
                            valueRange = -15f..15f,
                            valueLabel = "${state.eqLowGain.toInt()} dB",
                            accentColor = themeColors.accent1,
                            surfaceColor = themeColors.background
                        )
                    }
                    
                    // Mid
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.mid),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = themeColors.onSurface.copy(alpha = 0.7f)
                        )
                        ModernSlider(
                            value = state.eqMidGain,
                            onValueChange = { onEqChanged(state.eqLowGain, it, state.eqHighGain) },
                            valueRange = -15f..15f,
                            valueLabel = "${state.eqMidGain.toInt()} dB",
                            accentColor = themeColors.accent1,
                            surfaceColor = themeColors.background
                        )
                    }
                    
                    // Treble
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.treble),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = themeColors.onSurface.copy(alpha = 0.7f)
                        )
                        ModernSlider(
                            value = state.eqHighGain,
                            onValueChange = { onEqChanged(state.eqLowGain, state.eqMidGain, it) },
                            valueRange = -15f..15f,
                            valueLabel = "${state.eqHighGain.toInt()} dB",
                            accentColor = themeColors.accent1,
                            surfaceColor = themeColors.background
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}
