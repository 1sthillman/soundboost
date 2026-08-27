package com.stdev.soundstboost.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stdev.soundstboost.data.BoostSettings
import com.stdev.soundstboost.ui.components.BoostDial
import com.stdev.soundstboost.ui.components.EqualizerVisualizer
import com.stdev.soundstboost.ui.components.NeonSlider
import com.stdev.soundstboost.ui.theme.NeonCyan
import com.stdev.soundstboost.ui.theme.NeonGreen
import com.stdev.soundstboost.ui.theme.NeonPink
import com.stdev.soundstboost.ui.theme.NeonPurple

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
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sound'ST Boost", fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Ayarlar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            EqualizerVisualizer(isActive = state.isBoostEnabled, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))

            BoostDial(
                isActive = state.isBoostEnabled,
                progressPercent = ((state.masterGainPercent - 100) * 100 / 200).coerceIn(0, 100),
                onClick = onToggleBoost
            )

            Spacer(Modifier.height(28.dp))

            NeonSlider(
                label = "ANA GÜÇ",
                value = state.masterGainPercent.toFloat(),
                valueRange = 100f..300f,
                accentColor = NeonCyan,
                valueLabel = "+${(state.masterGainPercent - 100) * 20 / 200} dB",
                onValueChange = { onMasterGainChanged(it.toInt()) }
            )

            NeonSlider(
                label = "BAS BOOST",
                value = state.bassBoostPercent.toFloat(),
                valueRange = 0f..100f,
                accentColor = NeonPink,
                valueLabel = "%${state.bassBoostPercent}",
                onValueChange = { onBassBoostChanged(it.toInt()) }
            )

            NeonSlider(
                label = "3D ALAN (VIRTUALIZER)",
                value = state.virtualizerPercent.toFloat(),
                valueRange = 0f..100f,
                accentColor = NeonPurple,
                valueLabel = "%${state.virtualizerPercent}",
                onValueChange = { onVirtualizerChanged(it.toInt()) }
            )

            Spacer(Modifier.height(8.dp))
            Text("EKOLAYZER", style = MaterialTheme.typography.labelLarge)

            NeonSlider(
                label = "Bas",
                value = state.eqLowGain,
                valueRange = -15f..15f,
                accentColor = NeonGreen,
                valueLabel = "${state.eqLowGain.toInt()} dB",
                onValueChange = { onEqChanged(it, state.eqMidGain, state.eqHighGain) }
            )
            NeonSlider(
                label = "Orta",
                value = state.eqMidGain,
                valueRange = -15f..15f,
                accentColor = NeonGreen,
                valueLabel = "${state.eqMidGain.toInt()} dB",
                onValueChange = { onEqChanged(state.eqLowGain, it, state.eqHighGain) }
            )
            NeonSlider(
                label = "Tiz",
                value = state.eqHighGain,
                valueRange = -15f..15f,
                accentColor = NeonGreen,
                valueLabel = "${state.eqHighGain.toInt()} dB",
                onValueChange = { onEqChanged(state.eqLowGain, state.eqMidGain, it) }
            )

            Spacer(Modifier.height(20.dp))

            OutlinedButton(onClick = onMaximizeVolume, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Donanım Sesini Maksimuma Çıkar")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
