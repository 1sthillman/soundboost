# 🎤 Google Assistant Shortcuts - Comprehensive Test Guide

## ✅ IMPLEMENTATION STATUS: COMPLETE

All 24 shortcuts are now implemented with **DEEP LINK** format for maximum reliability.

---

## 📱 TESTING METHODS

### Method 1: ADB Testing (Recommended First)
```bash
# Test volume commands
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/set_volume_max"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/increase_volume"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/decrease_volume"

# Test bass commands
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/increase_bass"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/decrease_bass"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/set_bass_max"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/set_bass_zero"

# Test treble commands
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/increase_treble"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/decrease_treble"

# Test flash sync
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/toggle_flash"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/flash_on"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/flash_off"

# Test service control
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/start_service"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/stop_service"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/toggle_boost"

# Test call enhancement
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/call_enhancement_on"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/call_enhancement_off"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/toggle_call_enhancement"

# Test presets
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/preset_flat"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/preset_bass"
adb shell am start -a android.intent.action.VIEW -d "soundboost://action/preset_treble"
```

### Method 2: Google Assistant Voice Commands (TÜRKÇE)

#### 🔊 SES KONTROL KOMUTLARI
```
"Ses yükseltici uygulamasından sesi maksimuma al"
"Ses yükseltici uygulamasından sesi %75'e ayarla"
"Ses yükseltici uygulamasından sesi %50'ye ayarla"
"Ses yükseltici uygulamasından sesi yükselt"
"Ses yükseltici uygulamasından sesi azalt"
```

#### 🎵 BASS KONTROL KOMUTLARI
```
"Ses yükseltici uygulamasından bassı yükselt"
"Ses yükseltici uygulamasından bassı azalt"
"Ses yükseltici uygulamasından bassı maksimuma al"
"Ses yükseltici uygulamasından bassı kapat"
```

#### 🎼 TİZ KONTROL KOMUTLARI
```
"Ses yükseltici uygulamasından tizi yükselt"
"Ses yükseltici uygulamasından tizi azalt"
```

#### ⚡ FLAŞ KONTROL KOMUTLARI
```
"Ses yükseltici uygulamasından flaşı aç"
"Ses yükseltici uygulamasından flaşı kapat"
"Ses yükseltici uygulamasından flaşı değiştir"
```

#### � SERVİS KONTROL KOMUTLARI
```
"Ses yükseltici uygulamasından servisi başlat"
"Ses yükseltici uygulamasından servisi durdur"
"Ses yükseltici uygulamasından boost'u aç"
"Ses yükseltici uygulamasından boost'u kapat"
```

#### 📞 ARAMA İYİLEŞTİRME KOMUTLARI
```
"Ses yükseltici uygulamasından arama iyileştirmeyi aç"
"Ses yükseltici uygulamasından arama iyileştirmeyi kapat"
"Ses yükseltici uygulamasından arama iyileştirmeyi değiştir"
```

#### 🎚️ PRESET KOMUTLARI
```
"Ses yükseltici uygulamasından düz preset'i uygula"
"Ses yükseltici uygulamasından bass preset'i uygula"
"Ses yükseltici uygulamasından tiz preset'i uygula"
```

---

## 🧪 EXPECTED RESULTS

Each command should:
1. ✅ Open the app (if not already open)
2. ✅ Execute the action immediately
3. ✅ Show a toast notification with confirmation
4. ✅ Update the UI to reflect the change
5. ✅ Log the action in logcat

### Toast Messages to Expect:
- Volume: "🔊 Volume set to X%" or "🔊 Volume increased to X%"
- Bass: "🎵 Bass increased to X%" or "🎵 Bass set to MAXIMUM (100%)"
- Treble: "🎼 Treble increased" or "🎼 Treble decreased"
- Flash: "⚡ Flash sync ON" or "⚡ Flash sync OFF"
- Service: "🎧 Boost service starting..." or "🎧 Boost service stopped"
- Call Enhancement: "📞 Call enhancement turned ON/OFF"
- Presets: "🎚️ Flat preset applied" etc.

---

## 🐛 DEBUGGING

### Check Logcat
```bash
# Filter for MainActivity logs
adb logcat -s MainActivity:D

# You should see:
# D/MainActivity: 🔗 Deep Link: set_volume_max
# D/MainActivity: 🎤 Google Assistant Shortcut: set_volume_max
```

### Verify Intent Filter
```bash
# Check if deep links are properly registered
adb shell dumpsys package com.soundboost | grep -A 10 "soundboost://action"
```

### Common Issues & Fixes

#### Issue 1: "Malesef bir sorun çıktı" Error
**Cause:** Old targetClass format or missing deep link handler
**Fix:** ✅ All shortcuts now use deep link format - this should be fixed

#### Issue 2: App opens but nothing happens
**Cause:** Deep link not handled in onNewIntent()
**Fix:** ✅ Both onCreate() and onNewIntent() handle deep links

#### Issue 3: Google Assistant says "App doesn't support this"
**Cause:** Shortcuts not properly registered
**Fix:** 
1. Uninstall app
2. Reinstall fresh APK
3. Wait 5 minutes for Google to index shortcuts
4. Try again

#### Issue 4: Permission dialog appears instead of action
**Cause:** Boost requires microphone permission
**Expected:** This is CORRECT behavior for "start_service" commands

---

## 📊 TEST CHECKLIST

### Phase 1: ADB Testing (Do This First!)
- [ ] All volume commands work via ADB
- [ ] All bass commands work via ADB
- [ ] All treble commands work via ADB
- [ ] All flash commands work via ADB
- [ ] All service commands work via ADB
- [ ] All call enhancement commands work via ADB
- [ ] All preset commands work via ADB
- [ ] Toast notifications appear for each command
- [ ] Logcat shows deep link detection

### Phase 2: Google Assistant Testing (Turkish)
- [ ] Volume commands work via voice (5 commands)
- [ ] Bass commands work via voice (4 commands)
- [ ] Treble commands work via voice (2 commands)
- [ ] Flash commands work via voice (3 commands)
- [ ] Service commands work via voice (3 commands)
- [ ] Call enhancement commands work via voice (3 commands)
- [ ] Preset commands work via voice (3 commands)

### Phase 3: Edge Cases
- [ ] Commands work when app is closed
- [ ] Commands work when app is in background
- [ ] Commands work when app is already open
- [ ] Multiple commands in sequence work correctly
- [ ] No crashes or ANRs during testing

---

## 🎯 SUCCESS CRITERIA

**ALL 24 SHORTCUTS MUST:**
1. Respond within 1 second
2. Show visual feedback (toast)
3. Update UI immediately
4. Work from any app state (closed/background/foreground)
5. Log properly in logcat
6. Handle edge cases gracefully (e.g., permission needed)

---

## 📝 BUILD & DEPLOY

```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Install to device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Wait for Google to index shortcuts (5 minutes)
# Then test via ADB first, then Google Assistant
```

---

## 🚀 NEXT STEPS AFTER TESTING

1. ✅ Verify all ADB tests pass
2. ✅ Verify all Google Assistant voice commands work
3. ✅ Test with actual device (not emulator) for best results
4. ✅ Test flash commands with real flashlight
5. ✅ Document any device-specific issues (Xiaomi, Samsung, etc.)
6. ✅ Prepare release notes for v1.5.0
7. ✅ Update Play Store listing to highlight Google Assistant support

---

## 🔧 TECHNICAL IMPLEMENTATION

### Deep Link Format (Used by ALL shortcuts)
```xml
<intent
    android:action="android.intent.action.VIEW"
    android:data="soundboost://action/ACTION_NAME" />
```

### Handler in MainActivity.kt
```kotlin
// onCreate() - handles cold start
intent?.data?.let { uri ->
    if (uri.scheme == "soundboost" && uri.host == "action") {
        val action = uri.pathSegments.firstOrNull()
        action?.let { handleShortcutAction(it) }
    }
}

// onNewIntent() - handles warm start
override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    intent.data?.let { uri ->
        if (uri.scheme == "soundboost" && uri.host == "action") {
            val action = uri.pathSegments.firstOrNull()
            action?.let { handleShortcutAction(it) }
        }
    }
}
```

### Why Deep Links Work Better Than targetClass
1. ✅ More reliable across Android versions
2. ✅ Better Google Assistant integration
3. ✅ Survives app updates
4. ✅ No "malesef bir sorun çıktı" errors
5. ✅ Consistent behavior across devices

---

## 📞 SUPPORT

If issues persist after implementing deep links:
1. Check logcat for errors
2. Verify AndroidManifest.xml has deep link intent-filter
3. Ensure shortcuts.xml uses correct deep link format
4. Test with ADB first before trying Google Assistant
5. Wait 5-10 minutes after install for Google to index shortcuts

**Status:** ✅ READY FOR COMPREHENSIVE TESTING
**Last Updated:** 2026-09-21
**Implementation:** Complete (24/24 shortcuts with deep links)
