package com.soundboost.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.data.*
import com.soundboost.ui.theme.getThemeColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bluetooth Device Profiles Screen
 * Auto-switch audio settings per Bluetooth device
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothProfilesScreen(
    state: BoostSettings,
    profileManager: BluetoothProfileManager,
    onBack: () -> Unit,
    onConfigureProfile: (String) -> Unit  // Navigate to profile config
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    val profiles by profileManager.profiles.collectAsState(initial = emptyList())
    
    // Sort by last used (most recent first)
    val sortedProfiles = remember(profiles) {
        profiles.sortedByDescending { it.lastUsed }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.bluetooth_profiles_title),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.bluetooth_profiles_desc),
                            fontSize = 10.sp,
                            color = themeColors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
        ) {
            if (sortedProfiles.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = themeColors.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.bluetooth_profiles_empty),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.bluetooth_profiles_empty_desc),
                        fontSize = 14.sp,
                        color = themeColors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = themeColors.surface.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = themeColors.primary
                                )
                                Text(
                                    stringResource(R.string.bluetooth_profiles_how_title),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = themeColors.onSurface
                                )
                            }
                            Text(
                                stringResource(R.string.bluetooth_profiles_how_desc),
                                fontSize = 12.sp,
                                color = themeColors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedProfiles) { profile ->
                        BluetoothProfileCard(
                            profile = profile,
                            themeColors = themeColors,
                            onConfigure = { onConfigureProfile(profile.deviceAddress) },
                            onDelete = {
                                scope.launch {
                                    profileManager.deleteProfile(profile.deviceAddress)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothProfileCard(
    profile: BluetoothDeviceProfile,
    themeColors: com.soundboost.ui.theme.ThemeColors,
    onConfigure: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }
    val lastUsedText = remember(profile.lastUsed) {
        dateFormat.format(Date(profile.lastUsed))
    }
    
    val deviceIcon = when (profile.deviceType) {
        "HEADSET" -> Icons.Default.Headphones
        "SPEAKER" -> Icons.Default.Speaker
        "CAR" -> Icons.Default.DirectionsCar
        else -> Icons.Default.Bluetooth
    }
    
    Card(
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
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    deviceIcon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = themeColors.primary
                )
                Column {
                    Text(
                        profile.deviceName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Gain: ${profile.masterGainPercent}% • Bass: ${profile.bassBoostPercent}%",
                        fontSize = 12.sp,
                        color = themeColors.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        stringResource(R.string.bluetooth_profiles_last_used, lastUsedText),
                        fontSize = 10.sp,
                        color = themeColors.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onConfigure) {
                    Icon(Icons.Default.Edit, stringResource(R.string.app_profiles_edit), tint = themeColors.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, stringResource(R.string.app_profiles_delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
