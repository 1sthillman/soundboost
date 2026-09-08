package com.soundboost.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.soundboost.ui.theme.AppTheme
import com.soundboost.ui.theme.ColorAccent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "boost_settings")

data class BoostSettings(
    val isBoostEnabled: Boolean = false,
    val masterGainPercent: Int = 150,  // 150 = +10dB audible boost
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val eqLowGain: Float = 0f,
    val eqMidGain: Float = 0f,
    val eqHighGain: Float = 0f,
    val autoStartOnBoot: Boolean = false,
    val theme: AppTheme = AppTheme.MEHTAP,
    val colorAccent: ColorAccent = ColorAccent.MEHTAP_GOLD,
    val sensitivity: Int = 45,  // Visualizer sensitivity (0-100)
    val isDarkMode: Boolean? = null  // null = follow system, true/false = force mode
)

class BoostPreferences(private val context: Context) {
    
    private object Keys {
        val IS_BOOST_ENABLED = booleanPreferencesKey("is_boost_enabled")
        val MASTER_GAIN = intPreferencesKey("master_gain_percent")
        val BASS_BOOST = intPreferencesKey("bass_boost_percent")
        val VIRTUALIZER = intPreferencesKey("virtualizer_percent")
        val EQ_LOW = floatPreferencesKey("eq_low_gain")
        val EQ_MID = floatPreferencesKey("eq_mid_gain")
        val EQ_HIGH = floatPreferencesKey("eq_high_gain")
        val AUTO_START = booleanPreferencesKey("auto_start_on_boot")
        val THEME = stringPreferencesKey("app_theme")
        val COLOR_ACCENT = stringPreferencesKey("color_accent")
        val HAS_RATED_APP = booleanPreferencesKey("has_rated_app")  // Play Store'a gittiyse true
        val SHOULD_SHOW_RATE_DIALOG = booleanPreferencesKey("should_show_rate_dialog")  // Her açılışta true olur
        val SENSITIVITY = intPreferencesKey("sensitivity")
        val IS_DARK_MODE = stringPreferencesKey("is_dark_mode")  // "system", "light", "dark"
    }
    
    val settings: Flow<BoostSettings> = context.dataStore.data.map { prefs ->
        val darkModeStr = prefs[Keys.IS_DARK_MODE] ?: "system"
        val isDarkMode = when (darkModeStr) {
            "dark" -> true
            "light" -> false
            else -> null  // "system" or not set
        }
        
        BoostSettings(
            isBoostEnabled = prefs[Keys.IS_BOOST_ENABLED] ?: false,
            masterGainPercent = prefs[Keys.MASTER_GAIN] ?: 150,  // Default to +10dB
            bassBoostPercent = prefs[Keys.BASS_BOOST] ?: 0,
            virtualizerPercent = prefs[Keys.VIRTUALIZER] ?: 0,
            eqLowGain = prefs[Keys.EQ_LOW] ?: 0f,
            eqMidGain = prefs[Keys.EQ_MID] ?: 0f,
            eqHighGain = prefs[Keys.EQ_HIGH] ?: 0f,
            autoStartOnBoot = prefs[Keys.AUTO_START] ?: false,
            theme = try {
                AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.MEHTAP.name)
            } catch (e: Exception) {
                AppTheme.MEHTAP
            },
            colorAccent = try {
                ColorAccent.valueOf(prefs[Keys.COLOR_ACCENT] ?: ColorAccent.MEHTAP_GOLD.name)
            } catch (e: Exception) {
                ColorAccent.MEHTAP_GOLD
            },
            sensitivity = prefs[Keys.SENSITIVITY] ?: 45,
            isDarkMode = isDarkMode
        )
    }
    
    suspend fun setBoostEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_BOOST_ENABLED] = enabled }
    }
    
    suspend fun setMasterGain(percent: Int) {
        context.dataStore.edit { it[Keys.MASTER_GAIN] = percent.coerceIn(60, 200) }
    }
    
    suspend fun setBassBoost(percent: Int) {
        context.dataStore.edit { it[Keys.BASS_BOOST] = percent.coerceIn(0, 100) }
    }
    
    suspend fun setVirtualizer(percent: Int) {
        context.dataStore.edit { it[Keys.VIRTUALIZER] = percent.coerceIn(0, 100) }
    }
    
    suspend fun setEqGains(low: Float, mid: Float, high: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EQ_LOW] = low.coerceIn(-15f, 15f)
            prefs[Keys.EQ_MID] = mid.coerceIn(-15f, 15f)
            prefs[Keys.EQ_HIGH] = high.coerceIn(-15f, 15f)
        }
    }
    
    suspend fun setAutoStart(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_START] = enabled }
    }
    
    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }
    
    suspend fun setColorAccent(accent: ColorAccent) {
        context.dataStore.edit { it[Keys.COLOR_ACCENT] = accent.name }
    }
    
    suspend fun setHasRatedApp(rated: Boolean) {
        context.dataStore.edit { it[Keys.HAS_RATED_APP] = rated }
    }
    
    // Check if we should show rate dialog (her açılışta göster, SADECE rated ise gösterme)
    suspend fun shouldShowRateDialog(): Boolean {
        val prefs = context.dataStore.data
        val hasRated = prefs.map { it[Keys.HAS_RATED_APP] ?: false }
        return !hasRated.first()  // Eğer değerlendirme YAPILMADIYSA true döner
    }
    
    // "Daha sonra" butonuna basıldığında - hiçbir şey kaydetme, sadece dismiss
    suspend fun onRateLater() {
        // Hiçbir şey kaydetme - bir sonraki açılışta tekrar gösterecek
    }
    
    // "Değerlendir" butonuna basıldığında - bir daha gösterme
    suspend fun onRatedInStore() {
        context.dataStore.edit { it[Keys.HAS_RATED_APP] = true }
    }
    
    suspend fun setSensitivity(value: Int) {
        context.dataStore.edit { it[Keys.SENSITIVITY] = value.coerceIn(0, 100) }
    }
    
    suspend fun setDarkMode(isDark: Boolean?) {
        context.dataStore.edit { 
            it[Keys.IS_DARK_MODE] = when (isDark) {
                true -> "dark"
                false -> "light"
                null -> "system"
            }
        }
    }
}
