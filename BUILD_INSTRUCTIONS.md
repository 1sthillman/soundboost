# 🔨 Build Instructions - SoundSTBoost

**Version:** 1.1.0 (versionCode 7)  
**Package:** com.soundboost  
**Status:** ✅ Ready for Build

---

## ⚠️ JAVA_HOME Issue Detected

```
ERROR: JAVA_HOME is set to an invalid directory: 
C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot
```

**Solution:** Use Android Studio instead of command-line Gradle.

---

## 🎯 Recommended Build Method: Android Studio

### Step 1: Open Project
```
1. Launch Android Studio Ladybug (2024.2.1+)
2. File → Open → Select: C:\SoundSTBoost
3. Wait for Gradle sync (automatic)
```

### Step 2: Build Debug APK
```
1. Click green ▶️ Run button (or Shift+F10)
2. Select: "app" configuration
3. Choose: Connected device or emulator
4. Studio will build and install automatically
```

**Or via menu:**
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
Output: app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Build Release AAB (Play Store)
```
Build → Generate Signed Bundle / APK...
→ Select: Android App Bundle
→ Choose keystore: (follow prompts to create or select existing)
→ Build Variant: release
→ Output: app/build/outputs/bundle/release/SoundSTBoost-v1.1.0.aab
```

---

## 🔧 Alternative: Fix JAVA_HOME (Advanced)

### Option 1: Use Android Studio's JDK
```powershell
# Find Android Studio's embedded JDK
$jdkPath = "C:\Program Files\Android\Android Studio\jbr"

# Set JAVA_HOME (temporary, current session only)
$env:JAVA_HOME = $jdkPath

# Verify
java -version

# Build
.\gradlew.bat assembleDebug
```

### Option 2: Install JDK 17 (Recommended)
```powershell
# Download JDK 17 from:
# https://adoptium.net/temurin/releases/?version=17

# Install to: C:\Program Files\Java\jdk-17

# Set JAVA_HOME permanently:
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-17", "Machine")

# Restart PowerShell
.\gradlew.bat --version
```

### Option 3: Use Existing JDK (Fix Path)
```powershell
# Current JAVA_HOME has trailing dash/space issue
# Check actual path:
Get-ChildItem "C:\Program Files\Eclipse Adoptium\"

# If correct path is: C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot\
# Set correctly:
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot"
.\gradlew.bat --version
```

---

## 📦 Build Outputs

### Debug Build
- **APK:** `app/build/outputs/apk/debug/app-debug.apk`
- **Size:** ~15-20 MB (unoptimized)
- **Signature:** Debug keystore (auto-generated)
- **ProGuard:** Disabled
- **Shrinking:** Disabled

### Release Build
- **AAB:** `app/build/outputs/bundle/release/SoundSTBoost-v1.1.0.aab`
- **Size:** ~10-12 MB (optimized)
- **Signature:** Your keystore (key.properties)
- **ProGuard:** Enabled
- **Shrinking:** Enabled (resources)
- **Mapping:** `app/build/outputs/mapping/release/mapping.txt` (save for crashes!)

---

## ✅ Pre-Build Checklist

### Code
- ✅ Version updated (1.1.0, code 7)
- ✅ Package name correct (com.soundboost)
- ✅ Design system modernized (Color.kt, Type.kt, Theme.kt)
- ✅ All strings.xml localized (10 languages)

### Configuration
- ✅ `build.gradle.kts` - compileSdk 35, targetSdk 35
- ✅ `AndroidManifest.xml` - permissions declared
- ✅ `proguard-rules.pro` - configured
- ✅ Signing config - `key.properties.template` provided

### Documentation
- ✅ README.md - complete rewrite
- ✅ MODERNIZATION_ROADMAP.md - 7-phase plan
- ✅ MODERNIZATION_COMPLETE.md - status report
- ✅ BUILD_INSTRUCTIONS.md - this file
- ✅ .kiro/steering/ - 3 files with valid frontmatter

---

## 🧪 Post-Build Testing

### Device Testing
```bash
# Install debug APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.soundboost/.MainActivity

# Check logs
adb logcat | grep -i "soundboost"
```

### Verification Checklist
- [ ] App launches without crash
- [ ] Dark theme applied (NeonOrange primary, CyberBlue secondary)
- [ ] All sliders functional (boost, bass, virtualizer, EQ)
- [ ] Foreground service starts (notification shows)
- [ ] Settings accessible (language, auto-start)
- [ ] Audio effects apply (test with music)
- [ ] Notification permission requested (Android 13+)
- [ ] TalkBack labels present
- [ ] Reduced motion respected

### Performance Testing
- [ ] 60 FPS UI (enable GPU overdraw visualization)
- [ ] No ANR (Application Not Responding)
- [ ] Memory usage < 100 MB
- [ ] Battery drain acceptable (< 5% per hour with boost active)

---

## 🚀 Build Variants

### Debug
```bash
# Android Studio: Build Variants panel → Select "debug"
# Or CLI (after fixing JAVA_HOME):
.\gradlew.bat assembleDebug
```

**Features:**
- Fast build
- Debuggable
- No ProGuard
- Auto-signed

### Release
```bash
# Android Studio: Build → Generate Signed Bundle
# Or CLI (requires key.properties):
.\gradlew.bat bundleRelease
```

**Features:**
- Optimized build
- ProGuard enabled
- Resources shrunk
- Custom signed
- Play Store ready

---

## 🐛 Troubleshooting

### Issue: "Gradle sync failed"
**Solution:**
```
File → Invalidate Caches → Invalidate and Restart
```

### Issue: "SDK not found"
**Solution:**
```
File → Project Structure → SDK Location
Set Android SDK: C:\Users\[YOU]\AppData\Local\Android\Sdk
Set JDK: Android Studio's embedded JDK (automatic)
```

### Issue: "Build takes forever"
**Solution:**
```
1. Close other apps (free RAM)
2. Enable Gradle daemon:
   gradle.properties: org.gradle.daemon=true
3. Increase heap:
   gradle.properties: org.gradle.jvmargs=-Xmx4096m
```

### Issue: "ProGuard errors"
**Solution:**
```
Check: app/build/outputs/mapping/release/mapping.txt
Add rules to: app/proguard-rules.pro

Common additions:
-keep class com.soundboost.** { *; }
-keep class androidx.compose.** { *; }
```

---

## 📊 Build Metrics

### Expected Times (Intel i5 / 16GB RAM)
- **Clean Build:** 3-5 minutes
- **Incremental Build:** 30-60 seconds
- **Gradle Sync:** 10-30 seconds

### Expected Sizes
- **Debug APK:** 18-22 MB
- **Release AAB:** 10-13 MB
- **Installed Size:** 25-35 MB

---

## 🎯 Next Steps After Build

### 1. Test Locally
- [ ] Install debug APK on 3+ devices (different OEMs)
- [ ] Test all features (boost, EQ, 3D audio, volume controls)
- [ ] Verify accessibility (TalkBack, reduced motion)
- [ ] Check all 10 language translations

### 2. Prepare Assets
- [ ] App icon (512x512 PNG) - already exists: `play_store_icon_512.png`
- [ ] Feature graphic (1024x500) - create per MODERNIZATION_ROADMAP.md
- [ ] Screenshots (5 minimum) - capture on device
- [ ] Video preview (optional, 15-30 sec)

### 3. Play Store Setup
- [ ] Create app listing at [play.google.com/console](https://play.google.com/console)
- [ ] Upload release AAB to Internal Testing
- [ ] Complete store listing (see PLAY_STORE_LISTING.md)
- [ ] Set privacy policy URL (use PRIVACY_POLICY_TEMPLATE.md)
- [ ] Fill data safety form ("No data collected")
- [ ] Complete content rating questionnaire

### 4. Launch
- [ ] Internal Testing (2-3 days, 10+ testers)
- [ ] Alpha Testing (1 week, 100+ testers)
- [ ] Beta Testing (2 weeks, 1000+ testers)
- [ ] Production Release (go live!)

---

## 📞 Support

### Build Issues
- Check: Android Studio → Build → Build Output panel
- Logs: `app/build/outputs/logs/`
- ProGuard: `app/build/outputs/mapping/release/`

### Design System Questions
- See: `.kiro/steering/design-conventions.md`
- See: `.kiro/steering/design-system.md`
- See: `MODERNIZATION_ROADMAP.md`

### General Questions
- README: `README.md`
- Architecture: `README.md` → Project Architecture section
- Roadmap: `MODERNIZATION_ROADMAP.md`

---

**Status:** ✅ Ready for Android Studio Build

**Recommended:** Use Android Studio green ▶️ button (easiest, most reliable)

**Last Updated:** 2026-09-07  
**Build Config:** app/build.gradle.kts  
**Version:** 1.1.0 (versionCode 7)
