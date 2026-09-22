package com.soundboost.data

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Settings Backup & Restore
 * Enables exporting/importing app settings as JSON
 * Uses Storage Access Framework (no extra permissions needed)
 */

@Serializable
data class SettingsBackup(
    val version: Int = 1,
    val masterGainPercent: Int,
    val maxGainDb: Int,
    val bassBoostPercent: Int,
    val virtualizerPercent: Int,
    val eq10Band: List<Float>,  // 10 bands
    val activePresetName: String,
    val customPresetsJson: String,
    val vocalMusicBalance: Float,
    val isCallEnhancementEnabled: Boolean,
    val autoStartOnBoot: Boolean,
    val theme: String,
    val colorAccent: String,
    val sensitivity: Int,
    val isDarkMode: String  // "system", "dark", "light"
)

object SettingsBackupManager {
    private const val TAG = "SettingsBackup"
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * Export settings to JSON
     */
    fun exportSettings(context: Context, uri: Uri, settings: BoostSettings): Result<Unit> {
        return try {
            val backup = SettingsBackup(
                masterGainPercent = settings.masterGainPercent,
                maxGainDb = settings.maxGainDb,
                bassBoostPercent = settings.bassBoostPercent,
                virtualizerPercent = settings.virtualizerPercent,
                eq10Band = settings.get10BandEQ().toList(),
                activePresetName = settings.activePresetName,
                customPresetsJson = settings.customPresetsJson,
                vocalMusicBalance = settings.vocalMusicBalance,
                isCallEnhancementEnabled = settings.isCallEnhancementEnabled,
                autoStartOnBoot = settings.autoStartOnBoot,
                theme = settings.theme.name,
                colorAccent = settings.colorAccent.name,
                sensitivity = settings.sensitivity,
                isDarkMode = when (settings.isDarkMode) {
                    true -> "dark"
                    false -> "light"
                    null -> "system"
                }
            )
            
            val jsonString = json.encodeToString(backup)
            
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(jsonString.toByteArray())
            } ?: throw IOException("Could not open output stream")
            
            Log.d(TAG, "✅ Settings exported successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to export settings", e)
            Result.failure(e)
        }
    }
    
    /**
     * Import settings from JSON
     */
    fun importSettings(context: Context, uri: Uri): Result<SettingsBackup> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: throw IOException("Could not open input stream")
            
            val backup = json.decodeFromString<SettingsBackup>(jsonString)
            
            Log.d(TAG, "✅ Settings imported successfully")
            Result.success(backup)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to import settings", e)
            Result.failure(e)
        }
    }
    
    /**
     * Apply imported settings to preferences
     */
    suspend fun applyImportedSettings(prefs: BoostPreferences, backup: SettingsBackup) {
        try {
            prefs.setMasterGain(backup.masterGainPercent)
            prefs.setMaxGainDb(backup.maxGainDb)
            prefs.setBassBoost(backup.bassBoostPercent)
            prefs.setVirtualizer(backup.virtualizerPercent)
            prefs.set10BandEQ(backup.eq10Band.toFloatArray())
            prefs.setActivePreset(backup.activePresetName)
            prefs.setCustomPresetsJson(backup.customPresetsJson)
            prefs.setVocalMusicBalance(backup.vocalMusicBalance)
            prefs.setCallEnhancement(backup.isCallEnhancementEnabled)
            prefs.setAutoStart(backup.autoStartOnBoot)
            
            val theme = try { com.soundboost.ui.theme.AppTheme.valueOf(backup.theme) } catch (e: Exception) { com.soundboost.ui.theme.AppTheme.MEHTAP }
            prefs.setTheme(theme)
            
            val accent = try { com.soundboost.ui.theme.ColorAccent.valueOf(backup.colorAccent) } catch (e: Exception) { com.soundboost.ui.theme.ColorAccent.MEHTAP_GOLD }
            prefs.setColorAccent(accent)
            
            prefs.setSensitivity(backup.sensitivity)
            
            val isDarkMode = when (backup.isDarkMode) {
                "dark" -> true
                "light" -> false
                else -> null
            }
            prefs.setDarkMode(isDarkMode)
            
            Log.d(TAG, "✅ All settings applied successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to apply imported settings", e)
            throw e
        }
    }
}
