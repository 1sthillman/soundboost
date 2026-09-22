package com.soundboost.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.bluetoothProfilesDataStore: DataStore<Preferences> by preferencesDataStore(name = "bluetooth_profiles")

/**
 * Bluetooth Device Audio Profile
 * Remembers audio settings per Bluetooth device
 */
@Serializable
data class BluetoothDeviceProfile(
    val deviceAddress: String,  // MAC address (unique identifier)
    val deviceName: String,
    val deviceType: String,  // "HEADSET", "SPEAKER", "CAR", etc.
    val masterGainPercent: Int = 150,
    val maxGainDb: Int = 15,
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val eq10Band: List<Float> = List(10) { 0f },
    val presetName: String = "Flat",
    val autoApply: Boolean = true,  // Auto-apply when connected
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)

/**
 * Bluetooth Profile Manager
 * Auto-switches audio profiles when Bluetooth devices connect/disconnect
 */
class BluetoothProfileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothProfile"
        private val PROFILES_KEY = stringPreferencesKey("bluetooth_profiles_json")
    }
    
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }
    
    /**
     * Get all Bluetooth device profiles
     */
    val profiles: Flow<List<BluetoothDeviceProfile>> = context.bluetoothProfilesDataStore.data.map { prefs ->
        val jsonString = prefs[PROFILES_KEY] ?: "[]"
        try {
            json.decodeFromString<List<BluetoothDeviceProfile>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Bluetooth profiles", e)
            emptyList()
        }
    }
    
    /**
     * Get profile for specific device
     */
    fun getProfileForDevice(deviceAddress: String): Flow<BluetoothDeviceProfile?> {
        return profiles.map { list ->
            list.find { it.deviceAddress == deviceAddress }
        }
    }
    
    /**
     * Save or update Bluetooth device profile
     */
    suspend fun saveProfile(profile: BluetoothDeviceProfile) {
        context.bluetoothProfilesDataStore.edit { prefs ->
            val currentProfiles = try {
                val jsonString = prefs[PROFILES_KEY] ?: "[]"
                json.decodeFromString<List<BluetoothDeviceProfile>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            // Remove existing profile for same device
            currentProfiles.removeAll { it.deviceAddress == profile.deviceAddress }
            
            // Add new profile
            currentProfiles.add(profile.copy(lastUsed = System.currentTimeMillis()))
            
            // Save
            prefs[PROFILES_KEY] = json.encodeToString(currentProfiles)
            
            Log.d(TAG, "✅ Saved Bluetooth profile for ${profile.deviceName} (${profile.deviceAddress})")
        }
    }
    
    /**
     * Delete Bluetooth device profile
     */
    suspend fun deleteProfile(deviceAddress: String) {
        context.bluetoothProfilesDataStore.edit { prefs ->
            val currentProfiles = try {
                val jsonString = prefs[PROFILES_KEY] ?: "[]"
                json.decodeFromString<List<BluetoothDeviceProfile>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            currentProfiles.removeAll { it.deviceAddress == deviceAddress }
            prefs[PROFILES_KEY] = json.encodeToString(currentProfiles)
            
            Log.d(TAG, "✅ Deleted Bluetooth profile for $deviceAddress")
        }
    }
    
    /**
     * Update last used timestamp
     */
    suspend fun updateLastUsed(deviceAddress: String) {
        context.bluetoothProfilesDataStore.edit { prefs ->
            val currentProfiles = try {
                val jsonString = prefs[PROFILES_KEY] ?: "[]"
                json.decodeFromString<List<BluetoothDeviceProfile>>(jsonString).toMutableList()
            } catch (e: Exception) {
                return@edit
            }
            
            val index = currentProfiles.indexOfFirst { it.deviceAddress == deviceAddress }
            if (index != -1) {
                currentProfiles[index] = currentProfiles[index].copy(lastUsed = System.currentTimeMillis())
                prefs[PROFILES_KEY] = json.encodeToString(currentProfiles)
            }
        }
    }
}
