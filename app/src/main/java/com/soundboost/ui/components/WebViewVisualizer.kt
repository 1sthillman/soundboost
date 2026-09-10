package com.soundboost.ui.components

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.soundboost.ui.theme.AppTheme

/**
 * WebView-based visualizer using the original HTML/CSS/JavaScript
 * This gives us PERFECT rendering matching the HTML exactly
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewVisualizer(
    theme: AppTheme,
    audioLevels: FloatArray?,
    isActive: Boolean,
    sensitivity: Int,
    modifier: Modifier = Modifier
) {
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
    
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                
                // Add interface for audio data
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun getAudioLevels(): String {
                        return audioLevels?.joinToString(",") ?: ""
                    }
                    
                    @JavascriptInterface
                    fun isPlaying(): Boolean {
                        return isActive
                    }
                    
                    @JavascriptInterface
                    fun getSensitivity(): Int {
                        return sensitivity
                    }
                }, "Android")
                
                // Load the HTML file from assets
                loadUrl("file:///android_asset/rezonans_visualizer.html")
            }
        },
        update = { webView ->
            // Update theme
            webView.evaluateJavascript("setTheme('$themeName');", null)
            
            // Update audio levels
            if (audioLevels != null) {
                val levelsJson = audioLevels.joinToString(",")
                webView.evaluateJavascript("updateAudioLevels([$levelsJson]);", null)
            }
            
            // Update sensitivity
            webView.evaluateJavascript("updateSensitivity($sensitivity);", null)
        }
    )
}
