package com.soundboost.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Flash-Sync'e özel tercihler. BoostPreferences.kt ile AYNI DataStore dosyasını
 * PAYLAŞMAZ — parti modu ayarları ses boost ayarlarından bağımsız yaşam döngüsüne
 * sahip, bu yüzden ayrı bir dosya (`sync_prefs`) kullanılıyor.
 */
private val Context.syncDataStore by preferencesDataStore(name = "sync_prefs")

class SyncPreferences(private val context: Context) {

    private object Keys {
        val HAS_ACCEPTED_FLASH_WARNING = booleanPreferencesKey("has_accepted_flash_warning")
        val TORCH_ENABLED = booleanPreferencesKey("torch_enabled")
        val LAST_DEVICE_NAME = stringPreferencesKey("last_device_name")
        val LAST_ROOM_NAME = stringPreferencesKey("last_room_name")
        val DEFAULT_FLASH_COLOR = stringPreferencesKey("default_flash_color")
    }

    /** Play Console content-rating ve etik gereklilik: epilepsi uyarısı sadece bir kez gösterilir. */
    val hasAcceptedFlashWarning: Flow<Boolean> = context.syncDataStore.data.map {
        it[Keys.HAS_ACCEPTED_FLASH_WARNING] ?: false
    }

    suspend fun setHasAcceptedFlashWarning(accepted: Boolean) {
        context.syncDataStore.edit { it[Keys.HAS_ACCEPTED_FLASH_WARNING] = accepted }
    }

    /** Torch varsayılan olarak KAPALI — ekran flaşı birincil, torch opsiyonel ikincil çıktı. */
    val torchEnabled: Flow<Boolean> = context.syncDataStore.data.map {
        it[Keys.TORCH_ENABLED] ?: false
    }

    suspend fun setTorchEnabled(enabled: Boolean) {
        context.syncDataStore.edit { it[Keys.TORCH_ENABLED] = enabled }
    }

    val lastDeviceName: Flow<String> = context.syncDataStore.data.map {
        it[Keys.LAST_DEVICE_NAME] ?: android.os.Build.MODEL
    }

    suspend fun setLastDeviceName(name: String) {
        context.syncDataStore.edit { it[Keys.LAST_DEVICE_NAME] = name }
    }

    val lastRoomName: Flow<String> = context.syncDataStore.data.map {
        it[Keys.LAST_ROOM_NAME] ?: ""
    }

    suspend fun setLastRoomName(name: String) {
        context.syncDataStore.edit { it[Keys.LAST_ROOM_NAME] = name }
    }

    /** NeonOrange varsayılan — design-system'in "ONE accent per screen" kuralına uyar. */
    val defaultFlashColor: Flow<String> = context.syncDataStore.data.map {
        it[Keys.DEFAULT_FLASH_COLOR] ?: "#FFFFFF"
    }

    suspend fun setDefaultFlashColor(hex: String) {
        context.syncDataStore.edit { it[Keys.DEFAULT_FLASH_COLOR] = hex }
    }
}
