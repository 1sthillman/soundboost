package com.soundboost.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding")

class OnboardingPreferences(private val context: Context) {
    
    companion object {
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val PLAY_BUTTON_SHOWN = booleanPreferencesKey("play_button_shown")
        private val SLIDERS_SHOWN = booleanPreferencesKey("sliders_shown")
        private val THEMES_SHOWN = booleanPreferencesKey("themes_shown")
        private val EQUALIZER_BADGE_SHOWN = booleanPreferencesKey("equalizer_badge_shown")
        private val SETTINGS_BADGE_SHOWN = booleanPreferencesKey("settings_badge_shown")
        private val CURRENT_STEP = intPreferencesKey("current_step")
    }
    
    val isOnboardingCompleted: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences -> preferences[ONBOARDING_COMPLETED] ?: false }
    
    val isPlayButtonShown: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences -> preferences[PLAY_BUTTON_SHOWN] ?: false }
    
    val isSlidersShown: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences -> preferences[SLIDERS_SHOWN] ?: false }
    
    val isThemesShown: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences -> preferences[THEMES_SHOWN] ?: false }
    
    val isEqualizerBadgeShown: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences -> preferences[EQUALIZER_BADGE_SHOWN] ?: false }
    
    val isSettingsBadgeShown: Flow<Boolean> = context.onboardingDataStore.data
        .map { preferences -> preferences[SETTINGS_BADGE_SHOWN] ?: false }
    
    val currentStep: Flow<Int> = context.onboardingDataStore.data
        .map { preferences -> preferences[CURRENT_STEP] ?: 0 }
    
    suspend fun completeOnboarding() {
        context.onboardingDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = true
        }
    }
    
    suspend fun markPlayButtonShown() {
        context.onboardingDataStore.edit { preferences ->
            preferences[PLAY_BUTTON_SHOWN] = true
        }
    }
    
    suspend fun markSlidersShown() {
        context.onboardingDataStore.edit { preferences ->
            preferences[SLIDERS_SHOWN] = true
        }
    }
    
    suspend fun markThemesShown() {
        context.onboardingDataStore.edit { preferences ->
            preferences[THEMES_SHOWN] = true
        }
    }
    
    suspend fun markEqualizerBadgeShown() {
        context.onboardingDataStore.edit { preferences ->
            preferences[EQUALIZER_BADGE_SHOWN] = true
        }
    }
    
    suspend fun markSettingsBadgeShown() {
        context.onboardingDataStore.edit { preferences ->
            preferences[SETTINGS_BADGE_SHOWN] = true
        }
    }
    
    suspend fun setCurrentStep(step: Int) {
        context.onboardingDataStore.edit { preferences ->
            preferences[CURRENT_STEP] = step
        }
    }
    
    suspend fun resetOnboarding() {
        context.onboardingDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
