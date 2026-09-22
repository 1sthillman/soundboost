# SoundSTBoost v1.5.0 - Build & Test Guide

## 🔧 Build Setup

### 1. Fix JAVA_HOME Issue

Current error suggests JAVA_HOME path has extra characters. Fix:

**Windows:**
```powershell
# Set correct JAVA_HOME (adjust path if needed)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot"

# Verify
echo $env:JAVA_HOME
java -version
```

**Permanent fix (System Environment Variables):**
1. Open System Properties → Advanced → Environment Variables
2. Edit JAVA_HOME: `C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot`
3. Remove any trailing characters/spaces
4. Restart terminal

### 2. Build Debug APK

```bash
./gradlew assembleDebug
```

**Expected output:**
```
BUILD SUCCESSFUL in 2m 15s
123 actionable tasks: 45 executed, 78 up-to-date
```

### 3. Build Signed Release AAB

```bash
./gradlew bundleRelease
```

**Output location:** `app/build/outputs/bundle/release/app-release.aab`

---

## 🧪 Testing Checklist

### Phase 1: Core Features (Existing)
- [ ] Master volume boost works (60%-500%)
- [ ] Bass boost works (0%-100%)
- [ ] 10-band EQ works
- [ ] Themes switch correctly
- [ ] Visualizer responds to music
- [ ] Notification controls work
- [ ] Service survives screen off
- [ ] Auto-start on boot (if enabled)

### Phase 2: Quick Settings Tile
- [ ] Add tile from Quick Settings panel
- [ ] Tap tile → boost toggles ON
- [ ] Tap tile again → boost toggles OFF
- [ ] Tile label updates (e.g., "Boost: 150%")
- [ ] Tile state syncs with app
- [ ] Long press tile → app opens

### Phase 3: Widget
- [ ] Long press home screen → Add Widget → SoundSTBoost
- [ ] Widget displays current boost state
- [ ] Widget shows volume percentage
- [ ] Tap ON button → boost activates
- [ ] Tap OFF button → boost deactivates
- [ ] Tap + button → volume increases (+20%)
- [ ] Tap − button → volume decreases (−20%)
- [ ] Tap anywhere else → app opens
- [ ] Widget updates when boost changes in app

### Phase 4: Settings Backup
**Export Test:**
- [ ] Open Settings → Backup & Restore
- [ ] Tap "Export Now"
- [ ] File picker opens
- [ ] Save as `SoundSTBoost_test.json`
- [ ] Open file in text editor → verify valid JSON
- [ ] Check all settings present (gain, bass, EQ, theme, etc.)

**Import Test:**
- [ ] Change several settings in app (volume, bass, theme)
- [ ] Tap "Import from File"
- [ ] Select previously exported JSON
- [ ] Verify all settings restored correctly
- [ ] Check: volume, bass, EQ bands, preset, theme, color

**Error Handling:**
- [ ] Try importing invalid JSON → shows error message
- [ ] Try importing corrupted file → graceful error

### Phase 5: Per-App Profiles
**Create Profile:**
- [ ] Open Settings → Per-App Profiles
- [ ] Tap "+" to add profile
- [ ] App list loads with icons
- [ ] Select an app (e.g., Spotify)
- [ ] Set custom Gain: 200%, Bass: 50%
- [ ] Select preset: "Electronic"
- [ ] Save profile

**Verify Profile:**
- [ ] Profile appears in list with app name + icon
- [ ] Shows Gain: 200%, Bass: 50%, Preset: Electronic
- [ ] Tap Edit → can modify settings
- [ ] Tap Delete → profile removed

**Auto-Switch (Optional - requires PACKAGE_USAGE_STATS):**
- [ ] Grant usage access permission
- [ ] Open Spotify → profile auto-applies
- [ ] Check notification: "Profile: Spotify (200%)"
- [ ] Switch to YouTube → reverts to default
- [ ] Switch back to Spotify → profile reapplies

### Phase 6: Bluetooth Profiles
**Create Profile:**
- [ ] Connect Bluetooth headphones/speaker
- [ ] Notification: "Remember settings for [Device Name]?"
- [ ] Tap "Yes"
- [ ] Set custom Gain: 180%, Bass: 40%
- [ ] Select preset: "Headphones"
- [ ] Save profile

**Verify Profile:**
- [ ] Open Settings → Bluetooth Profiles
- [ ] Device appears with correct name
- [ ] Shows device type icon (headphones/speaker/car)
- [ ] Shows Gain: 180%, Bass: 40%, Preset: Headphones
- [ ] Shows "Last used: [timestamp]"

**Auto-Apply:**
- [ ] Disconnect Bluetooth device
- [ ] Change boost to different settings
- [ ] Reconnect Bluetooth device
- [ ] Verify profile auto-applies (Gain: 180%, Bass: 40%)
- [ ] Notification: "Applied profile: [Device Name]"

### Phase 7: EQ Presets
- [ ] Open Equalizer screen
- [ ] Tap "Select Preset"
- [ ] Verify 28 built-in presets visible
- [ ] Select "Rock" → EQ bands update
- [ ] Select "Classical" → different curve
- [ ] Manually adjust one band → preset shows "Custom"
- [ ] Tap "Save Preset" → enter name "My Custom"
- [ ] Verify "My Custom" appears in preset list
- [ ] Select "Flat" → all bands reset to 0dB

---

## 🐛 Known Issues & Workarounds

### Issue 1: JAVA_HOME Invalid Directory
**Symptom:** Build fails with "JAVA_HOME is set to an invalid directory"  
**Cause:** Extra characters in path or incorrect escaping  
**Fix:** Remove trailing characters, verify with `echo $env:JAVA_HOME`

### Issue 2: Widget Not Updating
**Symptom:** Widget doesn't reflect boost state changes  
**Cause:** Widget update broadcast not received  
**Fix:** Implemented `updateWidgets()` call in service (already done)

### Issue 3: Bluetooth Device Name Empty
**Symptom:** Profile shows "Unknown Device"  
**Cause:** Some OEMs don't expose Bluetooth device name  
**Workaround:** Use `AudioDeviceInfo.getProductName()` as fallback

### Issue 4: Per-App Profile Not Auto-Switching
**Symptom:** Profile doesn't apply when opening app  
**Cause:** `PACKAGE_USAGE_STATS` permission not granted  
**Fix:** Prompt user to grant permission in Settings

---

## 📊 Performance Benchmarks

Test on multiple devices and record:

| Device | Android | Build Time | APK Size | RAM Usage | Battery/hr |
|--------|---------|------------|----------|-----------|------------|
| Pixel 6 | 13 | ? | ? | ? | ? |
| Samsung S21 | 14 | ? | ? | ? | ? |
| Xiaomi 12 | 13 | ? | ? | ? | ? |
| OnePlus 9 | 13 | ? | ? | ? | ? |

**RAM Usage:** Measure with Android Profiler (app foreground + service background)  
**Battery:** Monitor with Battery Historian (8-hour test with boost active)

---

## 🚀 Release Steps

### 1. Pre-Release
```bash
# Update version
# app/build.gradle.kts:
# versionCode = 38
# versionName = "1.5.0"

# Clean build
./gradlew clean

# Build signed AAB
./gradlew bundleRelease

# Verify AAB
bundletool validate --bundle=app/build/outputs/bundle/release/app-release.aab
```

### 2. Google Play Console
1. Upload `app-release.aab`
2. Update "What's New" (copy from RELEASE_NOTES_v1.5.0.txt)
3. Add feature screenshots:
   - Quick Settings Tile demo
   - Widget demo
   - Settings Backup screen
   - Per-App Profiles screen
   - Bluetooth Profiles screen
4. Update app description (mention new features)
5. Set rollout: 10% → 50% → 100% over 7 days

### 3. Post-Release Monitoring
- Check Play Console crash reports (first 24h critical)
- Monitor ANR rate (should be < 0.5%)
- Check user reviews for issues
- Track retention rate (D1, D7, D30)

---

## 🔍 Debugging Tips

### View Logs (Service)
```bash
adb logcat -s BoostService:D
```

### View Logs (Widget)
```bash
adb logcat -s Widget:D
```

### View Logs (Profiles)
```bash
adb logcat -s AppProfileManager:D BluetoothProfile:D
```

### Inspect DataStore
```bash
# Export DataStore files
adb pull /data/data/com.soundboost/files/datastore/boost_settings.preferences_pb
adb pull /data/data/com.soundboost/files/datastore/app_profiles.preferences_pb
adb pull /data/data/com.soundboost/files/datastore/bluetooth_profiles.preferences_pb
```

### Widget Debug
```bash
# Force widget update
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE -n com.soundboost/.ui.widgets.RezonansGlanceWidgetReceiver
```

---

## 📝 Version History

### v1.5.0 (Current)
- ✅ Settings Export/Import
- ✅ Premium Widget
- ✅ Per-App Profiles
- ✅ Bluetooth Profiles
- ✅ 28 EQ Presets (already existed)
- ✅ Quick Settings Tile (already existed)

### v1.4.7 (Previous)
- 10-band parametric EQ
- Call enhancement
- Bass flash sync
- Google Assistant shortcuts
- Multi-language support (12 languages)

### v1.4.0
- Glance widget foundation
- Voice call boost
- DJ gesture control (experimental)

---

## 🎯 Success Criteria

**v1.5.0 is ready for release if:**
- [ ] Build succeeds with 0 errors
- [ ] All Phase 1-7 tests pass
- [ ] No crashes in 30-min stress test
- [ ] Memory leaks < 1MB/hr
- [ ] Battery usage < 2%/hr (screen off)
- [ ] Play Store pre-launch report: 0 critical issues
- [ ] Tested on ≥ 3 different devices
- [ ] Tested on ≥ 2 Android versions (min: 10, max: 15)

---

**Next:** Fix JAVA_HOME → Build → Test → Release!

---

**Document Version:** 1.0  
**Last Updated:** 2026-09-22  
**Target Release:** v1.5.0
