package com.soundboost.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.soundboost.data.BoostPreferences
import com.soundboost.data.BoostSettings
import com.soundboost.data.SettingsBackupManager
import com.soundboost.ui.theme.getThemeColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Settings Backup & Restore Screen
 * Export/Import app settings as JSON
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBackupScreen(
    state: BoostSettings,
    prefs: BoostPreferences,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    
    var showMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                isLoading = true
                val result = SettingsBackupManager.exportSettings(context, it, state)
                isLoading = false
                showMessage = if (result.isSuccess) {
                    context.getString(R.string.backup_success_export)
                } else {
                    "${context.getString(R.string.backup_error_export)}: ${result.exceptionOrNull()?.message}"
                }
            }
        }
    }
    
    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                isLoading = true
                val result = SettingsBackupManager.importSettings(context, it)
                if (result.isSuccess) {
                    try {
                        SettingsBackupManager.applyImportedSettings(prefs, result.getOrThrow())
                        showMessage = context.getString(R.string.backup_success_import)
                    } catch (e: Exception) {
                        showMessage = "${context.getString(R.string.backup_error_import)}: ${e.message}"
                    }
                } else {
                    showMessage = "${context.getString(R.string.backup_error_import)}: ${result.exceptionOrNull()?.message}"
                }
                isLoading = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.backup_restore_title),
                        fontWeight = FontWeight.Bold
                    )
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
        snackbarHost = {
            showMessage?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(message)
                }
            }
        },
        containerColor = themeColors.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = themeColors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                stringResource(R.string.backup_export_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.onSurface
                            )
                            Text(
                                stringResource(R.string.backup_export_desc),
                                fontSize = 12.sp,
                                color = themeColors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Text(
                        stringResource(R.string.backup_info_desc),
                        fontSize = 13.sp,
                        color = themeColors.onSurface.copy(alpha = 0.8f)
                    )
                    
                    Button(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            exportLauncher.launch("SoundSTBoost_$timestamp.json")
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Upload, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_export_button))
                    }
                }
            }
            
            // Import Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = themeColors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                stringResource(R.string.backup_import_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.onSurface
                            )
                            Text(
                                stringResource(R.string.backup_import_desc),
                                fontSize = 12.sp,
                                color = themeColors.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    Text(
                        stringResource(R.string.backup_warning),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                    )
                    
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_import_button))
                    }
                }
            }
            
            // Info Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = themeColors.surface.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = themeColors.primary.copy(alpha = 0.7f)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.backup_info_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = themeColors.onSurface
                        )
                        Text(
                            stringResource(R.string.backup_info_desc),
                            fontSize = 12.sp,
                            color = themeColors.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
