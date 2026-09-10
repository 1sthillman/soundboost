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
import androidx.compose.foundation.layout.Box
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
import com.soundboost.ui.components.OnboardingOverlayForWebView
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
            android.util.Log.d("MainActivity", "✅ Mikrofon izni verildi - Boost başlatılıyor")
            // İzin verildiyse boost'u başlat
            viewModel.startBoostAfterPermission()
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
        
        // CRITICAL: Request microphone permission for audio visualizer on first launch
        // This is REQUIRED for RealTimeAudioAnalyzer to work
        val sp = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!sp.getBoolean("first_launch_done", false)) {
            android.util.Log.d("MainActivity", "🎤 İlk açılış - mikrofon izni isteniyor")
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            sp.edit().putBoolean("first_launch_done", true).apply()
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
    
    fun checkAndRequestMicrophonePermission() {
        when {
            androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                // Already granted - start boost immediately
                android.util.Log.d("MainActivity", "✅ Mikrofon izni zaten var - Boost başlatılıyor")
                viewModel.startBoostAfterPermission()
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
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val audioLevels by viewModel.audioLevels.collectAsState()
    val themeColors = getThemeColors(uiState.theme, uiState.colorAccent)
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // YENİ: Onboarding state
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val onboardingStep by viewModel.currentOnboardingStep.collectAsState()
    
    // CRITICAL: Track language as state to trigger immediate WebView updates
    var currentLanguage by remember { mutableStateOf(viewModel.getCurrentLanguage(context)) }
    
    var showRateDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    
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
        NavHost(navController = navController, startDestination = "volume") {
            composable(
                route = "volume",
                content = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Ana WebView ekranı
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
                        
                        // YENİ: Onboarding Overlay (WebView üstünde)
                        if (!isOnboardingCompleted) {
                            OnboardingOverlayForWebView(
                                currentStep = onboardingStep,
                                onStepComplete = viewModel::onOnboardingStepComplete,
                                onSkip = viewModel::skipOnboarding,
                                themeColors = themeColors
                            )
                        }
                    }
                }
            )
            
            composable("equalizer") {
                EqualizerScreen(
                    state = uiState,
                    onBassBoostChanged = viewModel::onBassBoostChanged,
                    onVirtualizerChanged = viewModel::onVirtualizerChanged,
                    onEqChanged = viewModel::onEqChanged,
                    onVocalMusicBalanceChanged = viewModel::onVocalMusicBalanceChanged,
                    onNavigateToAISeparation = {
                        navController.navigate("ai_vocal_separation")
                    },
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
                    onBack = { 
                        navController.popBackStack()
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
    }
}
