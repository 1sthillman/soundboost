package com.soundboost.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soundboost.R
import com.soundboost.data.*
import com.soundboost.ui.theme.getThemeColors
import kotlinx.coroutines.launch

/**
 * Per-App Audio Profiles Screen
 * Different boost/EQ for different apps
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfilesScreen(
    state: BoostSettings,
    profileManager: AppProfileManager,
    onBack: () -> Unit,
    onConfigureProfile: (String) -> Unit  // Navigate to profile config
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    val profiles by profileManager.profiles.collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_profiles_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Load installed apps
                        scope.launch {
                            installedApps = profileManager.getInstalledApps()
                            showAddDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Add, "Add Profile")
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
            if (profiles.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = themeColors.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.app_profiles_empty),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.app_profiles_empty_desc),
                        fontSize = 14.sp,
                        color = themeColors.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = {
                        scope.launch {
                            installedApps = profileManager.getInstalledApps()
                            showAddDialog = true
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.app_profiles_add))
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profiles) { profile ->
                        AppProfileCard(
                            profile = profile,
                            themeColors = themeColors,
                            onConfigure = { onConfigureProfile(profile.packageName) },
                            onDelete = {
                                scope.launch {
                                    profileManager.deleteProfile(profile.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add Profile Dialog
    if (showAddDialog) {
        AppSelectionDialog(
            apps = installedApps,
            existingProfiles = profiles,
            themeColors = themeColors,
            onDismiss = { showAddDialog = false },
            onSelect = { appInfo ->
                // Create default profile for selected app
                scope.launch {
                    val newProfile = AppProfile(
                        packageName = appInfo.packageName,
                        appName = appInfo.appName,
                        masterGainPercent = state.masterGainPercent,
                        bassBoostPercent = state.bassBoostPercent,
                        virtualizerPercent = state.virtualizerPercent,
                        eq10Band = state.get10BandEQ().toList(),
                        presetName = state.activePresetName
                    )
                    profileManager.saveProfile(newProfile)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AppProfileCard(
    profile: AppProfile,
    themeColors: com.soundboost.ui.theme.ThemeColors,
    onConfigure: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConfigure() },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    profile.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Gain: ${profile.masterGainPercent}% • Bass: ${profile.bassBoostPercent}%",
                    fontSize = 12.sp,
                    color = themeColors.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    "Preset: ${profile.presetName}",
                    fontSize = 11.sp,
                    color = themeColors.primary.copy(alpha = 0.8f)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun AppSelectionDialog(
    apps: List<AppInfo>,
    existingProfiles: List<AppProfile>,
    themeColors: com.soundboost.ui.theme.ThemeColors,
    onDismiss: () -> Unit,
    onSelect: (AppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredApps = remember(apps, existingProfiles, searchQuery) {
        apps.filter { app ->
            existingProfiles.none { it.packageName == app.packageName }
        }.filter { app ->
            searchQuery.isEmpty() || app.appName.contains(searchQuery, ignoreCase = true) || 
            app.packageName.contains(searchQuery, ignoreCase = true)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                stringResource(R.string.app_profiles_select),
                color = themeColors.onSurface
            ) 
        },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.height(400.dp)
                ) {
                    items(filteredApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(app) }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            app.icon?.let { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                )
                            } ?: Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = themeColors.onSurface.copy(alpha = 0.6f)
                            )
                            Column {
                                Text(
                                    app.appName,
                                    color = themeColors.onSurface
                                )
                                Text(
                                    app.packageName,
                                    fontSize = 10.sp,
                                    color = themeColors.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.eq_cancel),
                    color = themeColors.onSurface
                )
            }
        },
        containerColor = themeColors.surface
    )
}
