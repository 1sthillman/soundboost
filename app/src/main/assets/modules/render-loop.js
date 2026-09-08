function render(){
  // Only resize on actual window size changes (not every frame)
  const now = Date.now();
  if(now - lastResize > 500) { // Check every 500ms max
    resize();
    lastResize = now;
  }
  
  t += 0.045;
  const sensBoost = 0.55 + (parseFloat(sensEl.value)/100)*1.1;
  const bars = getBars(sensBoost);
  const wave = getWave();
  const e = analyze(bars, sensBoost);
  updateMeter(bars);
  phone.style.setProperty('--lvl', playing || useRealAudioData ? Math.min(1, e.bass*1.3) : 0);
  const fn = RENDERERS[currentTheme] || drawSumi;
  fn(bars, wave, e);
  requestAnimationFrame(render);
}

setTheme('sumi');
sensVal.textContent = sensEl.value;
boostVal.textContent = boostEl.value;
resize();
updateUI(); // Initialize all i18n texts
render();

// External API for Android
window.updateAudioLevels = function(levels) {
  // Update visualization with external audio data
  if(Array.isArray(levels) && levels.length > 0) {
    // Could be used to inject real audio data from Android
  }
};

window.setBoostState = function(enabled) {
  // Update visual state only - no audio control
  // (Audio is controlled by Android, we just reflect the state)
  console.log('setBoostState called:', enabled, 'current playing:', playing);
  
  if(enabled !== playing) {
    updatePlayButtonState(enabled);
  }
};

// Map Kotlin volume value (60-200) to HTML slider value (0-100)
window.setVolumeFromKotlin = function(kotlinValue) {
  const clamped = Math.max(60, Math.min(200, kotlinValue));
  const htmlValue = Math.round((clamped - 60) / 140 * 100); // 60→0, 200→100
  if(boostEl) {
    boostEl.value = htmlValue;
    boostVal.textContent = htmlValue;
  }
  console.log('Volume from Kotlin:', kotlinValue, '→ HTML slider:', htmlValue);
};

window.setThemeFromAndroid = function(themeName) {
  if(i18n[currentLang].themes[themeName]) {
    setTheme(themeName);
  }
};

window.setLanguage = function(lang) {
  console.log('🌐 Language change requested:', lang);
  if(!i18n[lang]) {
    console.error('❌ Language not found:', lang);
    return;
  }
  
  // Validate lang has all required keys
  if(!i18n[lang].boost || !i18n[lang].themes) {
    console.error('❌ Incomplete translations for:', lang);
    return;
  }
  
  currentLang = lang;
  console.log('✅ Language set to:', lang);
  
  // Use requestAnimationFrame for smooth DOM updates
  requestAnimationFrame(() => {
    try {
      updateUI();
      console.log('✅ UI updated successfully');
    } catch(e) {
      console.error('❌ Error updating UI:', e);
    }
  });
};

function updateUI() {
  console.log('🔄 Updating UI for language:', currentLang);
  
  try {
    // 1. Update labels with safe checks
    const i18nElements = document.querySelectorAll('[data-i18n]');
    console.log('Found', i18nElements.length, 'i18n elements');
    
    i18nElements.forEach(el => {
      try {
        const key = el.dataset.i18n;
        if(key && i18n[currentLang] && i18n[currentLang][key]) {
          el.textContent = i18n[currentLang][key];
        }
      } catch(e) {
        console.warn('Failed to update element:', el, e);
      }
    });
    
    // 2. Update stage hint safely
    if(stageHint && i18n[currentLang] && i18n[currentLang].play_hint) {
      stageHint.textContent = i18n[currentLang].play_hint;
    }
    
    // 3. Update theme chips with safe checks
    const themeChips = document.querySelectorAll('[data-i18n-theme]');
    console.log('Found', themeChips.length, 'theme chips');
    
    themeChips.forEach(chip => {
      try {
        const theme = chip.dataset.i18nTheme;
        if(theme && i18n[currentLang] && i18n[currentLang].themes && i18n[currentLang].themes[theme]) {
          chip.textContent = i18n[currentLang].themes[theme].label;
        }
      } catch(e) {
        console.warn('Failed to update chip:', chip, e);
      }
    });
    
    // 4. Update current theme title/subtitle with animation
    if(currentTheme && i18n[currentLang] && i18n[currentLang].themes && i18n[currentLang].themes[currentTheme]) {
      const meta = i18n[currentLang].themes[currentTheme];
      
      // Smooth fade transition
      if(trackTitle && trackSub) {
        trackTitle.style.opacity = '0';
        trackSub.style.opacity = '0';
        
        setTimeout(() => {
          try {
            trackTitle.textContent = meta.title;
            trackSub.textContent = meta.sub;
            trackTitle.style.opacity = '1';
            trackSub.style.opacity = '1';
          } catch(e) {
            console.warn('Failed to update track info:', e);
          }
        }, 150);
      }
    }
    
    console.log('✅ UI update completed');
  } catch(e) {
    console.error('❌ Critical error in updateUI:', e);
    // Don't crash - just log and continue
  }
}

// Debug: Check if AndroidBridge is available
console.log('AndroidBridge available:', typeof AndroidBridge !== 'undefined');
if(typeof AndroidBridge !== 'undefined') {
  console.log('AndroidBridge methods:', Object.getOwnPropertyNames(Object.getPrototypeOf(AndroidBridge)));
}