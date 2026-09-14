package com.soundboost.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.deviceProfileStore: DataStore<Preferences> by preferencesDataStore(name = "device_profiles")

/**
 * Device Profile Storage Manager
 * 
 * PRIVACY COMPLIANT:
 * - Stores profiles per device TYPE, not per specific device
 * - No device names, MAC addresses, or identifiers stored
 * - All data stays local on device
 * - Deleted on app uninstall
 */
class DeviceProfilePreferences(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private object Keys {
        val PHONE_SPEAKER_PROFILE = stringPreferencesKey("profile_phone_speaker")
        val WIRED_HEADSET_PROFILE = stringPreferencesKey("profile_wired_headset")
        val BLUETOOTH_DEVICE_PROFILE = stringPreferencesKey("profile_bluetooth")
        val USB_DEVICE_PROFILE = stringPreferencesKey("profile_usb")
        val DEVICE_PROFILES_ENABLED = stringPreferencesKey("device_profiles_enabled")
    }
    
    /**
     * Get profile for specific device type
     */
    suspend fun getProfile(deviceType: AudioDeviceType): DeviceProfile? {
        val key = when (deviceType) {
            AudioDeviceType.PHONE_SPEAKER -> Keys.PHONE_SPEAKER_PROFILE
            AudioDeviceType.WIRED_HEADSET -> Keys.WIRED_HEADSET_PROFILE
            AudioDeviceType.BLUETOOTH_DEVICE -> Keys.BLUETOOTH_DEVICE_PROFILE
            AudioDeviceType.USB_DEVICE -> Keys.USB_DEVICE_PROFILE
            AudioDeviceType.UNKNOWN -> return null
        }
        
        val profileJson = context.deviceProfileStore.data.map { prefs ->
            prefs[key]
        }.first()
        
        return profileJson?.let { 
            try {
                json.decodeFromString<DeviceProfile>(it)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Save profile for specific device type
     */
    suspend fun saveProfile(profile: DeviceProfile) {
        val key = when (profile.deviceType) {
            AudioDeviceType.PHONE_SPEAKER -> Keys.PHONE_SPEAKER_PROFILE
            AudioDeviceType.WIRED_HEADSET -> Keys.WIRED_HEADSET_PROFILE
            AudioDeviceType.BLUETOOTH_DEVICE -> Keys.BLUETOOTH_DEVICE_PROFILE
            AudioDeviceType.USB_DEVICE -> Keys.USB_DEVICE_PROFILE
            AudioDeviceType.UNKNOWN -> return
        }
        
        val profileJson = json.encodeToString(profile)
        
        context.deviceProfileStore.edit { prefs ->
            prefs[key] = profileJson
        }
        
        android.util.Log.d("DeviceProfilePrefs", "✅ Saved profile for ${profile.deviceType}: $profileJson")
    }
    
    /**
     * Delete profile for specific device type
     */
    suspend fun deleteProfile(deviceType: AudioDeviceType) {
        val key = when (deviceType) {
            AudioDeviceType.PHONE_SPEAKER -> Keys.PHONE_SPEAKER_PROFILE
            AudioDeviceType.WIRED_HEADSET -> Keys.WIRED_HEADSET_PROFILE
            AudioDeviceType.BLUETOOTH_DEVICE -> Keys.BLUETOOTH_DEVICE_PROFILE
            AudioDeviceType.USB_DEVICE -> Keys.USB_DEVICE_PROFILE
            AudioDeviceType.UNKNOWN -> return
        }
        
        context.deviceProfileStore.edit { prefs ->
            prefs.remove(key)
        }
    }
    
    /**
     * Get all saved profiles
     */
    suspend fun getAllProfiles(): Map<AudioDeviceType, DeviceProfile> {
        val profiles = mutableMapOf<AudioDeviceType, DeviceProfile>()
        
        AudioDeviceType.values().filterNot { it == AudioDeviceType.UNKNOWN }.forEach { deviceType ->
            getProfile(deviceType)?.let { profile ->
                profiles[deviceType] = profile
            }
        }
        
        return profiles
    }
    
    /**
     * Check if device profiles feature is enabled
     */
    suspend fun isEnabled(): Boolean {
        return context.deviceProfileStore.data.map { prefs ->
            prefs[Keys.DEVICE_PROFILES_ENABLED]?.toBoolean() ?: true  // Enabled by default
        }.first()
    }
    
    /**
     * Enable/disable device profiles feature
     */
    suspend fun setEnabled(enabled: Boolean) {
        context.deviceProfileStore.edit { prefs ->
            prefs[Keys.DEVICE_PROFILES_ENABLED] = enabled.toString()
        }
    }
    
    /**
     * Flow of all profiles (for UI observation)
     */
    fun profilesFlow(): Flow<Map<AudioDeviceType, DeviceProfile>> {
        return context.deviceProfileStore.data.map { prefs ->
            val profiles = mutableMapOf<AudioDeviceType, DeviceProfile>()
            
            // Phone speaker
            prefs[Keys.PHONE_SPEAKER_PROFILE]?.let { jsonStr ->
                try {
                    profiles[AudioDeviceType.PHONE_SPEAKER] = json.decodeFromString(jsonStr)
                } catch (_: Exception) {}
            }
            
            // Wired headset
            prefs[Keys.WIRED_HEADSET_PROFILE]?.let { jsonStr ->
                try {
                    profiles[AudioDeviceType.WIRED_HEADSET] = json.decodeFromString(jsonStr)
                } catch (_: Exception) {}
            }
            
            // Bluetooth
            prefs[Keys.BLUETOOTH_DEVICE_PROFILE]?.let { jsonStr ->
                try {
                    profiles[AudioDeviceType.BLUETOOTH_DEVICE] = json.decodeFromString(jsonStr)
                } catch (_: Exception) {}
            }
            
            // USB
            prefs[Keys.USB_DEVICE_PROFILE]?.let { jsonStr ->
                try {
                    profiles[AudioDeviceType.USB_DEVICE] = json.decodeFromString(jsonStr)
                } catch (_: Exception) {}
            }
            
            profiles
        }
    }
}
