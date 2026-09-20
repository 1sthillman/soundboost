package com.soundboost

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundboost.audio.AudioAnalysis
import com.soundboost.audio.BassFlashlightSync
import com.soundboost.audio.RealTimeAudioAnalyzer
import com.soundboost.data.BoostPreferences
import com.soundboost.data.BoostSettings
import com.soundboost.service.BoostForegroundService
import com.soundboost.ui.theme.AppTheme
import com.soundboost.ui.theme.ColorAccent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefs = BoostPreferences(application)
    private val audioAnalyzer = RealTimeAudioAnalyzer()
    private var analysisJob: Job? = null
    
    // Flash özelliği
    private val bassFlashSync = BassFlashlightSync(application)
    val isFlashEnabled = bassFlashSync.isEnabled
    val flashIntensity = bassFlashSync.intensity
    val flashBassLevel = bassFlashSync.bassLevel
    
    // NEW v1.4.0: Device Profile Monitor
    private val deviceMonitor = com.soundboost.audio.AudioDeviceMonitor(
        context = application,
        scope = viewModelScope
    )
    
    val uiState: StateFlow<BoostSettings> = prefs.settings
        .distinctUntilChanged()  // CRITICAL: Only emit when value actually changes
        .stateIn(viewModelScope, SharingStarted.Eagerly, BoostSettings())
    
    // NEW v1.4.0: Current audio device type
    val currentDeviceType = deviceMonitor.currentDeviceType
    
    // NEW v1.4.0: Current device profile
    val currentDeviceProfile = deviceMonitor.currentProfile
    
    // YENİ: Tam audio analiz verisi (SharedFlow for flash sync)
    private val _audioAnalysis = MutableSharedFlow<AudioAnalysis>(replay = 0, extraBufferCapacity = 1)
    val audioAnalysis: SharedFlow<AudioAnalysis> = _audioAnalysis.asSharedFlow()
    
    // LEGACY: Bar seviyeler (eski visualizer'lar için)
    val audioLevels: StateFlow<FloatArray?> = _audioAnalysis
        .map { it.bars }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    init {
        // Start device monitoring with auto-profile switching
        deviceMonitor.startMonitoring { profile ->
            android.util.Log.d("MainViewModel", "📱 Device profile changed: ${profile.deviceType}")
            applyDeviceProfile(profile)
        }
    }
    
    fun toggleBoost(onRequestPermission: (() -> Unit)? = null) {
        viewModelScope.launch {
            val current = uiState.value.isBoostEnabled
            val newState = !current
            android.util.Log.d("MainViewModel", "toggleBoost: $current -> $newState")
            
            prefs.setBoostEnabled(newState)
            
            val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                action = if (newState) "START_BOOST" else "STOP_BOOST"
            }
            getApplication<Application>().startService(intent)
            
            if (newState) {
                android.util.Log.d("MainViewModel", "Starting audio visualization...")
                startAudioVisualization()
            } else {
                android.util.Log.d("MainViewModel", "Stopping audio visualization...")
                stopAudioVisualization()
            }
        }
    }
    
    private var lastEmittedGain: Int? = null
    
    fun onMasterGainChanged(percent: Int) {
        // CRITICAL: Prevent duplicate emissions that cause recomposition
        if (lastEmittedGain == percent) {
            android.util.Log.v("MainViewModel", "⏭️ onMasterGainChanged: $percent (skipped, same as last)")
            return
        }
        
        lastEmittedGain = percent
        android.util.Log.d("MainViewModel", "🎚️ onMasterGainChanged: $percent")
        
        viewModelScope.launch {
            prefs.setMasterGain(percent)
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun onBassBoostChanged(percent: Int) {
        viewModelScope.launch {
            prefs.setBassBoost(percent)
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun onVirtualizerChanged(percent: Int) {
        viewModelScope.launch {
            prefs.setVirtualizer(percent)
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun onEqChanged(low: Float, mid: Float, high: Float) {
        viewModelScope.launch {
            prefs.setEqGains(low, mid, high)
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    // NEW: 10-Band EQ
    fun on10BandEqChanged(bands: FloatArray) {
        viewModelScope.launch {
            prefs.set10BandEQ(bands)
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun onPresetSelected(preset: com.soundboost.audio.EqualizerPreset) {
        viewModelScope.launch {
            prefs.set10BandEQ(preset.toBandArray())
            prefs.setActivePreset(preset.name)
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun onSaveCustomPreset(name: String, bands: FloatArray) {
        viewModelScope.launch {
            // Get current custom presets
            val current = uiState.value.customPresetsJson
            // TODO: Parse, add new preset, serialize back
            // For now, simple implementation
            val newPreset = com.soundboost.audio.EqualizerPreset.fromBandArray(name, name, bands, isCustom = true)
            // Save to preferences
            prefs.setActivePreset(name)
        }
    }
    
    fun onMaxGainChanged(db: Int) {
        viewModelScope.launch {
            prefs.setMaxGainDb(db)
        }
    }
    
    fun onVocalMusicBalanceChanged(balance: Float) {
        viewModelScope.launch {
            prefs.setVocalMusicBalance(balance)
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun onCallEnhancementToggled(enabled: Boolean) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "📞 Call Enhancement: $enabled")
            prefs.setCallEnhancement(enabled)
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
        }
    }
    
    fun onFlashToggled(enabled: Boolean) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "⚡ Flash Sync: $enabled")
            if (enabled && bassFlashSync.hasFlashSupport()) {
                // Start flash with shared audio analysis flow
                bassFlashSync.start(_audioAnalysis)
            } else {
                bassFlashSync.stop()
            }
        }
    }
    
    fun onFlashIntensityChanged(intensity: BassFlashlightSync.FlashIntensity) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "⚡ Flash Intensity: $intensity")
            bassFlashSync.setIntensity(intensity)
        }
    }
    
    fun hasFlashSupport(): Boolean = bassFlashSync.hasFlashSupport()
    
    fun onAutoStartToggled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setAutoStart(enabled)
        }
    }
    
    fun onThemeChanged(theme: AppTheme) {
        viewModelScope.launch {
            prefs.setTheme(theme)
        }
    }
    
    fun onColorAccentChanged(accent: ColorAccent) {
        viewModelScope.launch {
            prefs.setColorAccent(accent)
        }
    }
    
    fun onLanguageChanged(language: com.soundboost.data.AppLanguage, context: android.content.Context) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "🌐 Language changed to: ${language.code}")
            com.soundboost.data.LanguageManager.setLanguage(context, language)
            
            // CRITICAL: Notify UI that language changed
            // This triggers recomposition and WebView will sync in DisposableEffect
            android.util.Log.d("MainViewModel", "Language saved, UI will update via state flow")
        }
    }
    
    fun getCurrentLanguage(context: android.content.Context): com.soundboost.data.AppLanguage {
        return com.soundboost.data.LanguageManager.getCurrentLanguage(context)
    }
    
    fun maximizeSystemVolume() {
        val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
            action = "MAXIMIZE_VOLUME"
        }
        getApplication<Application>().startService(intent)
    }
    
    private fun startAudioVisualization() {
        android.util.Log.d("MainViewModel", "🎵 Starting professional audio analysis...")
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Collecting real-time audio data...")
            audioAnalyzer.startAnalysis().collect { analysis: AudioAnalysis ->
                _audioAnalysis.emit(analysis)
                
                // Log significant events
                if (analysis.isBeat) {
                    android.util.Log.d("MainViewModel", "🥁 Beat! Energy: ${analysis.energy}")
                }
            }
        }
    }
    
    private fun stopAudioVisualization() {
        android.util.Log.d("MainViewModel", "Stopping audio visualization")
        analysisJob?.cancel()
        audioAnalyzer.stopAnalysis()
    }
    
    fun onAppRated() {
        viewModelScope.launch {
            prefs.setHasRatedApp(true)
            prefs.markRateDialogShown()
        }
    }
    
    fun onRateLater() {
        viewModelScope.launch {
            prefs.markRateDialogShown()
        }
    }
    
    private var lastEmittedSensitivity: Int? = null
    
    fun onSensitivityChanged(value: Int) {
        // CRITICAL: Prevent duplicate emissions that cause recomposition
        if (lastEmittedSensitivity == value) {
            android.util.Log.v("MainViewModel", "⏭️ onSensitivityChanged: $value (skipped, same as last)")
            return
        }
        
        lastEmittedSensitivity = value
        android.util.Log.d("MainViewModel", "📊 onSensitivityChanged: $value")
        
        viewModelScope.launch {
            prefs.setSensitivity(value)
        }
    }
    
    fun onDarkModeChanged(isDark: Boolean?) {
        viewModelScope.launch {
            prefs.setDarkMode(isDark)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAudioVisualization()
        deviceMonitor.stopMonitoring()
        bassFlashSync.release()
    }
    
    /**
     * Apply device profile when audio device changes (v1.4.0)
     */
    private fun applyDeviceProfile(profile: com.soundboost.data.DeviceProfile) {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "🎯 Applying device profile: ${profile.deviceType}")
            
            // Apply profile settings
            prefs.setMasterGain(profile.volumeBoostPercent)
            prefs.setBassBoost(profile.bassBoostPercent)
            prefs.setVirtualizer(profile.virtualizerPercent)
            
            // Apply equalizer preset if available
            if (profile.equalizerPresetName != null) {
                try {
                    val presetName = profile.equalizerPresetName
                    val preset = com.soundboost.audio.EqualizerPreset.ALL_PRESETS
                        .firstOrNull { it.name == presetName }
                    
                    if (preset != null) {
                        onPresetSelected(preset)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("MainViewModel", "Failed to apply preset: ${profile.equalizerPresetName}")
                }
            }
            
            // Update service if boost is active
            if (uiState.value.isBoostEnabled) {
                val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                    action = "UPDATE_EFFECTS"
                }
                getApplication<Application>().startService(intent)
            }
            
            android.util.Log.d("MainViewModel", "✅ Device profile applied successfully")
        }
    }
    
    /**
     * Save current settings as device profile (v1.4.0)
     */
    fun onSaveCurrentAsDeviceProfile(autoSwitch: Boolean = true) {
        viewModelScope.launch {
            val settings = uiState.value
            
            deviceMonitor.saveProfileForCurrentDevice(
                volumeBoost = settings.masterGainPercent,
                bassBoost = settings.bassBoostPercent,
                virtualizer = settings.virtualizerPercent,
                presetName = settings.activePresetName,
                autoSwitch = autoSwitch
            )
            
            android.util.Log.d("MainViewModel", "💾 Saved profile for ${deviceMonitor.currentDeviceType.value}")
        }
    }
    
    /**
     * Delete profile for current device (v1.4.0)
     */
    fun onDeleteCurrentDeviceProfile() {
        viewModelScope.launch {
            deviceMonitor.deleteProfileForCurrentDevice()
            android.util.Log.d("MainViewModel", "🗑️ Deleted profile for ${deviceMonitor.currentDeviceType.value}")
        }
    }
    
    /**
     * Check if current device has a saved profile (v1.4.0)
     */
    suspend fun hasProfileForCurrentDevice(): Boolean {
        return deviceMonitor.hasProfileForCurrentDevice()
    }
}