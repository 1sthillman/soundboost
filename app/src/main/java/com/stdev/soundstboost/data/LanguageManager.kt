package com.stdev.soundstboost.data

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
    
    fun setLanguage(context: Context, language: AppLanguage) {
        // Save preference
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.code)
            .apply()
        
        // Apply language
        val localeList = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.code)
        }
        
        AppCompatDelegate.setApplicationLocales(localeList)
    }
    
    fun getCurrentLanguage(context: Context): AppLanguage {
        val code = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, AppLanguage.SYSTEM.code) ?: AppLanguage.SYSTEM.code
        return AppLanguage.fromCode(code)
    }
    
    fun getSystemLanguage(): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleListCompat.getAdjustedDefault()[0]
        } else {
            Locale.getDefault()
        }
        return locale?.language ?: "en"
    }
}
