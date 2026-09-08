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
import kotlinx.coroutines.delay

/**
 * CRITICAL FIX: Single source of truth pattern
 * - WebView is cached at activity level
 * - State syncs via single verified function with retry
 * - No multiple LaunchedEffects creating race conditions
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewHomeScreenNew(
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
    cachedWebView: androidx.compose.runtime.MutableState<WebView?>,
    currentLanguage: com.soundboost.data.AppLanguage
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val effectiveDarkMode = state.isDarkMode ?: systemInDarkTheme
    
    var isWebViewReady by remember { mutableStateOf(false) }
    var hasInitializedWebView by remember { mutableStateOf(false) }
    
    // CRITICAL: Track visibility to manage WebView lifecycle
    var isVisible by remember { mutableStateOf(true) }
    
    // Master sync function with verification and retry
    fun syncAllState(webView: WebView?) {
        if (webView == null || !isWebViewReady) return
        
        val langCode = when (currentLanguage.code) {
            "tr" -> "tr"; "en" -> "en"; "de" -> "de"; "fr" -> "fr"
            "es" -> "es"; "ru" -> "ru"; "ar" -> "ar"; "ja" -> "ja"
            "zh" -> "zh"; "ko" -> "ko"
            "system" -> com.soundboost.data.LanguageManager.getSystemLanguage(context)
            else -> "en"
        }
        
        val mode = if (effectiveDarkMode) "dark" else "light"
        val themeName = when (state.theme) {
            com.soundboost.ui.theme.AppTheme.MEHTAP -> "mehtap"
            com.soundboost.ui.theme.AppTheme.SUMI -> "sumi"
            com.soundboost.ui.theme.AppTheme.AURORA -> "aurora"
            com.soundboost.ui.theme.AppTheme.NOVA -> "nova"
            com.soundboost.ui.theme.AppTheme.MYCEL -> "mycel"
            com.soundboost.ui.theme.AppTheme.REEF -> "reef"
            com.soundboost.ui.theme.AppTheme.MONSOON -> "monsoon"
            com.soundboost.ui.theme.AppTheme.MUREKKEP -> "murekkep"
            com.soundboost.ui.theme.AppTheme.COL -> "col"
            com.soundboost.ui.theme.AppTheme.DIVIT -> "divit"
        }
        
        android.util.Log.d("WebView", "🔥 SYNC: lang=$langCode, boost=${state.isBoostEnabled}, mode=$mode, theme=$themeName")
        
        webView.evaluateJavascript("""
            (function() {
                console.log('🔥🔥🔥 KOTLIN SYNC START 🔥🔥🔥');
                console.log('Target: boost=${state.isBoostEnabled}, lang=$langCode, mode=$mode, theme=$themeName');
                
                if(typeof window.updatePlayButtonState === 'function') {
                    console.log('✅ Calling updatePlayButtonState(${state.isBoostEnabled})');
                    window.updatePlayButtonState(${state.isBoostEnabled});
                } else {
                    console.error('❌ window.updatePlayButtonState NOT FOUND');
                }
                
                if(typeof window.setModeFromAndroid === 'function') {
                    console.log('✅ Calling setModeFromAndroid($mode)');
                    window.setModeFromAndroid('$mode');
                } else {
                    console.error('❌ window.setModeFromAndroid NOT FOUND');
                }
                
                if(typeof window.setLanguage === 'function') {
                    console.log('✅ Calling setLanguage($langCode)');
                    window.setLanguage('$langCode');
                } else {
                    console.error('❌ window.setLanguage NOT FOUND');
                }
                
                if(typeof window.setVolumeFromKotlin === 'function') {
                    window.setVolumeFromKotlin(${state.masterGainPercent});
                } else {
                    console.error('❌ window.setVolumeFromKotlin NOT FOUND');
                }
                
                if(document.getElementById('sens')) {
                    document.getElementById('sens').value = ${state.sensitivity};
                    if(document.getElementById('sensVal')) {
                        document.getElementById('sensVal').textContent = ${state.sensitivity};
                    }
                }
                
                if(typeof window.setThemeFromAndroid === 'function') {
                    console.log('✅ Calling setThemeFromAndroid($themeName)');
                    window.setThemeFromAndroid('$themeName');
                } else {
                    console.error('❌ window.setThemeFromAndroid NOT FOUND');
                }
                
                console.log('✅✅✅ SYNC COMPLETE ✅✅✅');
                return 'SYNC_OK';
            })();
        """.trimIndent()) { result ->
            android.util.Log.d("WebView", "📊 Sync result: $result")
        }
    }
    
    // CRITICAL: When composable is visible, resume WebView
    DisposableEffect(Unit) {
        android.util.Log.d("WebView", "💚 Composable VISIBLE - resuming WebView")
        isVisible = true
        
        // Resume WebView's JavaScript and rendering
        cachedWebView.value?.onResume()
        cachedWebView.value?.resumeTimers()
        
        // Force sync state when becoming visible
        if (isWebViewReady && hasInitializedWebView) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                android.util.Log.d("WebView", "🔥 Visibility restored - force syncing")
                syncAllState(cachedWebView.value)
            }, 100)
        }
        
        onDispose {
            android.util.Log.d("WebView", "⚠️ Composable HIDDEN - pausing WebView")
            isVisible = false
            
            // CRITICAL: Pause but keep alive
            cachedWebView.value?.onPause()
            // DON'T call pauseTimers() - we want JS to keep running
        }
    }
    
    // On composition (including return from navigation)
    LaunchedEffect(Unit) {
        if (cachedWebView.value != null && isWebViewReady) {
            android.util.Log.d("WebView", "💚 Composable entered - FORCE syncing state")
            delay(100) // Ensure WebView is fully attached
            syncAllState(cachedWebView.value)
        }
    }
    
    // When ANY state changes - with MORE AGGRESSIVE sync
    LaunchedEffect(state.isBoostEnabled, state.masterGainPercent, state.sensitivity, state.theme, effectiveDarkMode, currentLanguage.code) {
        if (isWebViewReady && hasInitializedWebView) {
            android.util.Log.d("WebView", "🔄 State changed - AGGRESSIVE sync")
            delay(100)
            
            // Sync 3 times with delays to ensure it sticks
            repeat(3) { attempt ->
                android.util.Log.d("WebView", "🔄 Sync attempt ${attempt + 1}/3")
                syncAllState(cachedWebView.value)
                if (attempt < 2) delay(50)
            }
        }
    }
    
    // CRITICAL: Audio levels for visualization
    LaunchedEffect(audioLevels) {
        if (!isWebViewReady) return@LaunchedEffect
        audioLevels?.let { levels ->
            val barsJson = levels.joinToString(",")
            cachedWebView.value?.evaluateJavascript(
                "if(typeof window.updateAudioLevels === 'function') { window.updateAudioLevels([$barsJson]); }",
                null
            )
        }
    }
    
    // CRITICAL: Full audio analysis data
    LaunchedEffect(isWebViewReady) {
        if (!isWebViewReady) return@LaunchedEffect
        
        audioAnalysis.collect { analysis ->
            analysis?.let {
                val audioDataJson = """{"bars":[${it.bars.joinToString(",")}],"waveform":[${it.waveform.joinToString(",")}],"subBass":${it.subBass},"bass":${it.bass},"lowMid":${it.lowMid},"mid":${it.mid},"highMid":${it.highMid},"brilliance":${it.brilliance},"energy":${it.energy},"spectralFlux":${it.spectralFlux},"isBeat":${it.isBeat}}""".replace("\n", "")
                
                cachedWebView.value?.evaluateJavascript(
                    "if(typeof window.updateRealAudioData === 'function') { window.updateRealAudioData('$audioDataJson'); }",
                    null
                )
            }
        }
    }
    
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            android.util.Log.d("WebView", "🏗️ Factory called")
            
            cachedWebView.value ?: WebView(ctx).apply {
                android.util.Log.d("WebView", "🆕 Creating NEW WebView")
                
                // CRITICAL: Mark as initialized BEFORE setting webViewClient
                // This prevents reload from re-initializing
                hasInitializedWebView = true
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        
                        // CRITICAL: Only run ONCE ever, not on reload
                        if (isWebViewReady) {
                            android.util.Log.d("WebView", "⚠️ Page finished but already initialized - SKIPPING")
                            return
                        }
                        
                        isWebViewReady = true
                        android.util.Log.d("WebView", "📱 FIRST PAGE LOAD - initializing")
                        
                        // Initial sync
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            syncAllState(this@apply)
                        }, 100)
                    }
                    
                    // CRITICAL: Prevent any reload/refresh
                    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                        android.util.Log.d("WebView", "🚫 Blocking URL load: $url")
                        return true // Block all navigation
                    }
                }
                
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onConsoleMessage(msg: android.webkit.ConsoleMessage?): Boolean {
                        msg?.let { android.util.Log.d("WebView-JS", "${it.message()} (${it.lineNumber()})") }
                        return true
                    }
                }
                
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                
                // CRITICAL: Disable cache to prevent stale state
                settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                
                // CRITICAL: Keep WebView rendering and JS running
                settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
                
                // CRITICAL: Enable hardware acceleration
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                
                addJavascriptInterface(
                    WebViewBridge(
                        onVolumeChange, onSensitivityChange, onToggleBoost,
                        onThemeChanged = { themeName ->
                            val theme = when (themeName) {
                                "mehtap" -> com.soundboost.ui.theme.AppTheme.MEHTAP
                                "sumi" -> com.soundboost.ui.theme.AppTheme.SUMI
                                "aurora" -> com.soundboost.ui.theme.AppTheme.AURORA
                                "nova" -> com.soundboost.ui.theme.AppTheme.NOVA
                                "mycel" -> com.soundboost.ui.theme.AppTheme.MYCEL
                                "reef" -> com.soundboost.ui.theme.AppTheme.REEF
                                "monsoon" -> com.soundboost.ui.theme.AppTheme.MONSOON
                                "murekkep" -> com.soundboost.ui.theme.AppTheme.MUREKKEP
                                "col" -> com.soundboost.ui.theme.AppTheme.COL
                                "divit" -> com.soundboost.ui.theme.AppTheme.DIVIT
                                else -> com.soundboost.ui.theme.AppTheme.MEHTAP
                            }
                            onThemeChanged(theme)
                        },
                        onModeChanged = { mode ->
                            val isDarkMode = when (mode) {
                                "dark" -> true
                                "light" -> false
                                else -> null
                            }
                            onModeChanged(isDarkMode)
                        },
                        onNavigateToSettings, onNavigateToEqualizer, onNavigateToLanguage
                    ),
                    "AndroidBridge"
                )
                
                loadUrl("file:///android_asset/awwardstheme.html")
                cachedWebView.value = this
            }
        },
        update = { view ->
            // CRITICAL: Don't recreate, just ensure reference is saved
            if (cachedWebView.value == null) {
                cachedWebView.value = view
            }
            
            // CRITICAL: Resume WebView when update is called (means we're visible again)
            android.util.Log.d("WebView", "🔄 Update called - resuming WebView")
            view.onResume()
            view.resumeTimers()
            view.visibility = android.view.View.VISIBLE
            
            // Force a sync after WebView resumes
            if (isWebViewReady && hasInitializedWebView) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    android.util.Log.d("WebView", "🔥 Update complete - syncing state")
                    syncAllState(view)
                }, 150)
            }
        }
    )
}
