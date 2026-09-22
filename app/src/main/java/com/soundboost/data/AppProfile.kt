package com.soundboost.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
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

private val Context.appProfilesDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_profiles")

/**
 * Per-App Audio Profile
 * Each app can have custom boost/EQ settings
 */
@Serializable
data class AppProfile(
    val packageName: String,
    val appName: String,
    val masterGainPercent: Int = 150,
    val maxGainDb: Int = 15,
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val eq10Band: List<Float> = List(10) { 0f },
    val presetName: String = "Flat",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * App Profile Manager
 * Manages per-app boost profiles
 */
class AppProfileManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AppProfileManager"
        private val PROFILES_KEY = stringPreferencesKey("app_profiles_json")
    }
    
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }
    
    /**
     * Get all app profiles
     */
    val profiles: Flow<List<AppProfile>> = context.appProfilesDataStore.data.map { prefs ->
        val jsonString = prefs[PROFILES_KEY] ?: "[]"
        try {
            json.decodeFromString<List<AppProfile>>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse profiles", e)
            emptyList()
        }
    }
    
    /**
     * Get profile for specific package
     */
    fun getProfileForPackage(packageName: String): Flow<AppProfile?> {
        return profiles.map { list ->
            list.find { it.packageName == packageName }
        }
    }
    
    /**
     * Save or update app profile
     */
    suspend fun saveProfile(profile: AppProfile) {
        context.appProfilesDataStore.edit { prefs ->
            val currentProfiles = try {
                val jsonString = prefs[PROFILES_KEY] ?: "[]"
                json.decodeFromString<List<AppProfile>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            // Remove existing profile for same package
            currentProfiles.removeAll { it.packageName == profile.packageName }
            
            // Add new profile
            currentProfiles.add(profile)
            
            // Save
            prefs[PROFILES_KEY] = json.encodeToString(currentProfiles)
            
            Log.d(TAG, "✅ Saved profile for ${profile.appName} (${profile.packageName})")
        }
    }
    
    /**
     * Delete app profile
     */
    suspend fun deleteProfile(packageName: String) {
        context.appProfilesDataStore.edit { prefs ->
            val currentProfiles = try {
                val jsonString = prefs[PROFILES_KEY] ?: "[]"
                json.decodeFromString<List<AppProfile>>(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            currentProfiles.removeAll { it.packageName == packageName }
            prefs[PROFILES_KEY] = json.encodeToString(currentProfiles)
            
            Log.d(TAG, "✅ Deleted profile for $packageName")
        }
    }
    
    /**
     * Get installed apps (for profile creation)
     * Returns ALL installed apps including system apps
     */
    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return apps
            .mapNotNull { app ->
                try {
                    AppInfo(
                        packageName = app.packageName,
                        appName = app.loadLabel(pm).toString(),
                        icon = try {
                            val drawable = app.loadIcon(pm)
                            if (drawable is BitmapDrawable) {
                                drawable.bitmap
                            } else {
                                val bitmap = Bitmap.createBitmap(
                                    drawable.intrinsicWidth.coerceAtLeast(1),
                                    drawable.intrinsicHeight.coerceAtLeast(1),
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = Canvas(bitmap)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                bitmap
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to load icon for ${app.packageName}", e)
                            null
                        }
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load app info for ${app.packageName}", e)
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Bitmap?
)
