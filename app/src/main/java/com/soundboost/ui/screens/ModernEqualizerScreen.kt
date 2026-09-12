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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soundboost.R
import com.soundboost.audio.EqualizerPreset
import com.soundboost.data.BoostSettings
import com.soundboost.ui.components.ModernSlider
import com.soundboost.ui.theme.*
import kotlinx.coroutines.launch

/**
 * MODERN 10-BAND PARAMETRIC EQUALIZER
 * Clean Design • No Overflow • Optimized Performance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernEqualizerScreen(
    state: BoostSettings,
    onBandsChanged: (FloatArray) -> Unit,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onSaveCustomPreset: (String, FloatArray) -> Unit,
    onMaxGainChanged: (Int) -> Unit,
    onCallEnhancementToggled: (Boolean) -> Unit,
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
                            stringResource(R.string.eq_professional),
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        )
                        Text(
                            stringResource(R.string.eq_10band_parametric),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColors.onSurface.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.eq_back))
                    }
                },
                actions = {
                    // Max Gain Settings
                    IconButton(onClick = { showMaxGainDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.eq_max_gain))
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
            
            // Call Enhancement Toggle
            CallEnhancementCard(
                isEnabled = state.isCallEnhancementEnabled,
                onToggle = onCallEnhancementToggled,
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
            containerColor = themeColors.surface
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
                    stringResource(R.string.eq_active_preset),
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
                        color = themeColors.onSurface
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
                        .background(themeColors.accent1.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = stringResource(R.string.eq_select_preset),
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
                            if (isModified) themeColors.accent2.copy(alpha = 0.15f) else themeColors.surface,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = stringResource(R.string.eq_save_preset),
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
                            if (isModified) Color(0xFFFF5252).copy(alpha = 0.15f) else themeColors.surface,
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.eq_reset),
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
            containerColor = themeColors.surface
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
    // Local state for smooth dragging
    var localValue by remember(value) { mutableFloatStateOf(value) }
    var isDragging by remember { mutableStateOf(false) }
    
    // Update local when external changes (only when not dragging)
    LaunchedEffect(value) {
        if (!isDragging && kotlin.math.abs(value - localValue) > 0.1f) {
            localValue = value
        }
    }
    
    val scale by animateFloatAsState(
        targetValue = if (localValue != 0f) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
            "${localValue.toInt()}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = when {
                localValue > 0 -> themeColors.accent1
                localValue < 0 -> themeColors.accent2
                else -> themeColors.onSurface.copy(alpha = 0.5f)
            }
        )
        
        // Vertical slider (simulated)
        Box(
            modifier = Modifier
                .height(120.dp)
                .width(40.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            themeColors.accent1.copy(alpha = 0.15f),
                            themeColors.surface,
                            themeColors.accent2.copy(alpha = 0.15f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Using horizontal slider rotated
            Box(modifier = Modifier.rotate(-90f)) {
                Slider(
                    value = localValue,
                    onValueChange = {
                        isDragging = true
                        localValue = it
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onValueChange(localValue)
                    },
                    valueRange = -15f..15f,
                    modifier = Modifier.width(120.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = when {
                            localValue > 0 -> themeColors.accent1
                            localValue < 0 -> themeColors.accent2
                            else -> themeColors.onSurface
                        },
                        activeTrackColor = when {
                            localValue > 0 -> themeColors.accent1
                            localValue < 0 -> themeColors.accent2
                            else -> themeColors.onSurface.copy(alpha = 0.3f)
                        },
                        inactiveTrackColor = themeColors.onSurface.copy(alpha = 0.1f)
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
            label = stringResource(R.string.eq_flat_btn),
            icon = Icons.Default.HorizontalRule,
            onClick = onFlatAll,
            themeColors = themeColors,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = stringResource(R.string.eq_bass_btn),
            icon = Icons.Default.MusicNote,
            onClick = onBassBoost,
            themeColors = themeColors,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            label = stringResource(R.string.eq_treble_btn),
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

@Composable
private fun CallEnhancementCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    themeColors: ThemeColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                themeColors.accent1.copy(alpha = 0.15f)
            } else {
                themeColors.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = if (isEnabled) themeColors.accent1 else themeColors.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        stringResource(R.string.call_enhancement_title),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        color = if (isEnabled) themeColors.accent1 else themeColors.onSurface
                    )
                }
                
                Text(
                    stringResource(R.string.call_enhancement_desc),
                    fontSize = 11.sp,
                    color = themeColors.onSurface.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
                
                if (isEnabled) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = themeColors.accent1.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "✓ ${stringResource(R.string.call_enhancement_noise_suppression)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = themeColors.accent2.copy(alpha = 0.2f)
                        ) {
                            Text(
                                "✓ ${stringResource(R.string.call_enhancement_auto_gain)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.accent2,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Toggle Switch
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = themeColors.accent1,
                    checkedTrackColor = themeColors.accent1.copy(alpha = 0.3f),
                    uncheckedThumbColor = themeColors.onSurface.copy(alpha = 0.3f),
                    uncheckedTrackColor = themeColors.onSurface.copy(alpha = 0.1f)
                )
            )
        }
    }
}

// Dialogs will be in next part due to size...

// Helper function to parse custom presets
private fun parseCustomPresets(json: String): List<EqualizerPreset> {
    if (json.isEmpty() || json == "[]") return emptyList()
    
    return try {
        val jsonArray = org.json.JSONArray(json)
        (0 until jsonArray.length()).mapNotNull { i ->
            try {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.getString("name")
                val bandsArray = obj.getJSONArray("bands")
                val bands = FloatArray(10) { idx ->
                    bandsArray.getDouble(idx).toFloat()
                }
                EqualizerPreset.fromBandArray(name, name, bands, isCustom = true)
            } catch (e: Exception) {
                null
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
private fun PresetSelectionDialog(
    presets: List<EqualizerPreset>,
    activePresetName: String,
    onPresetSelected: (EqualizerPreset) -> Unit,
    onDismiss: () -> Unit,
    themeColors: ThemeColors
) {
    val context = LocalContext.current
    
    // Separate custom and built-in presets
    val customPresets = presets.filter { it.isCustom }
    val builtInPresets = presets.filter { !it.isCustom }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = themeColors.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            stringResource(R.string.equalizer).uppercase(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColors.onSurface
                        )
                        Text(
                            "${presets.size} ${stringResource(R.string.settings)}",
                            fontSize = 12.sp,
                            color = themeColors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .background(themeColors.surface, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, stringResource(R.string.eq_close), tint = themeColors.onSurface)
                    }
                }
                
                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Custom Presets (if any)
                    if (customPresets.isNotEmpty()) {
                        PresetGridSection(
                            title = "MY PRESETS",
                            presets = customPresets,
                            activePresetName = activePresetName,
                            onPresetSelected = onPresetSelected,
                            themeColors = themeColors
                        )
                    }
                    
                    // Built-in Genre Presets
                    PresetGridSection(
                        title = "GENRE",
                        presets = builtInPresets.filter {
                            it.name in listOf("Rock", "Pop", "Jazz", "Classical", "Electronic",
                                "Hip-Hop", "R&B", "Heavy Metal", "Country", "Latin", "Blues", "Reggae", "Dance")
                        },
                        activePresetName = activePresetName,
                        onPresetSelected = onPresetSelected,
                        themeColors = themeColors
                    )
                    
                    // Bass & Treble
                    PresetGridSection(
                        title = "BASS & TREBLE",
                        presets = builtInPresets.filter {
                            it.name in listOf("Bass Boost", "Treble Boost", "Deep Bass")
                        },
                        activePresetName = activePresetName,
                        onPresetSelected = onPresetSelected,
                        themeColors = themeColors
                    )
                    
                    // Vocal & Acoustic
                    PresetGridSection(
                        title = "VOCAL & ACOUSTIC",
                        presets = builtInPresets.filter {
                            it.name in listOf("Vocal Boost", "Acoustic", "Piano")
                        },
                        activePresetName = activePresetName,
                        onPresetSelected = onPresetSelected,
                        themeColors = themeColors
                    )
                    
                    // Other
                    PresetGridSection(
                        title = "OTHER",
                        presets = builtInPresets.filter {
                            it.name in listOf("Flat", "Headphones", "Speakers", "Small Room", "Medium Room", "Large Hall", "Live", "Vinyl", "Soft")
                        },
                        activePresetName = activePresetName,
                        onPresetSelected = onPresetSelected,
                        themeColors = themeColors
                    )
                    
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun PresetGridSection(
    title: String,
    presets: List<EqualizerPreset>,
    activePresetName: String,
    onPresetSelected: (EqualizerPreset) -> Unit,
    themeColors: ThemeColors
) {
    if (presets.isEmpty()) return
    
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val columns = if (configuration.screenWidthDp >= 600) 4 else 3
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = themeColors.onSurface.copy(alpha = 0.7f),
            letterSpacing = 1.sp
        )
        
        // Calculate rows
        val rows = (presets.size + columns - 1) / columns
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(rows) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(columns) { colIndex ->
                        val index = rowIndex * columns + colIndex
                        if (index < presets.size) {
                            val preset = presets[index]
                            val name = remember(preset.nameKey) {
                                try {
                                    context.resources.getIdentifier(
                                        preset.nameKey,
                                        "string",
                                        context.packageName
                                    ).let { resId ->
                                        if (resId != 0) context.getString(resId) else preset.name
                                    }
                                } catch (e: Exception) {
                                    preset.name
                                }
                            }
                            
                            ModernPresetCard(
                                name = name,
                                isActive = preset.name == activePresetName,
                                onClick = { onPresetSelected(preset) },
                                themeColors = themeColors,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernPresetCard(
    name: String,
    isActive: Boolean,
    onClick: () -> Unit,
    themeColors: ThemeColors,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                themeColors.accent1.copy(alpha = 0.2f)
            } else {
                themeColors.surface
            }
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isActive) {
                        themeColors.accent1.copy(alpha = 0.2f)
                    } else {
                        themeColors.surface
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint = if (isActive) themeColors.accent1 else themeColors.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Text(
                    name,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) themeColors.accent1 else themeColors.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
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
                stringResource(R.string.eq_save_preset).uppercase(),
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
                    label = { Text(stringResource(R.string.eq_preset_name)) },
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
                Text(stringResource(R.string.eq_save).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.eq_cancel).uppercase())
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
                stringResource(R.string.eq_max_gain_settings).uppercase(),
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.eq_max_gain_desc),
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
                Text(stringResource(R.string.eq_apply).uppercase())
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.eq_cancel).uppercase())
            }
        }
    )
}
