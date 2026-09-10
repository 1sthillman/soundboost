package com.soundboost.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundboost.R
import com.soundboost.data.BoostSettings
import com.soundboost.ui.components.*
import com.soundboost.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: BoostSettings,
    onAutoStartToggled: (Boolean) -> Unit,
    onMaximizeVolume: () -> Unit,
    onOpenThemes: () -> Unit,
    onOpenHelp: () -> Unit,
    onRateApp: () -> Unit,
    onShareApp: () -> Unit,
    onDarkModeChanged: (Boolean?) -> Unit,
    onBack: () -> Unit
) {
    val themeColors = getThemeColors(state.theme, state.colorAccent)
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Check microphone permission status
    var micPermissionGranted by remember { mutableStateOf(
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    ) }
    
    // Re-check permission when screen resumes
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        micPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        onPauseOrDispose { }
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
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
            
            // Microphone Permission Section
            ModernSettingsCard(
                title = if (micPermissionGranted) 
                    stringResource(R.string.microphone_permission)
                else 
                    stringResource(R.string.microphone_permission_required),
                description = if (micPermissionGranted)
                    stringResource(R.string.microphone_permission_granted)
                else
                    stringResource(R.string.microphone_permission_desc),
                icon = if (micPermissionGranted) Icons.Default.Mic else Icons.Default.MicOff,
                accentColor = if (micPermissionGranted) 
                    androidx.compose.ui.graphics.Color(0xFF4CAF50) 
                else 
                    androidx.compose.ui.graphics.Color(0xFFFF9800),
                surfaceColor = themeColors.surfaceElevated,
                onClick = {
                    if (!micPermissionGranted) {
                        // Open app settings using intent
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    }
                },
                endContent = {
                    if (micPermissionGranted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        )
                    } else {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = themeColors.onSurfaceVariant
                        )
                    }
                }
            )
            
            // Theme Section
            ModernSettingsCard(
                title = stringResource(R.string.themes),
                description = stringResource(R.string.theme_customization),
                icon = Icons.Default.Palette,
                accentColor = themeColors.accent1,
                surfaceColor = themeColors.surfaceElevated,
                onClick = onOpenThemes
            )
            
            // Dark Mode Toggle
            val systemInDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
            val darkModeLabel = when (state.isDarkMode) {
                null -> stringResource(R.string.dark_mode_system)
                true -> stringResource(R.string.dark_mode_on)
                false -> stringResource(R.string.dark_mode_off)
            }
            val darkModeDesc = when (state.isDarkMode) {
                null -> stringResource(R.string.dark_mode_system_desc)
                true -> stringResource(R.string.dark_mode_on_desc)
                false -> stringResource(R.string.dark_mode_off_desc)
            }
            
            ModernSettingsCard(
                title = stringResource(R.string.dark_mode),
                description = "$darkModeLabel • $darkModeDesc",
                icon = if (state.isDarkMode ?: systemInDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                accentColor = themeColors.accent1,
                surfaceColor = themeColors.surfaceElevated,
                onClick = {
                    // Cycle: system → dark → light → system
                    val nextMode = when (state.isDarkMode) {
                        null -> true
                        true -> false
                        false -> null
                    }
                    onDarkModeChanged(nextMode)
                },
                endContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = themeColors.onSurfaceVariant
                    )
                }
            )
            
            // Auto Start with Modern Switch
            ModernSettingsCard(
                title = stringResource(R.string.auto_start),
                description = stringResource(R.string.auto_start_desc),
                icon = Icons.Default.PowerSettingsNew,
                accentColor = themeColors.accent1,
                surfaceColor = themeColors.surfaceElevated,
                onClick = { onAutoStartToggled(!state.autoStartOnBoot) },
                endContent = {
                    ModernSwitch(
                        checked = state.autoStartOnBoot,
                        onCheckedChange = onAutoStartToggled,
                        accentColor = themeColors.accent1
                    )
                }
            )
            
            // Maximize Volume Button
            ModernButton(
                text = stringResource(R.string.max_volume),
                icon = Icons.Default.VolumeUp,
                accentColor = themeColors.accent1,
                onClick = onMaximizeVolume,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Help & FAQ
            ModernSettingsCard(
                title = stringResource(R.string.help_and_faq),
                description = stringResource(R.string.help_desc),
                icon = Icons.Default.Help,
                accentColor = themeColors.accent2,
                surfaceColor = themeColors.surfaceElevated,
                onClick = onOpenHelp,
                endContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = themeColors.onSurfaceVariant
                    )
                }
            )
            
            // Rate App
            ModernSettingsCard(
                title = stringResource(R.string.rate_your_app),
                description = stringResource(R.string.rate_app_desc),
                icon = Icons.Default.Star,
                accentColor = androidx.compose.ui.graphics.Color(0xFFFFD700),
                surfaceColor = themeColors.surfaceElevated,
                onClick = onRateApp,
                endContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = themeColors.onSurfaceVariant
                    )
                }
            )
            
            // Share App
            ModernSettingsCard(
                title = stringResource(R.string.share_app),
                description = stringResource(R.string.share_app_desc),
                icon = Icons.Default.Share,
                accentColor = themeColors.accent1,
                surfaceColor = themeColors.surfaceElevated,
                onClick = onShareApp,
                endContent = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = themeColors.onSurfaceVariant
                    )
                }
            )
            
            // About Card
            ModernSettingsCard(
                title = stringResource(R.string.about),
                description = stringResource(R.string.about_desc),
                icon = Icons.Default.Info,
                accentColor = themeColors.accent2,
                surfaceColor = themeColors.surfaceElevated,
                onClick = {}
            )
            
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
