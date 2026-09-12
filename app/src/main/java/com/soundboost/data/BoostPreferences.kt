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
    val maxGainDb: Int = 15,  // Maximum gain boost: 15dB default, up to 30dB
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    
    // Legacy 3-band EQ (kept for backwards compatibility)
    val eqLowGain: Float = 0f,
    val eqMidGain: Float = 0f,
    val eqHighGain: Float = 0f,
    
    // NEW: 10-Band Parametric EQ (31Hz - 16kHz)
    val eq31Hz: Float = 0f,
    val eq62Hz: Float = 0f,
    val eq125Hz: Float = 0f,
    val eq250Hz: Float = 0f,
    val eq500Hz: Float = 0f,
    val eq1kHz: Float = 0f,
    val eq2kHz: Float = 0f,
    val eq4kHz: Float = 0f,
    val eq8kHz: Float = 0f,
    val eq16kHz: Float = 0f,
    
    // Active preset
    val activePresetName: String = "Flat",
    
    // Custom presets (JSON serialized)
    val customPresetsJson: String = "[]",
    
    val vocalMusicBalance: Float = 0.5f,  // 0.0 = music only, 0.5 = balanced, 1.0 = vocal only
    val isCallEnhancementEnabled: Boolean = false,  // Voice call noise suppression + clarity boost
    val autoStartOnBoot: Boolean = false,
    val theme: AppTheme = AppTheme.MEHTAP,
    val colorAccent: ColorAccent = ColorAccent.MEHTAP_GOLD,
    val sensitivity: Int = 45,  // Visualizer sensitivity (0-100)
    val isDarkMode: Boolean? = null  // null = follow system, true/false = force mode
) {
    // Get current 10-band EQ as array
    fun get10BandEQ(): FloatArray {
        return floatArrayOf(
            eq31Hz, eq62Hz, eq125Hz, eq250Hz, eq500Hz,
            eq1kHz, eq2kHz, eq4kHz, eq8kHz, eq16kHz
        )
    }
}

class BoostPreferences(private val context: Context) {
    
    private object Keys {
        val IS_BOOST_ENABLED = booleanPreferencesKey("is_boost_enabled")
        val MASTER_GAIN = intPreferencesKey("master_gain_percent")
        val MAX_GAIN_DB = intPreferencesKey("max_gain_db")
        val BASS_BOOST = intPreferencesKey("bass_boost_percent")
        val VIRTUALIZER = intPreferencesKey("virtualizer_percent")
        
        // Legacy 3-band
        val EQ_LOW = floatPreferencesKey("eq_low_gain")
        val EQ_MID = floatPreferencesKey("eq_mid_gain")
        val EQ_HIGH = floatPreferencesKey("eq_high_gain")
        
        // NEW: 10-band parametric EQ
        val EQ_31HZ = floatPreferencesKey("eq_31hz")
        val EQ_62HZ = floatPreferencesKey("eq_62hz")
        val EQ_125HZ = floatPreferencesKey("eq_125hz")
        val EQ_250HZ = floatPreferencesKey("eq_250hz")
        val EQ_500HZ = floatPreferencesKey("eq_500hz")
        val EQ_1KHZ = floatPreferencesKey("eq_1khz")
        val EQ_2KHZ = floatPreferencesKey("eq_2khz")
        val EQ_4KHZ = floatPreferencesKey("eq_4khz")
        val EQ_8KHZ = floatPreferencesKey("eq_8khz")
        val EQ_16KHZ = floatPreferencesKey("eq_16khz")
        
        val ACTIVE_PRESET = stringPreferencesKey("active_preset_name")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
        
        val VOCAL_MUSIC_BALANCE = floatPreferencesKey("vocal_music_balance")
        val IS_CALL_ENHANCEMENT_ENABLED = booleanPreferencesKey("is_call_enhancement_enabled")
        val AUTO_START = booleanPreferencesKey("auto_start_on_boot")
        val THEME = stringPreferencesKey("app_theme")
        val COLOR_ACCENT = stringPreferencesKey("color_accent")
        val HAS_RATED_APP = booleanPreferencesKey("has_rated_app")
        val SHOULD_SHOW_RATE_DIALOG = booleanPreferencesKey("should_show_rate_dialog")
        val SENSITIVITY = intPreferencesKey("sensitivity")
        val IS_DARK_MODE = stringPreferencesKey("is_dark_mode")
        val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
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
            masterGainPercent = prefs[Keys.MASTER_GAIN] ?: 150,
            maxGainDb = prefs[Keys.MAX_GAIN_DB] ?: 15,
            bassBoostPercent = prefs[Keys.BASS_BOOST] ?: 0,
            virtualizerPercent = prefs[Keys.VIRTUALIZER] ?: 0,
            
            // Legacy 3-band
            eqLowGain = prefs[Keys.EQ_LOW] ?: 0f,
            eqMidGain = prefs[Keys.EQ_MID] ?: 0f,
            eqHighGain = prefs[Keys.EQ_HIGH] ?: 0f,
            
            // NEW: 10-band
            eq31Hz = prefs[Keys.EQ_31HZ] ?: 0f,
            eq62Hz = prefs[Keys.EQ_62HZ] ?: 0f,
            eq125Hz = prefs[Keys.EQ_125HZ] ?: 0f,
            eq250Hz = prefs[Keys.EQ_250HZ] ?: 0f,
            eq500Hz = prefs[Keys.EQ_500HZ] ?: 0f,
            eq1kHz = prefs[Keys.EQ_1KHZ] ?: 0f,
            eq2kHz = prefs[Keys.EQ_2KHZ] ?: 0f,
            eq4kHz = prefs[Keys.EQ_4KHZ] ?: 0f,
            eq8kHz = prefs[Keys.EQ_8KHZ] ?: 0f,
            eq16kHz = prefs[Keys.EQ_16KHZ] ?: 0f,
            
            activePresetName = prefs[Keys.ACTIVE_PRESET] ?: "Flat",
            customPresetsJson = prefs[Keys.CUSTOM_PRESETS_JSON] ?: "[]",
            
            vocalMusicBalance = prefs[Keys.VOCAL_MUSIC_BALANCE] ?: 0.5f,
            isCallEnhancementEnabled = prefs[Keys.IS_CALL_ENHANCEMENT_ENABLED] ?: false,
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
    
    suspend fun setMaxGainDb(db: Int) {
        context.dataStore.edit { it[Keys.MAX_GAIN_DB] = db.coerceIn(15, 30) }
    }
    
    // NEW: Set 10-band EQ
    suspend fun set10BandEQ(bands: FloatArray) {
        require(bands.size == 10) { "Must provide exactly 10 band values" }
        context.dataStore.edit { prefs ->
            prefs[Keys.EQ_31HZ] = bands[0].coerceIn(-15f, 15f)
            prefs[Keys.EQ_62HZ] = bands[1].coerceIn(-15f, 15f)
            prefs[Keys.EQ_125HZ] = bands[2].coerceIn(-15f, 15f)
            prefs[Keys.EQ_250HZ] = bands[3].coerceIn(-15f, 15f)
            prefs[Keys.EQ_500HZ] = bands[4].coerceIn(-15f, 15f)
            prefs[Keys.EQ_1KHZ] = bands[5].coerceIn(-15f, 15f)
            prefs[Keys.EQ_2KHZ] = bands[6].coerceIn(-15f, 15f)
            prefs[Keys.EQ_4KHZ] = bands[7].coerceIn(-15f, 15f)
            prefs[Keys.EQ_8KHZ] = bands[8].coerceIn(-15f, 15f)
            prefs[Keys.EQ_16KHZ] = bands[9].coerceIn(-15f, 15f)
        }
    }
    
    suspend fun setActivePreset(presetName: String) {
        context.dataStore.edit { it[Keys.ACTIVE_PRESET] = presetName }
    }
    
    suspend fun setCustomPresetsJson(json: String) {
        context.dataStore.edit { it[Keys.CUSTOM_PRESETS_JSON] = json }
    }
    
    suspend fun setBoostEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_BOOST_ENABLED] = enabled }
    }
    
    suspend fun setMasterGain(percent: Int) {
        context.dataStore.edit { it[Keys.MASTER_GAIN] = percent.coerceIn(60, 500) }
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
    
    suspend fun setVocalMusicBalance(balance: Float) {
        context.dataStore.edit { 
            it[Keys.VOCAL_MUSIC_BALANCE] = balance.coerceIn(0f, 1f)
        }
    }
    
    suspend fun setCallEnhancement(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_CALL_ENHANCEMENT_ENABLED] = enabled }
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
    
    // Check if we should show rate dialog (sadece boost açılıp denemişse ve rated değilse göster)
    suspend fun shouldShowRateDialog(): Boolean {
        val prefs = context.dataStore.data
        val hasRated = prefs.map { it[Keys.HAS_RATED_APP] ?: false }
        val hasShownOnce = prefs.map { it[Keys.SHOULD_SHOW_RATE_DIALOG] ?: false }
        
        // İlk kez boost açıldığında bir kez göster, sonra tekrar gösterme
        return !hasRated.first() && !hasShownOnce.first()
    }
    
    // Mark that rate dialog has been shown
    suspend fun markRateDialogShown() {
        context.dataStore.edit { it[Keys.SHOULD_SHOW_RATE_DIALOG] = true }
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
    
    // Check if this is first launch
    suspend fun isFirstLaunch(): Boolean {
        val prefs = context.dataStore.data
        val completed = prefs.map { it[Keys.FIRST_LAUNCH_COMPLETED] ?: false }
        return !completed.first()
    }
    
    // Mark first launch as completed
    suspend fun setFirstLaunchCompleted() {
        context.dataStore.edit { it[Keys.FIRST_LAUNCH_COMPLETED] = true }
    }
}
