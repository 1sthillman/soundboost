package com.soundboost

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundboost.audio.AudioAnalysis
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
    private val audioAnalyzer = RealTimeAudioAnalyzer() // YENİ: Profesyonel analyzer
    private var analysisJob: Job? = null
    
    val uiState: StateFlow<BoostSettings> = prefs.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, BoostSettings())
    
    // YENİ: Tam audio analiz verisi
    private val _audioAnalysis = MutableStateFlow<AudioAnalysis?>(null)
    val audioAnalysis: StateFlow<AudioAnalysis?> = _audioAnalysis.asStateFlow()
    
    // LEGACY: Bar seviyeler (eski visualizer'lar için)
    val audioLevels: StateFlow<FloatArray?> = _audioAnalysis
        .map { it?.bars }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
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
    
    fun onLanguageChanged(language: com.soundboost.data.AppLanguage, context: android.content.Context) {
        viewModelScope.launch {
            com.soundboost.data.LanguageManager.setLanguage(context, language)
            // Language manager already handles recreation
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
                _audioAnalysis.value = analysis
                
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
        _audioAnalysis.value = null
    }
    
    fun onAppRated() {
        viewModelScope.launch {
            prefs.setHasRatedApp(true)
        }
    }
    
    fun onSensitivityChanged(value: Int) {
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
    }
}