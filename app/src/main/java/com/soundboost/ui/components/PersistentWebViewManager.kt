package com.soundboost.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.soundboost.ui.theme.AppTheme

/**
 * PROFESSIONAL SOLUTION: Singleton WebView that survives navigation
 * 
 * Problem: WebView.onPause() completely freezes JavaScript execution
 * Solution: Never pause the WebView - keep it alive and rendering at all times
 * 
 * This manager:
 * 1. Creates ONE WebView instance per application lifecycle
 * 2. NEVER calls onPause() or pauseTimers()
 * 3. Keeps WebView rendering even when not visible
 * 4. Provides clean sync API for Kotlin ↔ HTML state
 */
@SuppressLint("StaticFieldLeak") // Application context is safe
object PersistentWebViewManager {
    private var webView: WebView? = null
    private var isInitialized = false
    
    // YENİ: Public accessor for onboarding
    val webViewInstance: WebView?
        get() = webView
    
    /**
     * Get or create the persistent WebView instance
     * This WebView will NEVER be destroyed or paused
     */
    fun getWebView(context: Context): WebView {
        if (webView == null) {
            android.util.Log.d("WebViewManager", "🆕 Creating persistent WebView")
            webView = createWebView(context.applicationContext)
        }
        return webView!!
    }
    
    /**
     * Initialize WebView with bridge and callbacks
     * Called once when WebView is first attached to MainActivity
     */
    @SuppressLint("SetJavaScriptEnabled")
    fun initialize(
        context: Context,
        onVolumeChange: (Int) -> Unit,
        onSensitivityChange: (Int) -> Unit,
        onToggleBoost: () -> Unit,
        onThemeChanged: (AppTheme) -> Unit,
        onModeChanged: (Boolean?) -> Unit,
        onNavigateToSettings: () -> Unit,
        onNavigateToEqualizer: () -> Unit,
        onNavigateToLanguage: () -> Unit
    ) {
        if (isInitialized) {
            android.util.Log.d("WebViewManager", "⚠️ Already initialized - skipping")
            return
        }
        
        android.util.Log.d("WebViewManager", "🔧 Initializing WebView with bridge")
        val wv = getWebView(context)
        
        // Add JavaScript interface for Kotlin ↔ HTML communication
        wv.addJavascriptInterface(
            WebViewBridge(
                onVolumeChange, onSensitivityChange, onToggleBoost,
                onThemeChanged = { themeName ->
                    val theme = when (themeName) {
                        "mehtap" -> AppTheme.MEHTAP
                        "sumi" -> AppTheme.SUMI
                        "aurora" -> AppTheme.AURORA
                        "fener" -> AppTheme.FENER
                        "orman" -> AppTheme.ORMAN
                        "eyes" -> AppTheme.EYES
                        "mycel" -> AppTheme.MYCEL
                        "reef" -> AppTheme.REEF
                        "monsoon" -> AppTheme.MONSOON
                        "murekkep" -> AppTheme.MUREKKEP
                        "col" -> AppTheme.COL
                        "divit" -> AppTheme.DIVIT
                        else -> AppTheme.MEHTAP
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
        
        // Load HTML only once
        wv.loadUrl("file:///android_asset/awwardstheme.html")
        
        isInitialized = true
    }
    
    /**
     * Sync all state from Kotlin to HTML
     * This is the SINGLE source of truth for state synchronization
     */
    fun syncState(
        isBoostEnabled: Boolean,
        masterGainPercent: Int,
        sensitivity: Int,
        theme: AppTheme,
        isDarkMode: Boolean,
        languageCode: String
    ) {
        val wv = webView ?: return
        
        val mode = if (isDarkMode) "dark" else "light"
        val themeName = when (theme) {
            AppTheme.MEHTAP -> "mehtap"
            AppTheme.SUMI -> "sumi"
            AppTheme.AURORA -> "aurora"
            AppTheme.FENER -> "fener"
            AppTheme.ORMAN -> "orman"
            AppTheme.EYES -> "eyes"
            AppTheme.MYCEL -> "mycel"
            AppTheme.REEF -> "reef"
            AppTheme.MONSOON -> "monsoon"
            AppTheme.MUREKKEP -> "murekkep"
            AppTheme.COL -> "col"
            AppTheme.DIVIT -> "divit"
        }
        
        android.util.Log.d("WebViewManager", "🔄 Sync: boost=$isBoostEnabled, lang=$languageCode, mode=$mode, theme=$themeName")
        
        // Execute sync in a single JavaScript block for atomicity
        wv.evaluateJavascript("""
            (function() {
                console.log('🔥 KOTLIN SYNC: boost=$isBoostEnabled, lang=$languageCode, mode=$mode, theme=$themeName');
                
                // Update play button state
                if(typeof window.updatePlayButtonState === 'function') {
                    window.updatePlayButtonState($isBoostEnabled);
                } else {
                    console.error('❌ updatePlayButtonState not found');
                }
                
                // Update mode (dark/light)
                if(typeof window.setModeFromAndroid === 'function') {
                    window.setModeFromAndroid('$mode');
                } else {
                    console.error('❌ setModeFromAndroid not found');
                }
                
                // Update language
                if(typeof window.setLanguage === 'function') {
                    window.setLanguage('$languageCode');
                } else {
                    console.error('❌ setLanguage not found');
                }
                
                // Update volume
                if(typeof window.setVolumeFromKotlin === 'function') {
                    window.setVolumeFromKotlin($masterGainPercent);
                }
                
                // Update sensitivity
                if(document.getElementById('sens')) {
                    document.getElementById('sens').value = $sensitivity;
                    if(document.getElementById('sensVal')) {
                        document.getElementById('sensVal').textContent = $sensitivity;
                    }
                }
                
                // Update theme
                if(typeof window.setThemeFromAndroid === 'function') {
                    window.setThemeFromAndroid('$themeName');
                } else {
                    console.error('❌ setThemeFromAndroid not found');
                }
                
                console.log('✅ Sync complete');
                return 'OK';
            })();
        """.trimIndent()) { result ->
            android.util.Log.d("WebViewManager", "📊 Sync result: $result")
        }
    }
    
    /**
     * Update audio levels for visualization
     */
    fun updateAudioLevels(levels: FloatArray?) {
        val wv = webView ?: return
        levels?.let {
            val barsJson = it.joinToString(",")
            wv.evaluateJavascript(
                "if(typeof window.updateAudioLevels === 'function') { window.updateAudioLevels([$barsJson]); }",
                null
            )
        }
    }
    
    /**
     * Update full audio analysis data
     */
    fun updateAudioAnalysis(analysisJson: String) {
        val wv = webView ?: return
        wv.evaluateJavascript(
            "if(typeof window.updateRealAudioData === 'function') { window.updateRealAudioData('$analysisJson'); }",
            null
        )
    }
    
    /**
     * Create the WebView with optimal settings
     * CRITICAL: No onPause/pauseTimers - keeps JS running always
     */
    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context): WebView {
        return WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    android.util.Log.d("WebViewManager", "✅ Page loaded: $url")
                }
                
                // Block all navigation to prevent reload
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    android.util.Log.d("WebViewManager", "🚫 Blocking navigation: $url")
                    return true
                }
            }
            
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                    msg?.let {
                        android.util.Log.d("WebView-JS", "${it.message()} (${it.lineNumber()})")
                    }
                    return true
                }
            }
            
            // Enable JavaScript
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            
            // Disable cache for fresh state
            settings.cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
            
            // CRITICAL: Keep rendering priority high
            settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH)
            
            // Hardware acceleration for smooth animation
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            // CRITICAL: Keep WebView always visible to browser
            // This prevents JavaScript from being frozen
            visibility = android.view.View.VISIBLE
        }
    }
    
    /**
     * Check if WebView is ready to receive commands
     */
    fun isReady(): Boolean = isInitialized && webView != null
    
    /**
     * Clean up (only call when app is truly destroyed)
     */
    fun destroy() {
        android.util.Log.d("WebViewManager", "💀 Destroying WebView")
        webView?.destroy()
        webView = null
        isInitialized = false
    }
}
