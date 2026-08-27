package com.soundboost

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.soundboost.ui.screens.*
import com.soundboost.ui.theme.SoundSTBoostTheme
import com.soundboost.ui.theme.getThemeColors
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* İzin reddedilirse sadece bildirim gösterilmez */ }

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* İzin reddedilirse visualizer animasyonlu gösterilir */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch("android.permission.POST_NOTIFICATIONS")
        }
        
        // Request microphone permission for audio visualizer (Visualizer API)
        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            SoundSTBoostTheme {
                MainScreen(viewModel = viewModel)
            }
        }
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
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = themeColors.background
    ) {
        NavHost(navController = navController, startDestination = "volume") {
            composable("volume") {
                VolumeScreen(
                    state = uiState,
                    audioLevels = audioLevels,
                    onVolumeChange = viewModel::onMasterGainChanged,
                    onToggleBoost = viewModel::toggleBoost,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenEqualizer = { navController.navigate("equalizer") },
                    onThemeChanged = viewModel::onThemeChanged,
                    onOpenLanguage = { navController.navigate("language") }
                )
            }
            
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
        }
    }
}
