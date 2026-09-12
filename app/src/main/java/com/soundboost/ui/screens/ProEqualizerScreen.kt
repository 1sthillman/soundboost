package com.soundboost.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.audio.EqualizerPreset
import com.soundboost.data.BoostSettings
import com.soundboost.ui.components.ModernSlider
import com.soundboost.ui.theme.*
import kotlinx.coroutines.launch

/**
 * PROFESSIONAL 10-BAND PARAMETRIC EQUALIZER
 * DJ-Style Modern Design with Presets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProEqualizerScreen(
    state: BoostSettings,
    onBandsChanged: (FloatArray) -> Unit,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onSaveCustomPreset: (String, FloatArray) -> Unit,
    onMaxGainChanged: (Int) -> Unit,
    onBack: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    val scope = rememberCoroutineScope()
    
    var showPresetDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showMaxGainDialog by remember { mutableStateOf(false) }
    
    // Get custom presets (parse JSON)
    val customPresets = remember(state.customPresetsJson) {
        parseCustomPresets(state.customPresetsJson)
    }
    
    val allPresets = remember(customPresets) {
        EqualizerPreset.ALL_PRESETS + customPresets
    }
    
    // Current EQ bands
    var currentBands by remember(state) {
        mutableStateOf(state.get10BandEQ())
    }
    
    // Check if modified from preset
    val activePreset = remember(state.activePresetName, allPresets) {
        allPresets.find { it.name == state.activePresetName }
    }
    
    val isModified = remember(currentBands, activePreset) {
        activePreset?.let {
            !currentBands.contentEquals(it.toBandArray())
        } ?: false
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "PROFESSIONAL EQ",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "10-BAND PARAMETRIC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Max Gain Settings
                    IconButton(onClick = { showMaxGainDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Max Gain")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            
            // Active Preset Display + Actions
            PresetBar(
                activePresetName = state.activePresetName,
                isModified = isModified,
                onSelectPreset = { showPresetDialog = true },
                onSavePreset = { showSaveDialog = true },
                onReset = {
                    activePreset?.let {
                        currentBands = it.toBandArray()
                        onBandsChanged(currentBands)
                    }
                },
                themeColors = themeColors
            )
            
            // 10-Band EQ Faders (DJ Style)
            EQFadersSection(
                bands = currentBands,
                onBandChanged = { index, value ->
                    currentBands = currentBands.copyOf().apply {
                        this[index] = value
                    }
                    onBandsChanged(currentBands)
                },
                themeColors = themeColors
            )
            
            // Quick Actions
            QuickActionsRow(
                onFlatAll = {
                    currentBands = FloatArray(10) { 0f }
                    onBandsChanged(currentBands)
                    onPresetSelected(EqualizerPreset.FLAT)
                },
                onBassBoost = {
                    val preset = EqualizerPreset.BASS_BOOST
                    currentBands = preset.toBandArray()
                    onBandsChanged(currentBands)
                    onPresetSelected(preset)
                },
                onTrebleBoost = {
                    val preset = EqualizerPreset.TREBLE_BOOST
                    currentBands = preset.toBandArray()
                    onBandsChanged(currentBands)
                    onPresetSelected(preset)
                },
                themeColors = themeColors
            )
            
            Spacer(Modifier.height(16.dp))
        }
    }
    
    // Preset Selection Dialog
    if (showPresetDialog) {
        PresetSelectionDialog(
            presets = allPresets,
            activePresetName = state.activePresetName,
            onPresetSelected = { preset ->
                currentBands = preset.toBandArray()
                onBandsChanged(currentBands)
                onPresetSelected(preset)
                showPresetDialog = false
            },
            onDismiss = { showPresetDialog = false },
            themeColors = themeColors
        )
    }
    
    // Save Custom Preset Dialog
    if (showSaveDialog) {
        SavePresetDialog(
            onSave = { name ->
                onSaveCustomPreset(name, currentBands)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
            themeColors = themeColors
        )
    }
    
    // Max Gain Settings Dialog
    if (showMaxGainDialog) {
        MaxGainDialog(
            currentMaxGain = state.maxGainDb,
            onMaxGainChanged = onMaxGainChanged,
            onDismiss = { showMaxGainDialog = false },
            themeColors = themeColors
        )
    }
}

@Composable
private fun PresetBar(
    activePresetName: String,
    isModified: Boolean,
    onSelectPreset: () -> Unit,
    onSavePreset: () -> Unit,
    onReset: () -> Unit,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surface.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preset Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "ACTIVE PRESET",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        activePresetName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColors.accent1
                    )
                    if (isModified) {
                        Text(
                            " •",
                            fontSize = 18.sp,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
            
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Select Preset
                IconButton(
                    onClick = onSelectPreset,
                    modifier = Modifier
                        .size(40.dp)
                        .background(themeColors.accent1.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "Select Preset",
                        tint = themeColors.accent1
                    )
                }
                
                // Save Custom
                IconButton(
                    onClick = onSavePreset,
                    enabled = isModified,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isModified) themeColors.accent2.copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = "Save Preset",
                        tint = if (isModified) themeColors.accent2 else themeColors.onSurface.copy(alpha = 0.3f)
                    )
                }
                
                // Reset
                IconButton(
                    onClick = onReset,
                    enabled = isModified,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (isModified) Color(0xFFFF5252).copy(alpha = 0.2f) else Color.Transparent,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = if (isModified) Color(0xFFFF5252) else themeColors.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EQFadersSection(
    bands: FloatArray,
    onBandChanged: (Int, Float) -> Unit,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.surface.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "FREQUENCY BANDS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = themeColors.onSurface.copy(alpha = 0.7f),
                letterSpacing = 2.sp
            )
            
            // Faders in rows of 5
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 0..4) {
                    EQFader(
                        frequency = EqualizerPreset.BAND_FREQUENCIES[i],
                        value = bands[i],
                        onValueChange = { onBandChanged(i, it) },
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (i in 5..9) {
                    EQFader(
                        frequency = EqualizerPreset.BAND_FREQUENCIES[i],
                        value = bands[i],
                        onValueChange = { onBandChanged(i, it) },
                        themeColors = themeColors,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EQFader(
    frequency: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    themeColors: ThemeColors,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (value != 0f) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "faderScale"
    )
    
    Column(
        modifier = modifier
            .scale(scale)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Value display
        Text(
            "${value.toInt()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (value > 0) themeColors.accent1 else if (value < 0) themeColors.accent2 else themeColors.onSurface.copy(alpha = 0.5f)
        )
        
        // Vertical slider (simulated)
        Box(
            modifier = Modifier
                .height(120.dp)
                .width(40.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            themeColors.accent1.copy(alpha = 0.1f),
                            themeColors.accent2.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Using horizontal slider rotated
            Box(modifier = Modifier.rotate(-90f)) {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = -15f..15f,
                    modifier = Modifier.width(120.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = if (value > 0) themeColors.accent1 else if (value < 0) themeColors.accent2 else themeColors.onSurface,
                        activeTrackColor = if (value > 0) themeColors.accent1 else themeColors.accent2,
                        inactiveTrackColor = themeColors.onSurface.copy(alpha = 0.2f)
                    )
                )
            }
        }
        
        // Frequency label
        Text(
            frequency,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = themeColors.onSurface.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuickActionsRow(
    onFlatAll: () -> Unit,
    onBassBoost: () -> Unit,
    onTrebleBoost: () -> Unit,
    themeColors: ThemeColors
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            label = "FLAT",
            icon = Icons.Default.HorizontalRule,
            onClick = onFlatAll,
            themeColors = themeColors,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "BASS",
            icon = Icons.Default.MusicNote,
            onClick = onBassBoost,
            themeColors = themeColors,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = "TREBLE",
            icon = Icons.Default.GraphicEq,
            onClick = onTrebleBoost,
            themeColors = themeColors,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    themeColors: ThemeColors,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = themeColors.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

// Dialogs will be in next part due to size...

// Helper function to parse custom presets
private fun parseCustomPresets(json: String): List<EqualizerPreset> {
    // Simple JSON parsing - in production use Gson/Moshi
    return emptyList() // TODO: Implement JSON parsing
}

@Composable
private fun PresetSelectionDialog(
    presets: List<EqualizerPreset>,
    activePresetName: String,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeColors
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "SELECT PRESET",
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isActive = preset.name == activePresetName
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPresetSelected(preset) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) themeColors.accent1.copy(alpha = 0.2f) else themeColors.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                preset.name,
                                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                                color = if (isActive) themeColors.accent1 else themeColors.onSurface
                            )
                            if (isActive) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = themeColors.accent1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE")
            }
        }
    )
}

@Composable
private fun SavePresetDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeColors
) {
    var presetName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "SAVE CUSTOM PRESET",
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Enter a name for your custom EQ preset:",
                    fontSize = 14.sp
                )
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (presetName.isNotBlank()) {
                        onSave(presetName.trim())
                    }
                },
                enabled = presetName.isNotBlank()
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
private fun MaxGainDialog(
    currentMaxGain: Int,
    onMaxGainChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeColors
) {
    var tempMaxGain by remember { mutableStateOf(currentMaxGain) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "MAXIMUM GAIN",
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Set maximum amplification boost (15dB to 30dB):",
                    fontSize = 14.sp
                )
                Text(
                    "⚠️ Values above 20dB may cause distortion",
                    fontSize = 12.sp,
                    color = Color(0xFFFF9800)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("15 dB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$tempMaxGain dB",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColors.accent1
                    )
                    Text("30 dB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                Slider(
                    value = tempMaxGain.toFloat(),
                    onValueChange = { tempMaxGain = it.toInt() },
                    valueRange = 15f..30f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = themeColors.accent1,
                        activeTrackColor = themeColors.accent1
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onMaxGainChanged(tempMaxGain)
                onDismiss()
            }) {
                Text("APPLY")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
