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
    private val onboardingPrefs = com.soundboost.data.OnboardingPreferences(application) // YENİ
    private val audioAnalyzer = RealTimeAudioAnalyzer() // YENİ: Profesyonel analyzer
    private var analysisJob: Job? = null
    
    val uiState: StateFlow<BoostSettings> = prefs.settings
        .distinctUntilChanged()  // CRITICAL: Only emit when value actually changes
        .stateIn(viewModelScope, SharingStarted.Eagerly, BoostSettings())
    
    // YENİ: Onboarding state
    val isOnboardingCompleted: StateFlow<Boolean> = onboardingPrefs.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    val currentOnboardingStep: StateFlow<Int> = onboardingPrefs.currentStep
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    
    // YENİ: Tam audio analiz verisi
    private val _audioAnalysis = MutableStateFlow<AudioAnalysis?>(null)
    val audioAnalysis: StateFlow<AudioAnalysis?> = _audioAnalysis.asStateFlow()
    
    // LEGACY: Bar seviyeler (eski visualizer'lar için)
    val audioLevels: StateFlow<FloatArray?> = _audioAnalysis
        .map { it?.bars }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    
    fun toggleBoost(onRequestPermission: (() -> Unit)? = null) {
        viewModelScope.launch {
            val current = uiState.value.isBoostEnabled
            val newState = !current
            android.util.Log.d("MainViewModel", "toggleBoost: $current -> $newState")
            
            // CRITICAL: Check microphone permission before starting boost
            if (newState && onRequestPermission != null) {
                // Boost açılıyorsa ve izin kontrolü callback'i varsa
                android.util.Log.d("MainViewModel", "🎤 Checking microphone permission before starting boost")
                onRequestPermission()
                // Gerçek boost başlatma onRequestPermission callback'inden yapılacak
                return@launch
            }
            
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
    
    // NEW: Internal function to start boost after permission granted
    fun startBoostAfterPermission() {
        viewModelScope.launch {
            android.util.Log.d("MainViewModel", "✅ Starting boost after permission granted")
            prefs.setBoostEnabled(true)
            
            val intent = Intent(getApplication(), BoostForegroundService::class.java).apply {
                action = "START_BOOST"
            }
            getApplication<Application>().startService(intent)
            
            android.util.Log.d("MainViewModel", "Starting audio visualization...")
            startAudioVisualization()
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
    
    // YENİ: Onboarding fonksiyonları
    fun onOnboardingStepComplete() {
        viewModelScope.launch {
            val nextStep = currentOnboardingStep.value + 1
            onboardingPrefs.setCurrentStep(nextStep)
        }
    }
    
    fun completeOnboarding() {
        viewModelScope.launch {
            onboardingPrefs.completeOnboarding()
        }
    }
    
    fun skipOnboarding() {
        viewModelScope.launch {
            onboardingPrefs.completeOnboarding()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAudioVisualization()
    }
}