package com.soundboost.cache

import android.content.Context
import android.util.Log
import android.webkit.WebView
import com.soundboost.ui.theme.AppTheme
import com.soundboost.ui.theme.ColorAccent
import com.soundboost.ui.theme.getThemeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Preloads and caches all theme resources at app startup
 * to prevent lag/stutter when switching themes
 */
object ThemePreloader {
    
    private const val TAG = "ThemePreloader"
    private var isPreloaded = false
    private val htmlCache = mutableMapOf<String, String>()
    private val webViewCache = mutableMapOf<String, WebView>()
    
    /**
     * Call this once during app initialization (Application.onCreate or MainActivity.onCreate)
     * Preloads ALL themes and HTML visualizers into memory
     */
    suspend fun preloadAllThemes(context: Context) = withContext(Dispatchers.IO) {
        if (isPreloaded) {
            Log.d(TAG, "Already preloaded, skipping")
            return@withContext
        }
        
        Log.d(TAG, "Starting preload of all themes...")
        val startTime = System.currentTimeMillis()
        
        try {
            // 1. Preload all theme color combinations
            preloadThemeColors()
            
            // 2. Preload all HTML visualizer files
            preloadHtmlAssets(context)
            
            // 3. Optionally pre-initialize WebViews (careful with memory)
            // preloadWebViews(context)
            
            isPreloaded = true
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "✓ Completed in ${duration}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to preload themes", e)
        }
    }
    
    /**
     * Preload all theme color combinations into JVM memory
     * This ensures first access doesn't cause UI jank
     */
    private fun preloadThemeColors() {
        Log.d(TAG, "Preloading ${AppTheme.values().size} theme colors...")
        
        AppTheme.values().forEach { theme ->
            ColorAccent.values().forEach { accent ->
                // Access theme colors to trigger any lazy initialization
                val colors = getThemeColors(theme, accent)
                
                // Force color object creation to ensure they're in memory
                colors.primary
                colors.secondary
                colors.background
                colors.surface
                colors.onSurface
                colors.accent1
                colors.accent2
                colors.surfaceElevated
                colors.onSurfaceVariant
                colors.outline
            }
        }
        
        Log.d(TAG, "✓ Theme colors cached")
    }
    
    /**
     * Preload all HTML visualizer files from assets into memory
     * Prevents file I/O lag during theme switches
     */
    private suspend fun preloadHtmlAssets(context: Context) = withContext(Dispatchers.IO) {
        val htmlFiles = listOf(
            "awwardstheme.html",
            "eyes_visualizer.html",
            "mehtap_visualizer.html",
            "rezonans_visualizer.html"
        )
        
        Log.d(TAG, "Preloading ${htmlFiles.size} HTML files...")
        
        htmlFiles.forEach { filename ->
            try {
                val content = context.assets.open(filename).bufferedReader().use { it.readText() }
                htmlCache[filename] = content
                Log.d(TAG, "✓ Cached $filename (${content.length} bytes)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache $filename", e)
            }
        }
    }
    
    /**
     * Get cached HTML content (returns null if not cached)
     */
    fun getCachedHtml(filename: String): String? {
        return htmlCache[filename]
    }
    
    /**
     * Load HTML from cache or fallback to assets
     */
    suspend fun getHtmlContent(context: Context, filename: String): String = withContext(Dispatchers.IO) {
        htmlCache[filename] ?: run {
            Log.w(TAG, "Cache miss for $filename, loading from assets")
            context.assets.open(filename).bufferedReader().use { it.readText() }
        }
    }
    
    /**
     * Optional: Pre-initialize WebViews for each theme
     * WARNING: This uses significant memory (~10-20MB per WebView)
     * Only enable if you have memory to spare
     */
    private suspend fun preloadWebViews(context: Context) = withContext(Dispatchers.Main) {
        Log.d(TAG, "Pre-initializing WebViews (HIGH MEMORY usage)...")
        
        val htmlFiles = listOf(
            "awwardstheme.html",
            "eyes_visualizer.html",
            "mehtap_visualizer.html",
            "rezonans_visualizer.html"
        )
        
        htmlFiles.forEach { filename ->
            try {
                val webView = WebView(context.applicationContext)
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                }
                
                val html = htmlCache[filename]
                if (html != null) {
                    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    webViewCache[filename] = webView
                    Log.d(TAG, "✓ WebView initialized for $filename")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WebView for $filename", e)
            }
        }
    }
    
    /**
     * Get pre-initialized WebView (returns null if not cached)
     */
    fun getCachedWebView(filename: String): WebView? {
        return webViewCache[filename]
    }
    
    /**
     * Clear all caches (call when low memory warning received)
     */
    fun clearCache() {
        Log.d(TAG, "Clearing all caches")
        htmlCache.clear()
        webViewCache.values.forEach { it.destroy() }
        webViewCache.clear()
        isPreloaded = false
    }
    
    /**
     * Get memory usage estimate
     */
    fun getMemoryUsageKB(): Int {
        val htmlBytes = htmlCache.values.sumOf { it.length * 2 } // 2 bytes per char
        val webViewBytes = webViewCache.size * 15 * 1024 * 1024 // ~15MB per WebView estimate
        return (htmlBytes + webViewBytes) / 1024
    }
}
