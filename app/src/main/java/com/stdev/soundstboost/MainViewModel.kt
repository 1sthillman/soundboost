package com.stdev.soundstboost

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stdev.soundstboost.audio.AudioVisualizer
import com.stdev.soundstboost.audio.SystemAudioCapture
import com.stdev.soundstboost.data.BoostPreferences
import com.stdev.soundstboost.data.BoostSettings
import com.stdev.soundstboost.service.BoostForegroundService
import com.stdev.soundstboost.ui.theme.AppTheme
import com.stdev.soundstboost.ui.theme.ColorAccent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val prefs = BoostPreferences(application)
    private val audioCapture = SystemAudioCapture()
    private var captureJob: Job? = null
    
    val uiState: StateFlow<BoostSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, BoostSettings())
    
    private val _audioLevels = MutableStateFlow<FloatArray?>(null)
    val audioLevels: StateFlow<FloatArray?> = _audioLevels.asStateFlow()
    
    fun toggleBoost() {
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
    
    fun onMasterGainChanged(percent: Int) {
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
    
    fun onLanguageChanged(language: com.stdev.soundstboost.data.AppLanguage, context: android.content.Context) {
        com.stdev.soundstboost.data.LanguageManager.setLanguage(context, language)
    }
    
    fun getCurrentLanguage(context: android.content.Context): com.stdev.soundstboost.data.AppLanguage {
        return com.stdev.soundstboost.data.LanguageManager.getCurrentLanguage(context)
    }
    
    fun maximizeSystemVolume() {
        val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
            action = "MAXIMIZE_VOLUME"
        }
        getApplication<Application>().startService(intent)
    }
    
    private fun startAudioVisualization() {
        android.util.Log.d("MainViewModel", "startAudioVisualization called")
        captureJob?.cancel()
        captureJob = viewModelScope.launch {
            android.util.Log.d("MainViewModel", "Starting capture flow...")
            audioCapture.startCapture(32).collect { levels: FloatArray ->
                _audioLevels.value = levels
                android.util.Log.d("MainViewModel", "Received audio levels: ${levels.take(5).joinToString()}")
            }
        }
    }
    
    private fun stopAudioVisualization() {
        captureJob?.cancel()
        audioCapture.stopCapture()
        _audioLevels.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAudioVisualization()
    }
}
