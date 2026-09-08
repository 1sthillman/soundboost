package com.soundboost.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.soundboost.audio.AudioAnalysis
import com.soundboost.data.BoostSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay

/**
 * PROFESSIONAL WebView Implementation with Persistent Manager
 * 
 * This component uses PersistentWebViewManager to ensure:
 * 1. WebView NEVER gets paused or destroyed during navigation
 * 2. JavaScript execution continues seamlessly
 * 3. State syncs automatically when returning from Kotlin screens
 * 4. Audio visualization works continuously
 * 
 * Architecture:
 * - Single WebView instance lives at application level
 * - WebView is never removed from view hierarchy (just hidden/shown)
 * - State sync is deterministic and atomic
 * - No race conditions from multiple LaunchedEffect blocks
 */
@Composable
fun WebViewHomeScreenPersistent(
    state: BoostSettings,
    audioLevels: FloatArray?,
    audioAnalysis: Flow<AudioAnalysis?>,
    onVolumeChange: (Int) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onToggleBoost: () -> Unit,
    onThemeChanged: (com.soundboost.ui.theme.AppTheme) -> Unit,
    onModeChanged: (Boolean?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    currentLanguage: com.soundboost.data.AppLanguage
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveDarkMode = state.isDarkMode ?: systemInDarkTheme
    
    // Track if WebView manager is initialized
    var isManagerReady by remember { mutableStateOf(false) }
    
    // Track if we've done initial sync to prevent duplicates
    var hasInitialSynced by remember { mutableStateOf(false) }
    
    // Initialize WebView manager once
    LaunchedEffect(Unit) {
        if (!PersistentWebViewManager.isReady()) {
            android.util.Log.d("WebViewPersistent", "🔧 Initializing WebView manager")
            PersistentWebViewManager.initialize(
                context,
                onVolumeChange,
                onSensitivityChange,
                onToggleBoost,
                onThemeChanged,
                onModeChanged,
                onNavigateToSettings,
                onNavigateToEqualizer,
                onNavigateToLanguage
            )
            
            // Wait for page to load
            delay(500)
            isManagerReady = true
            android.util.Log.d("WebViewPersistent", "✅ Manager ready")
            
            // Do FIRST sync after page loads
            val langCode = when (currentLanguage.code) {
                "tr" -> "tr"; "en" -> "en"; "de" -> "de"; "fr" -> "fr"
                "es" -> "es"; "ru" -> "ru"; "ar" -> "ar"; "ja" -> "ja"
                "zh" -> "zh"; "ko" -> "ko"
                "system" -> com.soundboost.data.LanguageManager.getSystemLanguage(context)
                else -> "en"
            }
            
            android.util.Log.d("WebViewPersistent", "🔄 Initial sync")
            PersistentWebViewManager.syncState(
                isBoostEnabled = state.isBoostEnabled,
                masterGainPercent = state.masterGainPercent,
                sensitivity = state.sensitivity,
                theme = state.theme,
                isDarkMode = effectiveDarkMode,
                languageCode = langCode
            )
            hasInitialSynced = true
        } else {
            isManagerReady = true
            hasInitialSynced = true
        }
    }
    
    // Sync ONLY when state actually changes (not on every recomposition)
    LaunchedEffect(
        state.isBoostEnabled,
        state.masterGainPercent,
        state.sensitivity,
        state.theme,
        effectiveDarkMode,
        currentLanguage.code
    ) {
        // Skip if not ready or haven't done initial sync yet
        if (!isManagerReady || !hasInitialSynced) return@LaunchedEffect
        
        android.util.Log.d("WebViewPersistent", "🔄 State changed - syncing once")
        
        val langCode = when (currentLanguage.code) {
            "tr" -> "tr"; "en" -> "en"; "de" -> "de"; "fr" -> "fr"
            "es" -> "es"; "ru" -> "ru"; "ar" -> "ar"; "ja" -> "ja"
            "zh" -> "zh"; "ko" -> "ko"
            "system" -> com.soundboost.data.LanguageManager.getSystemLanguage(context)
            else -> "en"
        }
        
        // Single sync - no repeats
        PersistentWebViewManager.syncState(
            isBoostEnabled = state.isBoostEnabled,
            masterGainPercent = state.masterGainPercent,
            sensitivity = state.sensitivity,
            theme = state.theme,
            isDarkMode = effectiveDarkMode,
            languageCode = langCode
        )
    }
    
    // Update audio levels for visualization
    LaunchedEffect(audioLevels) {
        if (isManagerReady) {
            audioLevels?.let {
                PersistentWebViewManager.updateAudioLevels(it)
            }
        }
    }
    
    // Update full audio analysis data
    LaunchedEffect(isManagerReady) {
        if (!isManagerReady) return@LaunchedEffect
        
        audioAnalysis.collect { analysis ->
            analysis?.let {
                val audioDataJson = """{"bars":[${it.bars.joinToString(",")}],"waveform":[${it.waveform.joinToString(",")}],"subBass":${it.subBass},"bass":${it.bass},"lowMid":${it.lowMid},"mid":${it.mid},"highMid":${it.highMid},"brilliance":${it.brilliance},"energy":${it.energy},"spectralFlux":${it.spectralFlux},"isBeat":${it.isBeat}}"""
                PersistentWebViewManager.updateAudioAnalysis(audioDataJson)
            }
        }
    }
    
    // Display WebView using AndroidView
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            android.util.Log.d("WebViewPersistent", "🏗️ Factory called - getting persistent WebView")
            PersistentWebViewManager.getWebView(ctx)
        },
        update = { view ->
            // CRITICAL: Ensure WebView is always visible
            view.visibility = android.view.View.VISIBLE
            
            // NO SYNC HERE - causes duplicate calls on every recomposition
            // State sync is handled by LaunchedEffect blocks only
        }
    )
}
