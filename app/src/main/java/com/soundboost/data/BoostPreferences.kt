package com.soundboost.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.soundboost.ui.theme.AppTheme
import com.soundboost.ui.theme.ColorAccent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "boost_settings")

data class BoostSettings(
    val isBoostEnabled: Boolean = false,
    val masterGainPercent: Int = 100,
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val eqLowGain: Float = 0f,
    val eqMidGain: Float = 0f,
    val eqHighGain: Float = 0f,
    val autoStartOnBoot: Boolean = false,
    val theme: AppTheme = AppTheme.NEON_DARK,
    val colorAccent: ColorAccent = ColorAccent.CYAN
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
    }
    
    val settings: Flow<BoostSettings> = context.dataStore.data.map { prefs ->
        BoostSettings(
            isBoostEnabled = prefs[Keys.IS_BOOST_ENABLED] ?: false,
            masterGainPercent = prefs[Keys.MASTER_GAIN] ?: 100,
            bassBoostPercent = prefs[Keys.BASS_BOOST] ?: 0,
            virtualizerPercent = prefs[Keys.VIRTUALIZER] ?: 0,
            eqLowGain = prefs[Keys.EQ_LOW] ?: 0f,
            eqMidGain = prefs[Keys.EQ_MID] ?: 0f,
            eqHighGain = prefs[Keys.EQ_HIGH] ?: 0f,
            autoStartOnBoot = prefs[Keys.AUTO_START] ?: false,
            theme = try {
                AppTheme.valueOf(prefs[Keys.THEME] ?: AppTheme.NEON_DARK.name)
            } catch (e: Exception) {
                AppTheme.NEON_DARK
            },
            colorAccent = try {
                ColorAccent.valueOf(prefs[Keys.COLOR_ACCENT] ?: ColorAccent.CYAN.name)
            } catch (e: Exception) {
                ColorAccent.CYAN
            }
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
}
