package com.soundboost

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soundboost.ui.screens.*
import com.soundboost.ui.screens.ModernEqualizerScreen
import com.soundboost.ui.components.BatteryOnboardingCard
import com.soundboost.ui.theme.SoundSTBoostTheme
import com.soundboost.ui.theme.getThemeColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val syncViewModel: SyncViewModel by viewModels()
    
    // CRITICAL: Track if we should show prominent disclosure
    internal var shouldShowAudioDisclosure = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* İzin reddedilirse sadece bildirim gösterilmez */ }

    internal val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Mikrofon izni verildi - Boost başlatılıyor")
            // İzin verildiyse boost'u başlat
            viewModel.toggleBoost()
        } else {
            android.util.Log.w("MainActivity", "❌ Mikrofon izni reddedildi - Görselleştirme çalışmayacak")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize File Logger for debugging without ADB
        com.soundboost.debug.FileLogger.init(this)
        com.soundboost.debug.FileLogger.log("MainActivity", "🚀 App Started - onCreate called")
        
        // CRITICAL: Share BassFlashlightSync instance between ViewModels
        // This prevents camera resource conflicts (TWO instances trying to control same camera)
        syncViewModel.setBassFlashSync(viewModel.getBassFlashSyncInstance())
        android.util.Log.d("MainActivity", "🔗 Shared BassFlashlightSync instance with SyncViewModel")
        
        // CRITICAL: Connect audio analysis flow from MainViewModel to SyncViewModel
        // This enables bass-sync flash in party mode
        syncViewModel.setAudioAnalysisFlow(viewModel.audioAnalysis)
        android.util.Log.d("MainActivity", "🔗 Connected audio analysis flow to SyncViewModel")
        
        // CRITICAL: Apply saved language BEFORE setting content
        // This ensures proper system language detection and immediate effect
        com.soundboost.data.LanguageManager.applyLanguage(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
        }
        
        // CRITICAL: Check if we need to show audio permission disclosure on first launch
        // GOOGLE PLAY POLICY: Prominent disclosure MUST be shown before requesting RECORD_AUDIO
        val sp = getSharedPreferences("app_prefs", MODE_PRIVATE)
        shouldShowAudioDisclosure = !sp.getBoolean("audio_disclosure_shown", false)
        
        // Handle flash toggle from notification AND shortcuts
        handleIntent(intent)
        
        // Handle deep links (soundboost://action/...)
        intent?.data?.let { uri ->
            if (uri.scheme == "soundboost" && uri.host == "action") {
                val action = uri.pathSegments.firstOrNull()
                action?.let {
                    android.util.Log.d("MainActivity", "🔗 Deep Link: $it")
                    handleShortcutAction(it)
                }
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val systemInDarkTheme = isSystemInDarkTheme()
            
            // Determine effective dark mode
            val effectiveDarkMode = uiState.isDarkMode ?: systemInDarkTheme
            
            SoundSTBoostTheme(darkTheme = effectiveDarkMode) {
                val view = LocalView.current
                if (!view.isInEditMode) {
                    val themeColors = getThemeColors(uiState.theme, uiState.colorAccent)
                    
                    // Set system bars colors to match theme
                    LaunchedEffect(themeColors, effectiveDarkMode) {
                        val window = (view.context as ComponentActivity).window
                        
                        // Make system bars edge-to-edge
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        
                        // Set system bar colors - Android 15+ compatible
                        if (Build.VERSION.SDK_INT >= 35) {
                            // Android 15+ (API 35): Use modern edge-to-edge approach
                            // System handles colors automatically, we just set transparency
                            @Suppress("DEPRECATION")
                            window.statusBarColor = android.graphics.Color.TRANSPARENT
                            @Suppress("DEPRECATION")
                            window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        } else {
                            // Android 14 and below: Use traditional approach
                            @Suppress("DEPRECATION")
                            window.statusBarColor = android.graphics.Color.TRANSPARENT
                            @Suppress("DEPRECATION")
                            window.navigationBarColor = themeColors.background.toArgb()
                        }
                        
                        // Icon colors - dark icons on light background, light icons on dark
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !effectiveDarkMode
                            isAppearanceLightNavigationBars = !effectiveDarkMode
                        }
                    }
                }
                
                MainScreen(viewModel = viewModel, syncViewModel = syncViewModel)
            }
        }
    }
    
    fun checkAndRequestMicrophonePermission() {
        when {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                // Already granted - start boost immediately
                android.util.Log.d("MainActivity", "✅ Mikrofon izni zaten var - Boost başlatılıyor")
                viewModel.toggleBoost()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                // Show rationale and request
                android.util.Log.d("MainActivity", "ℹ️ Mikrofon izni açıklaması gösteriliyor")
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                // First time - request directly
                android.util.Log.d("MainActivity", "🎤 Mikrofon izni isteniyor")
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    fun openAppSettings() {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }
    
    override fun attachBaseContext(newBase: Context) {
        // CRITICAL: Apply language to base context for proper system integration
        // This ensures RTL support and system locale detection work correctly
        com.soundboost.data.LanguageManager.applyLanguage(newBase)
        super.attachBaseContext(newBase)
    }
    
    private fun handleIntent(intent: Intent?) {
        com.soundboost.debug.FileLogger.log("MainActivity", "🔍 handleIntent called - Intent: $intent")
        android.util.Log.d("MainActivity", "🔍 handleIntent called - Intent: $intent")
        
        intent?.let {
            // Log all intent data for debugging
            com.soundboost.debug.FileLogger.log("MainActivity", "📦 Intent Action: ${it.action}")
            com.soundboost.debug.FileLogger.log("MainActivity", "📦 Intent Data: ${it.data}")
            com.soundboost.debug.FileLogger.log("MainActivity", "📦 Intent Extras: ${it.extras?.keySet()?.joinToString()}")
            
            android.util.Log.d("MainActivity", "📦 Intent Action: ${it.action}")
            android.util.Log.d("MainActivity", "📦 Intent Data: ${it.data}")
            android.util.Log.d("MainActivity", "📦 Intent Extras: ${it.extras?.keySet()?.joinToString()}")
            
            it.extras?.keySet()?.forEach { key ->
                val value = it.extras?.get(key)
                com.soundboost.debug.FileLogger.log("MainActivity", "  📌 $key = $value")
                android.util.Log.d("MainActivity", "  📌 $key = $value")
            }
            
            // Handle notification actions
            if (it.getBooleanExtra("TOGGLE_FLASH", false)) {
                com.soundboost.debug.FileLogger.log("MainActivity", "⚡ Toggling flash from notification")
                android.util.Log.d("MainActivity", "⚡ Toggling flash from notification")
                val currentState = viewModel.isFlashEnabled.value
                viewModel.onFlashToggled(!currentState)
                return
            }
            
            // GOOGLE ASSISTANT SHORTCUTS - Handle all voice commands
            val shortcutAction = it.getStringExtra("shortcut_action")
            com.soundboost.debug.FileLogger.log("MainActivity", "🎤 Shortcut Action String: '$shortcutAction'")
            android.util.Log.d("MainActivity", "🎤 Shortcut Action String: '$shortcutAction'")
            
            if (shortcutAction != null) {
                com.soundboost.debug.FileLogger.log("MainActivity", "✅ Google Assistant Shortcut Detected: $shortcutAction")
                android.util.Log.d("MainActivity", "✅ Google Assistant Shortcut Detected: $shortcutAction")
                try {
                    handleShortcutAction(shortcutAction)
                    com.soundboost.debug.FileLogger.log("MainActivity", "✅ Shortcut handled successfully")
                    android.util.Log.d("MainActivity", "✅ Shortcut handled successfully")
                } catch (e: Exception) {
                    com.soundboost.debug.FileLogger.log("MainActivity", "❌ Error handling shortcut: ${e.message}")
                    android.util.Log.e("MainActivity", "❌ Error handling shortcut: ${e.message}", e)
                    showShortcutToast("❌ Error: ${e.message}")
                }
            } else {
                com.soundboost.debug.FileLogger.log("MainActivity", "⚠️ No shortcut_action found in intent")
                android.util.Log.w("MainActivity", "⚠️ No shortcut_action found in intent")
            }
        } ?: run {
            com.soundboost.debug.FileLogger.log("MainActivity", "⚠️ Intent is null")
            android.util.Log.d("MainActivity", "⚠️ Intent is null")
        }
    }
    
    /**
     * GOOGLE ASSISTANT SHORTCUTS HANDLER
     * Handles all voice commands from Google Assistant
     */
    private fun handleShortcutAction(action: String) {
        when (action) {
            // VOLUME SHORTCUTS
            "set_volume_max" -> {
                viewModel.onMasterGainChanged(100)
                showShortcutToast("🔊 Volume set to 100%")
            }
            "set_volume_75" -> {
                viewModel.onMasterGainChanged(75)
                showShortcutToast("🔊 Volume set to 75%")
            }
            "set_volume_50" -> {
                viewModel.onMasterGainChanged(50)
                showShortcutToast("🔊 Volume set to 50%")
            }
            "increase_volume" -> {
                val currentVolume = viewModel.uiState.value.masterGainPercent
                val newVolume = (currentVolume + 10).coerceIn(0, 200)
                viewModel.onMasterGainChanged(newVolume)
                showShortcutToast("🔊 Volume increased to $newVolume%")
            }
            "decrease_volume" -> {
                val currentVolume = viewModel.uiState.value.masterGainPercent
                val newVolume = (currentVolume - 10).coerceIn(0, 200)
                viewModel.onMasterGainChanged(newVolume)
                showShortcutToast("🔉 Volume decreased to $newVolume%")
            }
            
            // BASS SHORTCUTS
            "increase_bass" -> {
                val currentBass = viewModel.uiState.value.bassBoostPercent
                val newBass = (currentBass + 10).coerceIn(0, 100)
                viewModel.onBassBoostChanged(newBass)
                showShortcutToast("🎵 Bass increased to $newBass%")
            }
            "decrease_bass" -> {
                val currentBass = viewModel.uiState.value.bassBoostPercent
                val newBass = (currentBass - 10).coerceIn(0, 100)
                viewModel.onBassBoostChanged(newBass)
                showShortcutToast("🎵 Bass decreased to $newBass%")
            }
            "set_bass_max" -> {
                viewModel.onBassBoostChanged(100)
                showShortcutToast("🎵 Bass set to MAXIMUM (100%)")
            }
            "set_bass_zero" -> {
                viewModel.onBassBoostChanged(0)
                showShortcutToast("🎵 Bass turned OFF (0%)")
            }
            
            // TREBLE SHORTCUTS (via 10-band EQ)
            "increase_treble" -> {
                adjustTreble(10)
                showShortcutToast("🎼 Treble increased")
            }
            "decrease_treble" -> {
                adjustTreble(-10)
                showShortcutToast("🎼 Treble decreased")
            }
            
            // FLASH SYNC SHORTCUT
            "toggle_flash" -> {
                val currentState = viewModel.isFlashEnabled.value
                viewModel.onFlashToggled(!currentState)
                val status = if (!currentState) "ON" else "OFF"
                showShortcutToast("⚡ Flash sync $status")
            }
            "flash_on" -> {
                viewModel.onFlashToggled(true)
                showShortcutToast("⚡ Flash sync turned ON")
            }
            "flash_off" -> {
                viewModel.onFlashToggled(false)
                showShortcutToast("⚡ Flash sync turned OFF")
            }
            
            // SERVICE CONTROL
            "start_service", "boost_on" -> {
                if (!viewModel.uiState.value.isBoostEnabled) {
                    checkAndRequestMicrophonePermission()
                    showShortcutToast("🎧 Boost service starting...")
                } else {
                    showShortcutToast("🎧 Boost service already running")
                }
            }
            "stop_service", "boost_off" -> {
                if (viewModel.uiState.value.isBoostEnabled) {
                    viewModel.toggleBoost()
                    showShortcutToast("🎧 Boost service stopped")
                } else {
                    showShortcutToast("🎧 Boost service already stopped")
                }
            }
            
            // CALL ENHANCEMENT CONTROL
            "call_enhancement_on" -> {
                viewModel.onCallEnhancementToggled(true)
                showShortcutToast("📞 Call enhancement turned ON")
            }
            "call_enhancement_off" -> {
                viewModel.onCallEnhancementToggled(false)
                showShortcutToast("📞 Call enhancement turned OFF")
            }
            "toggle_call_enhancement" -> {
                val currentState = viewModel.uiState.value.isCallEnhancementEnabled
                viewModel.onCallEnhancementToggled(!currentState)
                val status = if (!currentState) "ON" else "OFF"
                showShortcutToast("📞 Call enhancement $status")
            }
            
            // BOOST TOGGLE SHORTCUT
            "toggle_boost" -> {
                val currentState = viewModel.uiState.value.isBoostEnabled
                if (!currentState) {
                    // Turning ON - check microphone permission first
                    checkAndRequestMicrophonePermission()
                    showShortcutToast("🎧 Boost activating...")
                } else {
                    // Turning OFF
                    viewModel.toggleBoost()
                    showShortcutToast("🎧 Boost OFF")
                }
            }
            
            // PRESET SHORTCUTS
            "preset_flat" -> {
                viewModel.onPresetSelected(com.soundboost.audio.EqualizerPreset.FLAT)
                showShortcutToast("🎚️ Flat preset applied")
            }
            "preset_bass" -> {
                viewModel.onPresetSelected(com.soundboost.audio.EqualizerPreset.BASS_BOOST)
                showShortcutToast("🎚️ Bass boost preset applied")
            }
            "preset_treble" -> {
                viewModel.onPresetSelected(com.soundboost.audio.EqualizerPreset.TREBLE_BOOST)
                showShortcutToast("🎚️ Treble boost preset applied")
            }
            
            else -> {
                android.util.Log.w("MainActivity", "❓ Unknown shortcut action: $action")
            }
        }
    }
    
    /**
     * Adjust treble by modifying high-frequency bands (8kHz, 16kHz)
     */
    private fun adjustTreble(delta: Int) {
        val currentState = viewModel.uiState.value
        // Adjust high-frequency bands: 8kHz and 16kHz
        val new8kHz = (currentState.eq8kHz + delta).coerceIn(-15f, 15f)
        val new16kHz = (currentState.eq16kHz + delta).coerceIn(-15f, 15f)
        
        // Build updated bands array
        val updatedBands = floatArrayOf(
            currentState.eq31Hz,
            currentState.eq62Hz,
            currentState.eq125Hz,
            currentState.eq250Hz,
            currentState.eq500Hz,
            currentState.eq1kHz,
            currentState.eq2kHz,
            currentState.eq4kHz,
            new8kHz,
            new16kHz
        )
        viewModel.on10BandEqChanged(updatedBands)
    }
    
    /**
     * Show feedback toast for shortcut actions
     */
    private fun showShortcutToast(message: String) {
        android.widget.Toast.makeText(
            this,
            message,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        
        // Handle deep links
        intent.data?.let { uri ->
            if (uri.scheme == "soundboost" && uri.host == "action") {
                val action = uri.pathSegments.firstOrNull()
                action?.let {
                    android.util.Log.d("MainActivity", "🔗 Deep Link (onNewIntent): $it")
                    handleShortcutAction(it)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, syncViewModel: SyncViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val audioLevels by viewModel.audioLevels.collectAsState()
    val themeColors = getThemeColors(uiState.theme, uiState.colorAccent)
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? MainActivity
    
    // CRITICAL: Track language as state to trigger immediate WebView updates
    var currentLanguage by remember { mutableStateOf(viewModel.getCurrentLanguage(context)) }
    
    var showRateDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    
    // GOOGLE PLAY POLICY: Show prominent disclosure before requesting RECORD_AUDIO
    var showAudioDisclosure by remember { mutableStateOf(activity?.shouldShowAudioDisclosure ?: false) }
    
    // NEW: Battery Onboarding Card - İlk açılışta göster
    var showBatteryOnboarding by remember { mutableStateOf(false) }
    
    // Check if we should show battery onboarding - ASYNC to avoid blocking UI
    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = com.soundboost.data.BoostPreferences(context)
            val shouldShow = prefs.shouldShowBatteryOnboarding()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                showBatteryOnboarding = shouldShow
                android.util.Log.d("MainActivity", "🔋 Should show battery onboarding: $shouldShow")
            }
        }
    }
    
    // Show rate dialog only after user has tried the boost feature
    LaunchedEffect(uiState.isBoostEnabled) {
        if (uiState.isBoostEnabled) {
            // User just enabled boost - wait a bit then show rate dialog
            kotlinx.coroutines.delay(5000)  // 5 saniye boost'u deneseler
            val prefs = com.soundboost.data.BoostPreferences(context)
            val shouldShow = prefs.shouldShowRateDialog()
            android.util.Log.d("MainActivity", "🌟 User tried boost, should show rate: $shouldShow")
            if (shouldShow) {
                showRateDialog = true
            }
        }
    }
    
    android.util.Log.d("MainActivity", "🔵 MainScreen recomposed")
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = themeColors.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = "volume") {
            composable(
                route = "volume",
                content = {
                    // Ana WebView ekranı - doğrudan açılır, hiçbir onboarding yok
                    com.soundboost.ui.components.WebViewHomeScreenPersistent(
                        state = uiState,
                        audioLevels = audioLevels,
                        audioAnalysis = viewModel.audioAnalysis,
                        onVolumeChange = viewModel::onMasterGainChanged,
                        onSensitivityChange = viewModel::onSensitivityChanged,
                        onToggleBoost = {
                            // Check if boost is being turned ON
                            if (!uiState.isBoostEnabled) {
                                // Request permission before starting
                                (context as? MainActivity)?.checkAndRequestMicrophonePermission()
                            } else {
                                // Turning OFF - no permission needed
                                viewModel.toggleBoost()
                            }
                        },
                        onThemeChanged = viewModel::onThemeChanged,
                        onModeChanged = viewModel::onDarkModeChanged,
                        onNavigateToSettings = { 
                            navController.navigate("settings")
                        },
                        onNavigateToEqualizer = { 
                            navController.navigate("equalizer")
                        },
                        onNavigateToLanguage = { 
                            navController.navigate("language")
                        },
                        currentLanguage = currentLanguage
                    )
                }
            )
            
            composable("equalizer") {
                // NEW: 2026 DJ-Style Modern Equalizer
                val isFlashEnabled by viewModel.isFlashEnabled.collectAsState()
                val flashIntensity by viewModel.flashIntensity.collectAsState()
                
                ModernEqualizerScreen(
                    state = uiState,
                    onBandsChanged = viewModel::on10BandEqChanged,
                    onPresetSelected = viewModel::onPresetSelected,
                    onSaveCustomPreset = viewModel::onSaveCustomPreset,
                    onMaxGainChanged = viewModel::onMaxGainChanged,
                    onCallEnhancementToggled = viewModel::onCallEnhancementToggled,
                    onFlashToggled = viewModel::onFlashToggled,
                    onFlashIntensityChanged = viewModel::onFlashIntensityChanged,
                    isFlashEnabled = isFlashEnabled,
                    flashIntensity = flashIntensity,
                    hasFlashSupport = viewModel.hasFlashSupport(),
                    onBack = { 
                        navController.popBackStack()
                    }
                )
            }
            
            composable("ai_vocal_separation") {
                AIVocalSeparationScreen(
                    state = uiState,
                    onBack = { 
                        navController.popBackStack()
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    state = uiState,
                    onAutoStartToggled = viewModel::onAutoStartToggled,
                    onMaximizeVolume = viewModel::maximizeSystemVolume,
                    onOpenThemes = { navController.navigate("themes") },
                    onOpenHelp = { navController.navigate("help") },
                    onRateApp = { showRateDialog = true },
                    onShareApp = { showShareDialog = true },
                    onDarkModeChanged = viewModel::onDarkModeChanged,
                    onOpenBackup = { navController.navigate("settings_backup") },
                    onOpenAppProfiles = { navController.navigate("app_profiles") },
                    onOpenBluetoothProfiles = { navController.navigate("bluetooth_profiles") },
                    onOpenPartyMode = { navController.navigate("party_mode_room") },
                    onBack = { 
                        navController.popBackStack()
                    }
                )
            }
            
            composable("settings_backup") {
                com.soundboost.ui.screens.SettingsBackupScreen(
                    state = uiState,
                    prefs = com.soundboost.data.BoostPreferences(context),
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("app_profiles") {
                com.soundboost.ui.screens.AppProfilesScreen(
                    state = uiState,
                    profileManager = viewModel.appProfileManager,
                    onBack = {
                        navController.popBackStack()
                    },
                    onConfigureProfile = { packageName ->
                        // TODO: Navigate to profile config screen
                        android.util.Log.d("MainActivity", "Configure profile for: $packageName")
                    }
                )
            }
            
            composable("bluetooth_profiles") {
                com.soundboost.ui.screens.BluetoothProfilesScreen(
                    state = uiState,
                    profileManager = viewModel.bluetoothProfileManager,
                    onBack = {
                        navController.popBackStack()
                    },
                    onConfigureProfile = { deviceAddress ->
                        // TODO: Navigate to profile config screen
                        android.util.Log.d("MainActivity", "Configure Bluetooth profile for: $deviceAddress")
                    }
                )
            }
            
            composable("party_mode_room") {
                val syncViewModel: SyncViewModel by activity?.viewModels() ?: return@composable
                SyncRoomScreen(
                    viewModel = syncViewModel,
                    onRoomReady = {
                        navController.navigate("party_mode_control")
                    }
                )
            }
            
            composable("party_mode_control") {
                val syncViewModel: SyncViewModel by activity?.viewModels() ?: return@composable
                FlashControlScreen(
                    viewModel = syncViewModel,
                    onBack = {
                        navController.popBackStack()
                        navController.popBackStack() // Go back to settings, not room screen
                    }
                )
            }
            
            composable("themes") {
                ThemeScreen(
                    state = uiState,
                    onThemeChanged = viewModel::onThemeChanged,
                    onColorAccentChanged = viewModel::onColorAccentChanged,
                    onBack = { 
                        navController.popBackStack()
                    }
                )
            }
            
            composable("language") {
                LanguageScreen(
                    state = uiState,
                    currentLanguage = currentLanguage,
                    onLanguageSelected = { language ->
                        android.util.Log.d("MainActivity", "🌐 Language selected: ${language.code}")
                        currentLanguage = language
                        viewModel.onLanguageChanged(language, context)
                    },
                    onBack = { 
                        navController.popBackStack()
                    }
                )
            }
            
            composable("help") {
                HelpScreen(
                    state = uiState,
                    onBack = { 
                        navController.popBackStack()
                    }
                )
            }
        }
        
        // NEW: Battery Onboarding Card - Modern overlay with scrim
        AnimatedVisibility(
            visible = showBatteryOnboarding,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            // Semi-transparent background scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Dismiss when clicking outside
                        CoroutineScope(Dispatchers.Main).launch {
                            val prefs = com.soundboost.data.BoostPreferences(context)
                            prefs.setBatteryOnboardingShown()
                            showBatteryOnboarding = false
                        }
                    }
            ) {
                // Card positioned in bottom area but with safe margins
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 16.dp) // Safe margin from bottom
                        .navigationBarsPadding()
                ) {
                    BatteryOnboardingCard(
                        themeColors = themeColors,
                        isVisible = showBatteryOnboarding,
                        onOpenSettings = {
                            // Direkt pil optimizasyon ayarlarını aç
                            try {
                                val intent = android.content.Intent(
                                    android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Failed to open battery settings", e)
                            }
                            // Onboarding'i dismiss et
                            CoroutineScope(Dispatchers.Main).launch {
                                val prefs = com.soundboost.data.BoostPreferences(context)
                                prefs.setBatteryOnboardingShown()
                                showBatteryOnboarding = false
                            }
                        },
                        onDismiss = {
                            CoroutineScope(Dispatchers.Main).launch {
                                val prefs = com.soundboost.data.BoostPreferences(context)
                                prefs.setBatteryOnboardingShown()
                                showBatteryOnboarding = false
                                android.util.Log.d("MainActivity", "✅ Battery onboarding dismissed")
                            }
                        }
                    )
                }
            }
        }
        
        // Dialogs
        // Dialogs
        if (showRateDialog) {
            com.soundboost.ui.components.RateAppDialog(
                themeColors = themeColors,
                onDismiss = { 
                    // "Daha sonra" butonuna basıldı - bir daha gösterme
                    viewModel.onRateLater()
                    showRateDialog = false 
                },
                onRated = { 
                    // "Değerlendir" butonuna basıp Play Store'a gitti - bir daha gösterme
                    viewModel.onAppRated()
                    showRateDialog = false
                }
            )
        }
        
        if (showShareDialog) {
            com.soundboost.ui.components.ShareDialog(
                themeColors = themeColors,
                onDismiss = { showShareDialog = false }
            )
        }
        
        // GOOGLE PLAY POLICY: Prominent Disclosure for RECORD_AUDIO permission
        // MUST be shown BEFORE requesting the permission
        if (showAudioDisclosure) {
            com.soundboost.ui.components.AudioPermissionDisclosureDialog(
                onAccept = {
                    android.util.Log.d("MainActivity", "✅ Kullanıcı ses izni açıklamasını kabul etti")
                    showAudioDisclosure = false
                    // Mark as shown
                    activity?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        ?.edit()
                        ?.putBoolean("audio_disclosure_shown", true)
                        ?.apply()
                    // Now request the actual Android permission
                    activity?.microphonePermissionLauncher?.launch(Manifest.permission.RECORD_AUDIO)
                },
                onDeny = {
                    android.util.Log.w("MainActivity", "❌ Kullanıcı ses izni açıklamasını reddetti")
                    showAudioDisclosure = false
                    // Mark as shown but don't request permission
                    activity?.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                        ?.edit()
                        ?.putBoolean("audio_disclosure_shown", true)
                        ?.apply()
                }
            )
        }
        }
    }
}
