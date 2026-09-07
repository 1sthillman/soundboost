package com.soundboost.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.soundboost.audio.AudioAnalysis
import com.soundboost.data.BoostSettings
import kotlinx.coroutines.flow.Flow

/**
 * WebView-based home screen using awwardstheme.html
 * JavaScript bridge for audio communication
 * 
 * CRITICAL: Accepts cachedWebView from parent to survive navigation.
 * This prevents state loss when navigating away and back.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewHomeScreen(
    state: BoostSettings,
    audioLevels: FloatArray?,
    audioAnalysis: Flow<AudioAnalysis?>,
    onVolumeChange: (Int) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onToggleBoost: () -> Unit,
    onThemeChanged: (com.soundboost.ui.theme.AppTheme) -> Unit,
    onModeChanged: (Boolean?) -> Unit,  // CRITICAL: Dark/Light mode callback
    onNavigateToSettings: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    cachedWebView: androidx.compose.runtime.MutableState<WebView?>
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveDarkMode = state.isDarkMode ?: systemInDarkTheme
    
    var isWebViewReady by remember { mutableStateOf(false) }
    var lastThemeFromKotlin by remember { mutableStateOf<com.soundboost.ui.theme.AppTheme?>(null) }
    
    // Keep WebView alive when navigating away
    DisposableEffect(Unit) {
        android.util.Log.d("WebViewHomeScreen", "💚 WebViewHomeScreen entered composition")
        
        // CRITICAL: Sync state when returning from navigation
        // WebView DOM is still alive, just need to update from Kotlin state
        if (isWebViewReady) {
            android.util.Log.d("WebViewHomeScreen", "🔄 Returning from navigation - syncing state")
            
            // Force sync dark/light mode (CRITICAL)
            val mode = if (effectiveDarkMode) "dark" else "light"
            cachedWebView.value?.evaluateJavascript(
                "if(window.setModeFromAndroid) { console.log('Force sync mode: $mode'); window.setModeFromAndroid('$mode'); }",
                null
            )
            
            // Force sync language (CRITICAL when returning from language selection)
            val currentLang = com.soundboost.data.LanguageManager.getCurrentLanguage(context)
            val langCode = when (currentLang.code) {
                "tr" -> "tr"
                "en" -> "en"
                "de" -> "de"
                "fr" -> "fr"
                "es" -> "es"
                "ru" -> "ru"
                "ar" -> "ar"
                "ja" -> "ja"
                "zh" -> "zh"
                "ko" -> "ko"
                "system" -> com.soundboost.data.LanguageManager.getSystemLanguage(context)
                else -> "en"
            }
            cachedWebView.value?.evaluateJavascript(
                "if(window.setLanguage) { console.log('Force sync language: $langCode'); window.setLanguage('$langCode'); }",
                null
            )
            
            // Force sync boost state (CRITICAL for play button)
            // Use updatePlayButtonState directly to bypass playing !== enabled check
            cachedWebView.value?.evaluateJavascript(
                """
                if(typeof updatePlayButtonState === 'function') {
                    console.log('Force updating play button state to: ${state.isBoostEnabled}');
                    updatePlayButtonState(${state.isBoostEnabled});
                }
                """.trimIndent(),
                null
            )
            
            // Force sync volume
            cachedWebView.value?.evaluateJavascript(
                "if(window.setVolumeFromKotlin) { window.setVolumeFromKotlin(${state.masterGainPercent}); }",
                null
            )
            
            // Force sync sensitivity
            cachedWebView.value?.evaluateJavascript(
                "if(document.getElementById('sens')) { document.getElementById('sens').value = ${state.sensitivity}; document.getElementById('sensVal').textContent = ${state.sensitivity}; }",
                null
            )
            
            // CRITICAL: Force layout recalculation to prevent navigation bar overflow
            cachedWebView.value?.post {
                cachedWebView.value?.requestLayout()
                cachedWebView.value?.invalidate()
            }
        }
        
        onDispose {
            // CRITICAL FIX: Don't call onPause() - it can break layout when resuming
            // WebView will remain active in background, which is acceptable for better UX
            android.util.Log.d("WebViewHomeScreen", "⚠️ WebViewHomeScreen leaving composition - keeping WebView active")
        }
    }
    
    // Update audio levels in WebView with REAL AUDIO DATA
    LaunchedEffect(audioLevels) {
        if (!isWebViewReady) return@LaunchedEffect
        audioLevels?.let { levels ->
            // Send minimal bar data for legacy support
            val barsJson = levels.joinToString(",")
            cachedWebView.value?.evaluateJavascript(
                "if(window.updateAudioLevels) { window.updateAudioLevels([$barsJson]); }",
                null
            )
        }
    }
    
    // NEW: Send complete audio analysis data from RealTimeAudioAnalyzer
    LaunchedEffect(isWebViewReady) {
        if (!isWebViewReady) return@LaunchedEffect
        
        // Listen to audio analysis flow
        audioAnalysis.collect { analysis ->
            analysis?.let {
                // Escape JSON properly for JavaScript injection
                val audioDataJson = """
                    {"bars":[${it.bars.joinToString(",")}],"waveform":[${it.waveform.joinToString(",")}],"subBass":${it.subBass},"bass":${it.bass},"lowMid":${it.lowMid},"mid":${it.mid},"highMid":${it.highMid},"brilliance":${it.brilliance},"energy":${it.energy},"spectralFlux":${it.spectralFlux},"isBeat":${it.isBeat}}
                """.trimIndent().replace("\n", "")
                
                cachedWebView.value?.evaluateJavascript(
                    "if(window.updateRealAudioData) { window.updateRealAudioData('$audioDataJson'); }",
                    null
                )
            }
        }
    }
    
    // Update boost state
    LaunchedEffect(state.isBoostEnabled) {
        if (!isWebViewReady) return@LaunchedEffect
        android.util.Log.d("WebViewHomeScreen", "Updating boost state: ${state.isBoostEnabled}")
        cachedWebView.value?.evaluateJavascript(
            "if(window.setBoostState) { window.setBoostState(${state.isBoostEnabled}); }",
            null
        )
    }
    
    // Update volume value (map Kotlin 60-200 to HTML 0-100)
    LaunchedEffect(state.masterGainPercent) {
        if (!isWebViewReady) return@LaunchedEffect
        cachedWebView.value?.evaluateJavascript(
            "if(window.setVolumeFromKotlin) { window.setVolumeFromKotlin(${state.masterGainPercent}); }",
            null
        )
    }
    
    // Update sensitivity value
    LaunchedEffect(state.sensitivity) {
        if (!isWebViewReady) return@LaunchedEffect
        cachedWebView.value?.evaluateJavascript(
            "if(document.getElementById('sens')) { document.getElementById('sens').value = ${state.sensitivity}; document.getElementById('sensVal').textContent = ${state.sensitivity}; }",
            null
        )
    }
    
    // Update theme from Kotlin (Settings screen'den değiştirildiğinde)
    LaunchedEffect(state.theme) {
        if (!isWebViewReady) return@LaunchedEffect
        // Sadece Kotlin'den değiştirilmişse HTML'e gönder
        if (lastThemeFromKotlin != state.theme) {
            val themeName = when (state.theme) {
                com.soundboost.ui.theme.AppTheme.SUMI -> "sumi"
                com.soundboost.ui.theme.AppTheme.AURORA -> "aurora"
                com.soundboost.ui.theme.AppTheme.NOVA -> "nova"
                com.soundboost.ui.theme.AppTheme.MYCEL -> "mycel"
                com.soundboost.ui.theme.AppTheme.REEF -> "reef"
                com.soundboost.ui.theme.AppTheme.MONSOON -> "monsoon"
                com.soundboost.ui.theme.AppTheme.MUREKKEP -> "murekkep"
                com.soundboost.ui.theme.AppTheme.COL -> "col"
            }
            cachedWebView.value?.evaluateJavascript(
                "if(window.setThemeFromAndroid) { window.setThemeFromAndroid('$themeName'); }",
                null
            )
        }
    }
    
    // CRITICAL: Update dark/light mode when changed from Settings
    LaunchedEffect(effectiveDarkMode) {
        if (!isWebViewReady) return@LaunchedEffect
        val mode = if (effectiveDarkMode) "dark" else "light"
        android.util.Log.d("WebViewHomeScreen", "Syncing mode to WebView: $mode")
        cachedWebView.value?.evaluateJavascript(
            "if(window.setModeFromAndroid) { window.setModeFromAndroid('$mode'); }",
            null
        )
    }
    
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            android.util.Log.d("WebViewHomeScreen", "🏗️ Factory called")
            
            // Return existing cached WebView if available, otherwise create new one
            cachedWebView.value ?: WebView(ctx).apply {
                android.util.Log.d("WebViewHomeScreen", "🆕 Creating NEW WebView instance")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // WebView ready, initialize with current state
                        isWebViewReady = true
                        
                        android.util.Log.d("WebViewHomeScreen", "📱 Page loaded, restoring state...")
                        
                        // Restore all state from Kotlin
                        // 1. Set language
                        val currentLang = com.soundboost.data.LanguageManager.getCurrentLanguage(ctx)
                        val langCode = when (currentLang.code) {
                            "tr" -> "tr"
                            "en" -> "en"
                            "de" -> "de"
                            "fr" -> "fr"
                            "es" -> "es"
                            "ru" -> "ru"
                            "ar" -> "ar"
                            "ja" -> "ja"
                            "zh" -> "zh"
                            "ko" -> "ko"
                            "system" -> com.soundboost.data.LanguageManager.getSystemLanguage(ctx)
                            else -> "en"
                        }
                        evaluateJavascript("if(window.setLanguage) { window.setLanguage('$langCode'); }", null)
                        
                        // 2. Restore boost state
                        evaluateJavascript(
                            "if(window.setBoostState) { window.setBoostState(${state.isBoostEnabled}); }",
                            null
                        )
                        android.util.Log.d("WebViewHomeScreen", "✅ Restored boost state: ${state.isBoostEnabled}")
                        
                        // 3. Restore volume slider
                        evaluateJavascript(
                            "if(window.setVolumeFromKotlin) { window.setVolumeFromKotlin(${state.masterGainPercent}); }",
                            null
                        )
                        android.util.Log.d("WebViewHomeScreen", "✅ Restored volume: ${state.masterGainPercent}")
                        
                        // 4. Restore sensitivity slider
                        evaluateJavascript(
                            "if(document.getElementById('sens')) { document.getElementById('sens').value = ${state.sensitivity}; document.getElementById('sensVal').textContent = ${state.sensitivity}; }",
                            null
                        )
                        
                        // 5. Restore theme
                        val themeName = when (state.theme) {
                            com.soundboost.ui.theme.AppTheme.SUMI -> "sumi"
                            com.soundboost.ui.theme.AppTheme.AURORA -> "aurora"
                            com.soundboost.ui.theme.AppTheme.NOVA -> "nova"
                            com.soundboost.ui.theme.AppTheme.MYCEL -> "mycel"
                            com.soundboost.ui.theme.AppTheme.REEF -> "reef"
                            com.soundboost.ui.theme.AppTheme.MONSOON -> "monsoon"
                            com.soundboost.ui.theme.AppTheme.MUREKKEP -> "murekkep"
                            com.soundboost.ui.theme.AppTheme.COL -> "col"
                        }
                        evaluateJavascript(
                            "if(window.setThemeFromAndroid) { window.setThemeFromAndroid('$themeName'); }",
                            null
                        )
                        android.util.Log.d("WebViewHomeScreen", "✅ Restored theme: $themeName")
                        
                        // 6. CRITICAL: Restore dark/light mode
                        val mode = if (effectiveDarkMode) "dark" else "light"
                        evaluateJavascript(
                            "if(window.setModeFromAndroid) { window.setModeFromAndroid('$mode'); }",
                            null
                        )
                        android.util.Log.d("WebViewHomeScreen", "✅ Restored mode: $mode")
                    }
                }
                
                // Enable console logging for debugging
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            android.util.Log.d("WebView", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                        }
                        return true
                    }
                }
                
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                
                // Add JavaScript interface
                addJavascriptInterface(
                    WebViewBridge(
                        onVolumeChange = onVolumeChange,
                        onSensitivityChange = onSensitivityChange,
                        onToggleBoost = onToggleBoost,
                        onThemeChanged = { themeName ->
                            val theme = when (themeName) {
                                "sumi" -> com.soundboost.ui.theme.AppTheme.SUMI
                                "aurora" -> com.soundboost.ui.theme.AppTheme.AURORA
                                "nova" -> com.soundboost.ui.theme.AppTheme.NOVA
                                "mycel" -> com.soundboost.ui.theme.AppTheme.MYCEL
                                "reef" -> com.soundboost.ui.theme.AppTheme.REEF
                                "monsoon" -> com.soundboost.ui.theme.AppTheme.MONSOON
                                "murekkep" -> com.soundboost.ui.theme.AppTheme.MUREKKEP
                                "col" -> com.soundboost.ui.theme.AppTheme.COL
                                else -> com.soundboost.ui.theme.AppTheme.SUMI
                            }
                            lastThemeFromKotlin = theme  // HTML'den geldiğini işaretle
                            onThemeChanged(theme)
                        },
                        onModeChanged = { mode ->
                            // Convert "dark"/"light" string to Boolean? (null = system, true = dark, false = light)
                            val isDarkMode = when (mode) {
                                "dark" -> true
                                "light" -> false
                                else -> null  // system
                            }
                            android.util.Log.d("WebViewHomeScreen", "Mode changed from HTML: $mode -> isDarkMode: $isDarkMode")
                            onModeChanged(isDarkMode)
                        },
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToEqualizer = onNavigateToEqualizer,
                        onNavigateToLanguage = onNavigateToLanguage
                    ),
                    "AndroidBridge"
                )
                
                loadUrl("file:///android_asset/awwardstheme.html")
                
                // Cache this WebView instance
                cachedWebView.value = this
            }
        },
        update = { view ->
            // Update block - called on recomposition
            // CRITICAL: WebView already exists, just resume if needed
            android.util.Log.d("WebViewHomeScreen", "🔄 Update called - WebView exists: ${cachedWebView.value != null}")
            
            // Ensure WebView is in active state when visible
            if (cachedWebView.value == null) {
                android.util.Log.w("WebViewHomeScreen", "⚠️ Cached WebView lost, saving current instance")
                cachedWebView.value = view
            }
        }
    )
}

/**
 * JavaScript Bridge for WebView communication
 */
class WebViewBridge(
    private val onVolumeChange: (Int) -> Unit,
    private val onSensitivityChange: (Int) -> Unit,
    private val onToggleBoost: () -> Unit,
    private val onThemeChanged: (String) -> Unit,
    private val onModeChanged: (String) -> Unit,
    private val onNavigateToSettings: () -> Unit,
    private val onNavigateToEqualizer: () -> Unit,
    private val onNavigateToLanguage: () -> Unit
) {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    
    @android.webkit.JavascriptInterface
    fun onVolumeChanged(volume: Int) {
        handler.post { onVolumeChange(volume) }
    }
    
    @android.webkit.JavascriptInterface
    fun onSensitivityChanged(sensitivity: Int) {
        handler.post { onSensitivityChange(sensitivity) }
    }
    
    @android.webkit.JavascriptInterface
    fun toggleBoost() {
        handler.post { onToggleBoost() }
    }
    
    @android.webkit.JavascriptInterface
    fun changeTheme(themeName: String) {
        handler.post { onThemeChanged(themeName) }
    }
    
    @android.webkit.JavascriptInterface
    fun onModeChanged(mode: String) {
        android.util.Log.d("WebViewBridge", "Mode changed from HTML: $mode")
        handler.post { onModeChanged(mode) }
    }
    
    @android.webkit.JavascriptInterface
    fun navigateToHome() {
        // Already on home
    }
    
    @android.webkit.JavascriptInterface
    fun navigateToSettings() {
        handler.post { onNavigateToSettings() }
    }
    
    @android.webkit.JavascriptInterface
    fun navigateToEqualizer() {
        handler.post { onNavigateToEqualizer() }
    }
    
    @android.webkit.JavascriptInterface
    fun navigateToLanguage() {
        handler.post { onNavigateToLanguage() }
    }
}
