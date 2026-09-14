package com.soundboost.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.soundboost.data.AudioDeviceType
import com.soundboost.data.DeviceProfile
import com.soundboost.data.DeviceProfilePreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Audio Device Monitor
 * 
 * Monitors audio output device changes (speaker, headphones, Bluetooth)
 * and automatically applies user's saved profiles for each device type.
 * 
 * PRIVACY COMPLIANT:
 * - Only detects device TYPE, not device identity
 * - No device names, MAC addresses, or pairing info accessed
 * - Uses standard Android AudioManager APIs
 */
class AudioDeviceMonitor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "AudioDeviceMonitor"
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val profilePrefs = DeviceProfilePreferences(context)
    
    private val _currentDeviceType = MutableStateFlow(AudioDeviceType.PHONE_SPEAKER)
    val currentDeviceType: StateFlow<AudioDeviceType> = _currentDeviceType.asStateFlow()
    
    private val _currentProfile = MutableStateFlow<DeviceProfile?>(null)
    val currentProfile: StateFlow<DeviceProfile?> = _currentProfile.asStateFlow()
    
    private var deviceCallback: AudioDeviceCallback? = null
    private var onProfileChangeCallback: ((DeviceProfile) -> Unit)? = null
    
    /**
     * Start monitoring audio device changes
     */
    fun startMonitoring(onProfileChange: (DeviceProfile) -> Unit) {
        this.onProfileChangeCallback = onProfileChange
        
        // Detect initial device
        updateCurrentDevice()
        
        // Register callback for device changes (Android M+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deviceCallback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                    Log.d(TAG, "🎧 Audio device added")
                    updateCurrentDevice()
                }
                
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                    Log.d(TAG, "🎧 Audio device removed")
                    updateCurrentDevice()
                }
            }
            
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
            Log.d(TAG, "✅ Device monitoring started")
        }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            deviceCallback?.let {
                audioManager.unregisterAudioDeviceCallback(it)
            }
        }
        deviceCallback = null
        onProfileChangeCallback = null
        Log.d(TAG, "⏹️ Device monitoring stopped")
    }
    
    /**
     * Detect current audio output device type
     */
    private fun updateCurrentDevice() {
        val deviceType = detectCurrentDeviceType()
        
        if (deviceType != _currentDeviceType.value) {
            Log.d(TAG, "📱 Device changed: ${_currentDeviceType.value} -> $deviceType")
            _currentDeviceType.value = deviceType
            
            // Load and apply profile for new device
            scope.launch(Dispatchers.IO) {
                val profile = profilePrefs.getProfile(deviceType)
                
                if (profile != null && profile.autoSwitch) {
                    Log.d(TAG, "✅ Auto-applying profile for $deviceType: boost=${profile.volumeBoostPercent}%")
                    _currentProfile.value = profile
                    onProfileChangeCallback?.invoke(profile)
                } else {
                    Log.d(TAG, "ℹ️ No profile found for $deviceType or auto-switch disabled")
                    _currentProfile.value = null
                }
            }
        }
    }
    
    /**
     * Detect current audio output device type
     * 
     * PRIVACY COMPLIANT:
     * - Only returns generic device TYPE enum
     * - Does NOT return device name, address, or identifier
     */
    private fun detectCurrentDeviceType(): AudioDeviceType {
        // Check for Bluetooth
        if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
            return AudioDeviceType.BLUETOOTH_DEVICE
        }
        
        // Check for wired headset
        if (audioManager.isWiredHeadsetOn) {
            return AudioDeviceType.WIRED_HEADSET
        }
        
        // Check using AudioDeviceInfo (Android M+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        return AudioDeviceType.BLUETOOTH_DEVICE
                    }
                    
                    AudioDeviceInfo.TYPE_WIRED_HEADSET,
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        return AudioDeviceType.WIRED_HEADSET
                    }
                    
                    AudioDeviceInfo.TYPE_USB_DEVICE,
                    AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                        return AudioDeviceType.USB_DEVICE
                    }
                }
            }
        }
        
        // Default to phone speaker
        return AudioDeviceType.PHONE_SPEAKER
    }
    
    /**
     * Get current device type name for display
     */
    fun getCurrentDeviceName(): String {
        return _currentDeviceType.value.getDisplayName()
    }
    
    /**
     * Save profile for current device type
     */
    suspend fun saveProfileForCurrentDevice(
        volumeBoost: Int,
        bassBoost: Int,
        virtualizer: Int,
        presetName: String?,
        autoSwitch: Boolean
    ) {
        val profile = DeviceProfile(
            deviceType = _currentDeviceType.value,
            volumeBoostPercent = volumeBoost,
            bassBoostPercent = bassBoost,
            virtualizerPercent = virtualizer,
            equalizerPresetName = presetName,
            autoSwitch = autoSwitch
        )
        
        profilePrefs.saveProfile(profile)
        _currentProfile.value = profile
        
        Log.d(TAG, "💾 Saved profile for ${_currentDeviceType.value}: $profile")
    }
    
    /**
     * Delete profile for current device type
     */
    suspend fun deleteProfileForCurrentDevice() {
        profilePrefs.deleteProfile(_currentDeviceType.value)
        _currentProfile.value = null
        
        Log.d(TAG, "🗑️ Deleted profile for ${_currentDeviceType.value}")
    }
    
    /**
     * Check if current device has a saved profile
     */
    suspend fun hasProfileForCurrentDevice(): Boolean {
        return profilePrefs.getProfile(_currentDeviceType.value) != null
    }
}
