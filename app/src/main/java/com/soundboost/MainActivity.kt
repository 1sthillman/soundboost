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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soundboost.ui.screens.*
import com.soundboost.ui.theme.SoundSTBoostTheme
import com.soundboost.ui.theme.getThemeColors

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* İzin reddedilirse sadece bildirim gösterilmez */ }

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "✅ Mikrofon izni verildi - Görselleştirme aktif")
        } else {
            android.util.Log.w("MainActivity", "❌ Mikrofon izni reddedildi - Görselleştirme çalışmayacak")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // CRITICAL: Apply saved language BEFORE setting content
        // This ensures proper system language detection and immediate effect
        com.soundboost.data.LanguageManager.applyLanguage(this)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
        }
        
        // CRITICAL: Request microphone permission for audio visualizer
        // This is REQUIRED for RealTimeAudioAnalyzer to work
        requestMicrophonePermissionIfNeeded()

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
                        
                        // Status bar (üst)
                        window.statusBarColor = android.graphics.Color.TRANSPARENT
                        
                        // Navigation bar (alt - geri, home, recent apps)
                        window.navigationBarColor = themeColors.background.toArgb()
                        
                        // Icon colors - dark icons on light background, light icons on dark
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !effectiveDarkMode
                            isAppearanceLightNavigationBars = !effectiveDarkMode
                        }
                    }
                }
                
                MainScreen(viewModel = viewModel)
            }
        }
    }
    
    private fun requestMicrophonePermissionIfNeeded() {
        when {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                // Already granted
                android.util.Log.d("MainActivity", "✅ Mikrofon izni zaten var")
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                // Show rationale and request
                android.util.Log.d("MainActivity", "ℹ️ Mikrofon izni açıklaması gösteriliyor")
                microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            else -> {
                // First time - request directly
                android.util.Log.d("MainActivity", "🎤 İlk mikrofon izni isteniyor")
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
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val audioLevels by viewModel.audioLevels.collectAsState()
    val themeColors = getThemeColors(uiState.theme, uiState.colorAccent)
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLanguage = remember { mutableStateOf(viewModel.getCurrentLanguage(context)) }
    
    var showRateDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    
    // CRITICAL FIX: Cache WebView instance at MainScreen level to survive navigation
    // This prevents WebView disposal when navigating away from home screen
    val cachedWebView = remember { mutableStateOf<android.webkit.WebView?>(null) }
    
    android.util.Log.d("MainActivity", "🔵 MainScreen recomposed, cachedWebView: ${cachedWebView.value != null}")
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = themeColors.background
    ) {
        NavHost(navController = navController, startDestination = "volume") {
            composable(
                route = "volume",
                content = {
                    // Use WebView-based home screen with awwardstheme.html
                    // Pass cached WebView to survive navigation
                    com.soundboost.ui.components.WebViewHomeScreen(
                        state = uiState,
                        audioLevels = audioLevels,
                        audioAnalysis = viewModel.audioAnalysis,
                        onVolumeChange = viewModel::onMasterGainChanged,
                        onSensitivityChange = viewModel::onSensitivityChanged,
                        onToggleBoost = viewModel::toggleBoost,
                        onThemeChanged = viewModel::onThemeChanged,
                        onModeChanged = viewModel::onDarkModeChanged,  // CRITICAL: Dark/Light mode from HTML toggle
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToEqualizer = { navController.navigate("equalizer") },
                        onNavigateToLanguage = { navController.navigate("language") },
                        cachedWebView = cachedWebView,
                        currentLanguage = currentLanguage.value  // CRITICAL: Pass current language for immediate sync
                    )
                }
            )
            
            composable("equalizer") {
                EqualizerScreen(
                    state = uiState,
                    onBassBoostChanged = viewModel::onBassBoostChanged,
                    onVirtualizerChanged = viewModel::onVirtualizerChanged,
                    onEqChanged = viewModel::onEqChanged,
                    onBack = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("themes") {
                ThemeScreen(
                    state = uiState,
                    onThemeChanged = viewModel::onThemeChanged,
                    onColorAccentChanged = viewModel::onColorAccentChanged,
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("language") {
                LanguageScreen(
                    state = uiState,
                    currentLanguage = currentLanguage.value,
                    onLanguageSelected = { language ->
                        currentLanguage.value = language
                        viewModel.onLanguageChanged(language, context)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            
            composable("help") {
                HelpScreen(
                    state = uiState,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        
        // Dialogs
        if (showRateDialog) {
            com.soundboost.ui.components.RateAppDialog(
                themeColors = themeColors,
                onDismiss = { showRateDialog = false },
                onRated = { viewModel.onAppRated() }
            )
        }
        
        if (showShareDialog) {
            com.soundboost.ui.components.ShareDialog(
                themeColors = themeColors,
                onDismiss = { showShareDialog = false }
            )
        }
    }
}
