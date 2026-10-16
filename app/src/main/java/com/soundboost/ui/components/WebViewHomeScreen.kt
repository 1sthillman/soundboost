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
    cachedWebView: androidx.compose.runtime.MutableState<WebView?>,
    currentLanguage: com.soundboost.data.AppLanguage  // NEW: Current language for immediate sync
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveDarkMode = state.isDarkMode ?: systemInDarkTheme
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()  // NEW: For force restore
    
    var isWebViewReady by remember { mutableStateOf(false) }
    var lastThemeFromKotlin by remember { mutableStateOf<com.soundboost.ui.theme.AppTheme?>(null) }
    var hasInitializedWebView by remember { mutableStateOf(false) }  // NEW: Track if WebView was initialized
    
    // CRITICAL: Sync language IMMEDIATELY when it changes (no waiting for navigation!)
    // This handles both: 1) returning from language screen, 2) activity recreation after language change
    LaunchedEffect(currentLanguage, isWebViewReady) {
        if (!isWebViewReady) {
            android.util.Log.d("WebViewHomeScreen", "⏳ WebView not ready yet, skipping language sync")
            return@LaunchedEffect
        }
        
        val langCode = when (currentLanguage.code) {
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
        
        android.util.Log.d("WebViewHomeScreen", "🌐 IMMEDIATE Language sync! Code: $langCode from ${currentLanguage.code}")
        cachedWebView.value?.evaluateJavascript(
            """
            if(typeof window.setLanguage === 'function') { 
                console.log('📢 INSTANT language change: $langCode'); 
                window.setLanguage('$langCode'); 
            } else {
                console.error('❌ window.setLanguage function not found');
            }
            """.trimIndent(),
            null
        )
    }
    
    // Keep WebView alive when navigating away
    DisposableEffect(Unit) {
        android.util.Log.d("WebViewHomeScreen", "💚 WebViewHomeScreen entered composition")
        
        onDispose {
            // CRITICAL: Don't call onPause() - keep WebView active
            android.util.Log.d("WebViewHomeScreen", "⚠️ WebViewHomeScreen leaving composition - keeping WebView active")
        }
    }
    
    // CRITICAL: When returning to this screen, immediately restore state if WebView exists
    LaunchedEffect(Unit) {
        if (cachedWebView.value != null && isWebViewReady && hasInitializedWebView) {
            android.util.Log.d("WebViewHomeScreen", "🔄 Returned to screen - restoring state immediately")
            
            // Force immediate state sync
            kotlinx.coroutines.delay(100) // Small delay for WebView to be ready
            
            val langCode = when (currentLanguage.code) {
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
            
            val mode = if (effectiveDarkMode) "dark" else "light"
            
            val themeName = when (state.theme) {
                com.soundboost.ui.theme.AppTheme.MEHTAP -> "mehtap"
                com.soundboost.ui.theme.AppTheme.SUMI -> "sumi"
                com.soundboost.ui.theme.AppTheme.AURORA -> "aurora"
                com.soundboost.ui.theme.AppTheme.FENER -> "fener"
                com.soundboost.ui.theme.AppTheme.ORMAN -> "orman"
                com.soundboost.ui.theme.AppTheme.EYES -> "eyes"
                com.soundboost.ui.theme.AppTheme.MYCEL -> "mycel"
                com.soundboost.ui.theme.AppTheme.REEF -> "reef"
                com.soundboost.ui.theme.AppTheme.MONSOON -> "monsoon"
                com.soundboost.ui.theme.AppTheme.MUREKKEP -> "murekkep"
                com.soundboost.ui.theme.AppTheme.COL -> "col"
                com.soundboost.ui.theme.AppTheme.DIVIT -> "divit"
            }
            
            android.util.Log.d("WebViewHomeScreen", "🔥 FORCE SYNC on return:")
            android.util.Log.d("WebViewHomeScreen", "   Language: $langCode")
            android.util.Log.d("WebViewHomeScreen", "   Boost: ${state.isBoostEnabled}")
            android.util.Log.d("WebViewHomeScreen", "   Mode: $mode")
            android.util.Log.d("WebViewHomeScreen", "   Theme: $themeName")
            
            cachedWebView.value?.evaluateJavascript(
                """
                (function() {
                    console.log('🔥 FORCE RESTORE STATE ON RETURN');
                    
                    if(typeof window.updatePlayButtonState === 'function') {
                        console.log('🎵 Force restore play: ${state.isBoostEnabled}');
                        window.updatePlayButtonState(${state.isBoostEnabled});
                    }
                    
                    if(typeof window.setModeFromAndroid === 'function') {
                        console.log('🌓 Force restore mode: $mode');
                        window.setModeFromAndroid('$mode');
                    }
                    
                    if(typeof window.setLanguage === 'function') {
                        console.log('🌐 Force restore language: $langCode');
                        window.setLanguage('$langCode');
                    }
                    
                    if(typeof window.setVolumeFromKotlin === 'function') {
                        window.setVolumeFromKotlin(${state.masterGainPercent});
                    }
                    
                    if(document.getElementById('sens')) {
                        document.getElementById('sens').value = ${state.sensitivity};
                        if(document.getElementById('sensVal')) {
                            document.getElementById('sensVal').textContent = ${state.sensitivity};
                        }
                    }
                    
                    if(typeof window.setThemeFromAndroid === 'function') {
                        window.setThemeFromAndroid('$themeName');
                    }
                    
                    console.log('✅ Force restore complete!');
                })();
                """.trimIndent(),
                null
            )
        }
    }
    
    // CRITICAL FIX: REAL-TIME bidirectional sync - monitors ALL state changes
    // This runs EVERY TIME any state value changes (boost, volume, sensitivity, theme, mode, language)
    LaunchedEffect(
        isWebViewReady,
        state.isBoostEnabled,
        state.masterGainPercent,
        state.sensitivity,
        state.theme,
        effectiveDarkMode,
        currentLanguage.code
    ) {
        if (!isWebViewReady || cachedWebView.value == null) {
            android.util.Log.d("WebViewHomeScreen", "⏳ WebView not ready, skipping sync")
            return@LaunchedEffect
        }
        
        android.util.Log.d("WebViewHomeScreen", "🔄 STATE CHANGED - REAL-TIME SYNC")
        android.util.Log.d("WebViewHomeScreen", "   Boost: ${state.isBoostEnabled}")
        android.util.Log.d("WebViewHomeScreen", "   Volume: ${state.masterGainPercent}")
        android.util.Log.d("WebViewHomeScreen", "   Sensitivity: ${state.sensitivity}")
        android.util.Log.d("WebViewHomeScreen", "   Theme: ${state.theme}")
        android.util.Log.d("WebViewHomeScreen", "   Mode: ${if (effectiveDarkMode) "dark" else "light"}")
        android.util.Log.d("WebViewHomeScreen", "   Language: ${currentLanguage.code}")
        
        // Small delay to ensure WebView rendering is complete
        kotlinx.coroutines.delay(50)
        
        val themeName = when (state.theme) {
            com.soundboost.ui.theme.AppTheme.MEHTAP -> "mehtap"
            com.soundboost.ui.theme.AppTheme.SUMI -> "sumi"
            com.soundboost.ui.theme.AppTheme.AURORA -> "aurora"
            com.soundboost.ui.theme.AppTheme.FENER -> "fener"
            com.soundboost.ui.theme.AppTheme.ORMAN -> "orman"
            com.soundboost.ui.theme.AppTheme.EYES -> "eyes"
            com.soundboost.ui.theme.AppTheme.MYCEL -> "mycel"
            com.soundboost.ui.theme.AppTheme.REEF -> "reef"
            com.soundboost.ui.theme.AppTheme.MONSOON -> "monsoon"
            com.soundboost.ui.theme.AppTheme.MUREKKEP -> "murekkep"
            com.soundboost.ui.theme.AppTheme.COL -> "col"
            com.soundboost.ui.theme.AppTheme.DIVIT -> "divit"
        }
        
        val langCode = when (currentLanguage.code) {
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
        
        val mode = if (effectiveDarkMode) "dark" else "light"
        
        // Single JavaScript call with ALL state updates
        cachedWebView.value?.evaluateJavascript(
            """
            (function() {
                console.log('🔥 REAL-TIME STATE SYNC');
                
                // 1. Sync play button (MOST CRITICAL)
                if(typeof window.updatePlayButtonState === 'function') {
                    console.log('🎵 Sync play: ${state.isBoostEnabled}');
                    window.updatePlayButtonState(${state.isBoostEnabled});
                } else {
                    console.error('❌ window.updatePlayButtonState not found!');
                }
                
                // 2. Sync dark/light mode
                if(typeof window.setModeFromAndroid === 'function') {
                    console.log('🌓 Sync mode: $mode');
                    window.setModeFromAndroid('$mode');
                } else {
                    console.error('❌ window.setModeFromAndroid not found!');
                }
                
                // 3. Sync language (INSTANT)
                if(typeof window.setLanguage === 'function') {
                    console.log('🌐 Sync language: $langCode');
                    window.setLanguage('$langCode');
                } else {
                    console.error('❌ window.setLanguage not found!');
                }
                
                // 4. Sync volume
                if(typeof window.setVolumeFromKotlin === 'function') {
                    console.log('🔊 Sync volume: ${state.masterGainPercent}');
                    window.setVolumeFromKotlin(${state.masterGainPercent});
                } else {
                    console.error('❌ window.setVolumeFromKotlin not found!');
                }
                
                // 5. Sync sensitivity
                if(document.getElementById('sens')) {
                    document.getElementById('sens').value = ${state.sensitivity};
                    if(document.getElementById('sensVal')) {
                        document.getElementById('sensVal').textContent = ${state.sensitivity};
                    }
                    console.log('🎚️ Sync sensitivity: ${state.sensitivity}');
                } else {
                    console.error('❌ sens element not found!');
                }
                
                // 6. Sync theme
                if(typeof window.setThemeFromAndroid === 'function') {
                    console.log('🎨 Sync theme: $themeName');
                    window.setThemeFromAndroid('$themeName');
                } else {
                    console.error('❌ window.setThemeFromAndroid not found!');
                }
                
                console.log('✅ Real-time sync complete!');
            })();
            """.trimIndent(),
            null
        )
    }
    
    // Update audio levels in WebView with REAL AUDIO DATA
    LaunchedEffect(audioLevels) {
        if (!isWebViewReady) return@LaunchedEffect
        audioLevels?.let { levels ->
            // Send minimal bar data for legacy support
            val barsJson = levels.joinToString(",")
            cachedWebView.value?.evaluateJavascript(
                "if(typeof window.updateAudioLevels === 'function') { window.updateAudioLevels([$barsJson]); }",
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
                    "if(typeof window.updateRealAudioData === 'function') { window.updateRealAudioData('$audioDataJson'); }",
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
            "if(typeof window.setBoostState === 'function') { window.setBoostState(${state.isBoostEnabled}); }",
            null
        )
    }
    
    // Update volume value (map Kotlin 60-200 to HTML 0-100)
    LaunchedEffect(state.masterGainPercent) {
        if (!isWebViewReady) return@LaunchedEffect
        cachedWebView.value?.evaluateJavascript(
            "if(typeof window.setVolumeFromKotlin === 'function') { window.setVolumeFromKotlin(${state.masterGainPercent}); }",
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
                com.soundboost.ui.theme.AppTheme.MEHTAP -> "mehtap"
                com.soundboost.ui.theme.AppTheme.SUMI -> "sumi"
                com.soundboost.ui.theme.AppTheme.AURORA -> "aurora"
                com.soundboost.ui.theme.AppTheme.FENER -> "fener"
                com.soundboost.ui.theme.AppTheme.ORMAN -> "orman"
                com.soundboost.ui.theme.AppTheme.EYES -> "eyes"
                com.soundboost.ui.theme.AppTheme.MYCEL -> "mycel"
                com.soundboost.ui.theme.AppTheme.REEF -> "reef"
                com.soundboost.ui.theme.AppTheme.MONSOON -> "monsoon"
                com.soundboost.ui.theme.AppTheme.MUREKKEP -> "murekkep"
                com.soundboost.ui.theme.AppTheme.COL -> "col"
                com.soundboost.ui.theme.AppTheme.DIVIT -> "divit"
            }
            cachedWebView.value?.evaluateJavascript(
                "if(typeof window.setThemeFromAndroid === 'function') { window.setThemeFromAndroid('$themeName'); }",
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
            "if(typeof window.setModeFromAndroid === 'function') { window.setModeFromAndroid('$mode'); }",
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
                        
                        // CRITICAL: Only restore state on FIRST load, not on every page finish
                        if (hasInitializedWebView) {
                            android.util.Log.d("WebViewHomeScreen", "📱 Page finished but already initialized - skipping")
                            return
                        }
                        
                        // WebView ready, initialize with current state
                        isWebViewReady = true
                        hasInitializedWebView = true
                        
                        android.util.Log.d("WebViewHomeScreen", "📱 FIRST PAGE LOAD - Initializing state...")
                        
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
                        evaluateJavascript("if(typeof window.setLanguage === 'function') { window.setLanguage('$langCode'); }", null)
                        
                        // 2. Restore boost state (CRITICAL: Use window.updatePlayButtonState)
                        evaluateJavascript(
                            "if(typeof window.updatePlayButtonState === 'function') { window.updatePlayButtonState(${state.isBoostEnabled}); }",
                            null
                        )
                        android.util.Log.d("WebViewHomeScreen", "✅ Restored boost state: ${state.isBoostEnabled}")
                        
                        // 3. Restore volume slider
                        evaluateJavascript(
                            "if(typeof window.setVolumeFromKotlin === 'function') { window.setVolumeFromKotlin(${state.masterGainPercent}); }",
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
                            com.soundboost.ui.theme.AppTheme.MEHTAP -> "mehtap"
                            com.soundboost.ui.theme.AppTheme.SUMI -> "sumi"
                            com.soundboost.ui.theme.AppTheme.AURORA -> "aurora"
                            com.soundboost.ui.theme.AppTheme.FENER -> "fener"
                            com.soundboost.ui.theme.AppTheme.ORMAN -> "orman"
                            com.soundboost.ui.theme.AppTheme.EYES -> "eyes"
                            com.soundboost.ui.theme.AppTheme.MYCEL -> "mycel"
                            com.soundboost.ui.theme.AppTheme.REEF -> "reef"
                            com.soundboost.ui.theme.AppTheme.MONSOON -> "monsoon"
                            com.soundboost.ui.theme.AppTheme.MUREKKEP -> "murekkep"
                            com.soundboost.ui.theme.AppTheme.COL -> "col"
                            com.soundboost.ui.theme.AppTheme.DIVIT -> "divit"
                        }
                        evaluateJavascript(
                            "if(typeof window.setThemeFromAndroid === 'function') { window.setThemeFromAndroid('$themeName'); }",
                            null
                        )
                        android.util.Log.d("WebViewHomeScreen", "✅ Restored theme: $themeName")
                        
                        // 6. CRITICAL: Restore dark/light mode
                        val mode = if (effectiveDarkMode) "dark" else "light"
                        evaluateJavascript(
                            "if(typeof window.setModeFromAndroid === 'function') { window.setModeFromAndroid('$mode'); }",
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
                        onThemeChanged = onThemeChanged,
                        onModeChanged = onModeChanged
                    ).also { bridge ->
                        // Set navigation callbacks separately
                        bridge.updateNavigationCallbacks(
                            settings = onNavigateToSettings,
                            equalizer = onNavigateToEqualizer,
                            language = onNavigateToLanguage
                        )
                    },
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
 * 
 * SAFETY: Navigation calls use debounce to prevent rapid double-tap crashes
 * LIFECYCLE: Callbacks are nullable and reassignable to prevent leaks after Activity recreation
 */
class WebViewBridge(
    private var onVolumeChange: ((Int) -> Unit)?,
    private var onSensitivityChange: ((Int) -> Unit)?,
    private var onToggleBoost: (() -> Unit)?,
    private var onThemeChanged: ((com.soundboost.ui.theme.AppTheme) -> Unit)?,
    private var onModeChanged: ((Boolean?) -> Unit)?
) {
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    
    // Debounce mechanism for navigation (prevent rapid double-tap crashes)
    private var lastNavigationTime = 0L
    private val NAVIGATION_DEBOUNCE_MS = 500L
    
    // LIFECYCLE-SAFE: Nullable navigation callbacks that can be reassigned/cleared
    @Volatile
    var onNavigateToSettings: (() -> Unit)? = null
    
    @Volatile
    var onNavigateToEqualizer: (() -> Unit)? = null
    
    @Volatile
    var onNavigateToLanguage: (() -> Unit)? = null
    
    @android.webkit.JavascriptInterface
    fun onVolumeChanged(volume: Int) {
        val callback = onVolumeChange
        if (callback != null) {
            handler.post { callback(volume) }
        } else {
            android.util.Log.w("WebViewBridge", "Volume change blocked: callback is null (Activity destroyed?)")
        }
    }
    
    @android.webkit.JavascriptInterface
    fun onSensitivityChanged(sensitivity: Int) {
        val callback = onSensitivityChange
        if (callback != null) {
            handler.post { callback(sensitivity) }
        } else {
            android.util.Log.w("WebViewBridge", "Sensitivity change blocked: callback is null (Activity destroyed?)")
        }
    }
    
    @android.webkit.JavascriptInterface
    fun toggleBoost() {
        val callback = onToggleBoost
        if (callback != null) {
            handler.post { callback() }
        } else {
            android.util.Log.w("WebViewBridge", "Boost toggle blocked: callback is null (Activity destroyed?)")
        }
    }
    
    @android.webkit.JavascriptInterface
    fun changeTheme(themeName: String) {
        val callback = onThemeChanged
        if (callback != null) {
            val theme = when (themeName) {
                "mehtap" -> com.soundboost.ui.theme.AppTheme.MEHTAP
                "fener" -> com.soundboost.ui.theme.AppTheme.FENER
                "sumi" -> com.soundboost.ui.theme.AppTheme.SUMI
                "monsoon" -> com.soundboost.ui.theme.AppTheme.MONSOON
                "murekkep" -> com.soundboost.ui.theme.AppTheme.MUREKKEP
                "mycel" -> com.soundboost.ui.theme.AppTheme.MYCEL
                "reef" -> com.soundboost.ui.theme.AppTheme.REEF
                "aurora" -> com.soundboost.ui.theme.AppTheme.AURORA
                "nova" -> com.soundboost.ui.theme.AppTheme.AURORA  // Nova maps to Aurora
                "divit" -> com.soundboost.ui.theme.AppTheme.DIVIT
                "col" -> com.soundboost.ui.theme.AppTheme.COL
                "orman" -> com.soundboost.ui.theme.AppTheme.ORMAN
                "eyes" -> com.soundboost.ui.theme.AppTheme.EYES
                else -> com.soundboost.ui.theme.AppTheme.MEHTAP
            }
            handler.post { callback(theme) }
        } else {
            android.util.Log.w("WebViewBridge", "Theme change blocked: callback is null (Activity destroyed?)")
        }
    }
    
    @android.webkit.JavascriptInterface
    fun onModeChanged(mode: String) {
        android.util.Log.d("WebViewBridge", "Mode changed from HTML: $mode")
        val callback = this.onModeChanged  // Shadow warning fix
        if (callback != null) {
            val darkMode = when (mode) {
                "dark" -> true
                "light" -> false
                "system" -> null
                else -> null
            }
            handler.post { callback(darkMode) }
        } else {
            android.util.Log.w("WebViewBridge", "Mode change blocked: callback is null (Activity destroyed?)")
        }
    }
    
    @android.webkit.JavascriptInterface
    fun navigateToHome() {
        // Already on home
    }
    
    @android.webkit.JavascriptInterface
    fun navigateToSettings() {
        // Debounce: Ignore if called within 500ms of last navigation
        val now = System.currentTimeMillis()
        if (now - lastNavigationTime < NAVIGATION_DEBOUNCE_MS) {
            android.util.Log.w("WebViewBridge", "Navigation to settings debounced (rapid tap)")
            return
        }
        lastNavigationTime = now
        
        val callback = onNavigateToSettings
        if (callback != null) {
            handler.post { callback() }
        } else {
            android.util.Log.w("WebViewBridge", "Navigation to settings blocked: callback is null (Activity destroyed?)")
        }
    }
    
    @android.webkit.JavascriptInterface
    fun navigateToEqualizer() {
        // Debounce: Ignore if called within 500ms of last navigation
        val now = System.currentTimeMillis()
        if (now - lastNavigationTime < NAVIGATION_DEBOUNCE_MS) {
            android.util.Log.w("WebViewBridge", "Navigation to equalizer debounced (rapid tap)")
            return
        }
        lastNavigationTime = now
        
        val callback = onNavigateToEqualizer
        if (callback != null) {
            handler.post { callback() }
        } else {
            android.util.Log.w("WebViewBridge", "Navigation to equalizer blocked: callback is null (Activity destroyed?)")
        }
    }
    
    @android.webkit.JavascriptInterface
    fun navigateToLanguage() {
        // Debounce: Ignore if called within 500ms of last navigation
        val now = System.currentTimeMillis()
        if (now - lastNavigationTime < NAVIGATION_DEBOUNCE_MS) {
            android.util.Log.w("WebViewBridge", "Navigation to language debounced (rapid tap)")
            return
        }
        lastNavigationTime = now
        
        val callback = onNavigateToLanguage
        if (callback != null) {
            handler.post { callback() }
        } else {
            android.util.Log.w("WebViewBridge", "Navigation to language blocked: callback is null (Activity destroyed?)")
        }
    }
    
    /**
     * Clear navigation callbacks only if they match the provided references
     * This prevents race condition where new composition's SideEffect runs before old composition's onDispose
     */
    fun clearNavigationCallbacksIfMatch(
        settings: (() -> Unit)?,
        equalizer: (() -> Unit)?,
        language: (() -> Unit)?
    ) {
        var cleared = false
        if (onNavigateToSettings === settings && settings != null) {
            android.util.Log.d("WebViewBridge", "🧹 Clearing settings callback (match)")
            onNavigateToSettings = null
            cleared = true
        }
        if (onNavigateToEqualizer === equalizer && equalizer != null) {
            android.util.Log.d("WebViewBridge", "🧹 Clearing equalizer callback (match)")
            onNavigateToEqualizer = null
            cleared = true
        }
        if (onNavigateToLanguage === language && language != null) {
            android.util.Log.d("WebViewBridge", "🧹 Clearing language callback (match)")
            onNavigateToLanguage = null
            cleared = true
        }
        if (!cleared) {
            android.util.Log.d("WebViewBridge", "⏭️ Skipping clear - callbacks already replaced by new composition")
        }
    }
    
    /**
     * Clear all navigation callbacks (called when Activity is destroyed)
     */
    fun clearNavigationCallbacks() {
        android.util.Log.d("WebViewBridge", "🧹 Clearing navigation callbacks")
        onNavigateToSettings = null
        onNavigateToEqualizer = null
        onNavigateToLanguage = null
    }
    
    /**
     * Update navigation callbacks (called when Activity is recreated)
     */
    fun updateNavigationCallbacks(
        settings: () -> Unit,
        equalizer: () -> Unit,
        language: () -> Unit
    ) {
        android.util.Log.d("WebViewBridge", "🔄 Updating navigation callbacks")
        onNavigateToSettings = settings
        onNavigateToEqualizer = equalizer
        onNavigateToLanguage = language
    }
    
    /**
     * Update ViewModel callbacks (called on every recomposition to prevent stale references)
     * CRITICAL: Prevents memory leak after Activity recreation with "Don't keep activities"
     */
    fun updateViewModelCallbacks(
        onVolumeChange: (Int) -> Unit,
        onSensitivityChange: (Int) -> Unit,
        onToggleBoost: () -> Unit,
        onThemeChanged: (com.soundboost.ui.theme.AppTheme) -> Unit,
        onModeChanged: (Boolean?) -> Unit
    ) {
        android.util.Log.d("WebViewBridge", "🔄 Updating ViewModel callbacks")
        this.onVolumeChange = onVolumeChange
        this.onSensitivityChange = onSensitivityChange
        this.onToggleBoost = onToggleBoost
        this.onThemeChanged = onThemeChanged
        this.onModeChanged = onModeChanged
    }
    
    /**
     * Clear ViewModel callbacks only if they match the provided references
     * This prevents race condition where new composition's SideEffect runs before old composition's onDispose
     */
    fun clearViewModelCallbacksIfMatch(
        volumeChange: ((Int) -> Unit)?,
        sensitivityChange: ((Int) -> Unit)?,
        toggleBoost: (() -> Unit)?,
        themeChanged: ((com.soundboost.ui.theme.AppTheme) -> Unit)?,
        modeChanged: ((Boolean?) -> Unit)?
    ) {
        var cleared = false
        if (onVolumeChange === volumeChange && volumeChange != null) {
            android.util.Log.d("WebViewBridge", "🧹 Clearing volume callback (match)")
            onVolumeChange = null
            cleared = true
        }
        if (onSensitivityChange === sensitivityChange && sensitivityChange != null) {
            android.util.Log.d("WebViewBridge", "🧹 Clearing sensitivity callback (match)")
            onSensitivityChange = null
            cleared = true
        }
        if (onToggleBoost === toggleBoost && toggleBoost != null) {
            android.util.Log.d("WebViewBridge", "🧹 Clearing toggle callback (match)")
            onToggleBoost = null
            cleared = true
        }
        if (onThemeChanged === themeChanged && themeChanged != null) {
            android.util.Log.d("WebViewBridge", "🧹 Clearing theme callback (match)")
            onThemeChanged = null
            cleared = true
        }
        if (this.onModeChanged === modeChanged && modeChanged != null) {
            android.util.Log.d("WebViewBridge", "🧹 Clearing mode callback (match)")
            this.onModeChanged = null
            cleared = true
        }
        if (!cleared) {
            android.util.Log.d("WebViewBridge", "⏭️ Skipping clear - ViewModel callbacks already replaced by new composition")
        }
    }
}
