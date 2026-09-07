package com.soundboost.data

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.*

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("system", "Auto (System)"),
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    RUSSIAN("ru", "Русский"),
    CHINESE("zh", "中文"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    ARABIC("ar", "العربية");
    
    companion object {
        fun fromCode(code: String): AppLanguage {
            return values().find { it.code == code } ?: SYSTEM
        }
    }
}

object LanguageManager {
    
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    
    /**
     * Set application language and persist the choice.
     * Automatically detects system language when SYSTEM is selected.
     */
    fun setLanguage(context: Context, language: AppLanguage) {
        // Save preference
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
        
        // Apply language using AppCompat for proper system integration
        applyLanguageWithAppCompat(language)
        
        // Update configuration for immediate effect
        updateConfiguration(context, language)
        
        // CRITICAL FIX: Recreate activity to apply language changes immediately
        // This ensures all string resources are updated without app restart
        if (context is Activity) {
            android.util.Log.d("LanguageManager", "🔄 Recreating activity for immediate language change")
            context.recreate()
        }
    }
    
    /**
     * Apply language using AppCompatDelegate for system-level integration.
     * This ensures proper RTL support and system language detection.
     */
    private fun applyLanguageWithAppCompat(language: AppLanguage) {
        val localeList = if (language == AppLanguage.SYSTEM) {
            // Use system default - AppCompat will detect automatically
            LocaleListCompat.getEmptyLocaleList()
        } else {
            // Use specific language
            LocaleListCompat.forLanguageTags(language.code)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
    
    /**
     * Update configuration for immediate effect within the app.
     */
    private fun updateConfiguration(context: Context, language: AppLanguage) {
        val locale = getLocaleForLanguage(context, language)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        }
        
        // Update resources configuration
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
    
    /**
     * Get the appropriate Locale for a given language.
     * Automatically detects system locale when SYSTEM is selected.
     */
    private fun getLocaleForLanguage(context: Context, language: AppLanguage): Locale {
        return if (language == AppLanguage.SYSTEM) {
            // Detect system language properly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val systemLocales = context.resources.configuration.locales
                if (systemLocales.size() > 0) {
                    systemLocales[0]
                } else {
                    Locale.getDefault()
                }
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale ?: Locale.getDefault()
            }
        } else {
            Locale(language.code)
        }
    }
    
    /**
     * Apply saved language on app start.
     * Called from Application.onCreate()
     */
    fun applyLanguage(context: Context) {
        val language = getCurrentLanguage(context)
        
        // Apply using AppCompat
        applyLanguageWithAppCompat(language)
        
        // Update configuration
        updateConfiguration(context, language)
    }
    
    /**
     * Get currently selected language from preferences.
     */
    fun getCurrentLanguage(context: Context): AppLanguage {
        val code = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code
        return AppLanguage.fromCode(code)
    }
    
    /**
     * Get system language code for display purposes.
     */
    fun getSystemLanguage(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val systemLocales = context.resources.configuration.locales
            if (systemLocales.size() > 0) {
                systemLocales[0]
            } else {
                Locale.getDefault()
            }
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale ?: Locale.getDefault()
        }
        return locale.language
    }
    
    /**
     * Get display name for system language.
     */
    fun getSystemLanguageDisplayName(context: Context): String {
        val systemLangCode = getSystemLanguage(context)
        val matchingLanguage = AppLanguage.values().find { it.code == systemLangCode }
        return matchingLanguage?.displayName ?: "English"
    }
}
