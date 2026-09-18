# Release Notes v1.4.6

**Release Date:** September 18, 2026  
**Version Code:** 36  
**Version Name:** 1.4.6  

---

## 🔒 Critical Google Play Policy Compliance Update

### Privacy & Security Improvements

✅ **Removed Unnecessary Permissions**
- Explicitly removed `ACCESS_NETWORK_STATE` permission
- Explicitly removed `INTERNET` permission (already not requested)
- Explicitly removed `WAKE_LOCK` permission
- These permissions were being added by dependencies and have been blocked

✅ **100% Offline Guarantee**
- App is now technically impossible to access internet
- All network-related permissions removed at manifest level
- Enhanced privacy protection

✅ **Updated Privacy Policy**
- Added documentation for removed permissions
- Clarified data collection policies (ZERO data collection)
- Updated to v1.4.6 with transparency improvements

---

## Why This Update?

**Google Play Review:** Previous version (1.4.2) was flagged because build included `ACCESS_NETWORK_STATE` permission that was added by a dependency. This contradicted our "no data collection" declaration.

**Solution:** This update explicitly removes all network-related permissions using `tools:node="remove"` in AndroidManifest.xml, ensuring complete privacy compliance.

---

## Technical Changes

### AndroidManifest.xml Updates

```xml
<!-- Explicitly removed permissions -->
<uses-permission android:name="com.google.android.gms.permission.AD_ID" 
    tools:node="remove" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" 
    tools:node="remove" />
<uses-permission android:name="android.permission.INTERNET" 
    tools:node="remove" />
<uses-permission android:name="android.permission.WAKE_LOCK" 
    tools:node="remove" />
```

### Build Configuration

- Version Code: 35 → 36
- Version Name: 1.4.5 → 1.4.6
- Target SDK: 36 (Android 15)
- Compile SDK: 36

---

## Existing Features (Unchanged)

All features from v1.4.x remain fully functional:

### Core Features
- 🔊 Volume Boost (60% - 500%)
- 🎛️ Professional 10-Band Equalizer
- 🎵 Bass Boost & 3D Virtualizer
- 📞 Call Enhancement
- 🎨 Audio Visualizations

### v1.4.0 Features
- 🏠 Home Screen Widgets (1x1, 2x1, 4x1)
- ⚡ Quick Settings Tile
- 🎧 Device Profile Memory
- 🔇 Smart Volume Limits

---

## Google Play Submission Notes

### Data Safety Declaration

**Answer:** ❌ **NO DATA COLLECTED**

All questions about data collection should be answered "NO":
- Collects personal data: NO
- Shares data with third parties: NO
- Uses advertising ID: NO
- Accesses network: NO

### Permissions Explanation

When Google Play asks about permissions:

**MODIFY_AUDIO_SETTINGS:** Core audio effects (volume boost, equalizer)  
**RECORD_AUDIO:** Optional - Audio visualization & call enhancement only  
**FOREGROUND_SERVICE:** Keep audio boost active in background  
**POST_NOTIFICATIONS:** Status notification (optional)  
**RECEIVE_BOOT_COMPLETED:** Auto-start on boot (disabled by default)  
**REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:** Better reliability (optional, user-initiated)

### What Changed from v1.4.2?

**Before (v1.4.2 - REJECTED):**
- Build included `ACCESS_NETWORK_STATE` permission
- Conflicted with "no data collection" claim
- Google flagged as policy violation

**After (v1.4.6 - COMPLIANT):**
- All network permissions explicitly removed
- Manifest blocks dependency permissions
- Clean permission list matching v1.3.9
- Full policy compliance

---

## Privacy Commitment

**Rezonans v1.4.6 Privacy Guarantees:**

✅ ZERO data collection  
✅ 100% offline operation  
✅ No internet access (technically impossible)  
✅ No analytics or tracking  
✅ No ads ever  
✅ Open source & auditable  
✅ All data stays on device  
✅ GDPR, CCPA, COPPA compliant  

---

## Installation

### For Users

Update will be available through Google Play Store once approved.

### For Developers

```bash
# Build release AAB
./gradlew clean bundleRelease

# Output location
app/build/outputs/bundle/release/SoundSTBoost-v1.4.6-release.aab
```

---

## Known Issues

None. This is a compliance-only update with no functional changes.

---

## Next Steps

1. Upload to Google Play Console
2. Update store listing (no changes needed)
3. Submit for review
4. Monitor review status

Expected approval: Within 24-48 hours (compliance fix, no new features)

---

**Full Changelog:** https://github.com/1sthillman/soundboost/releases/tag/v1.4.6  
**Privacy Policy:** https://github.com/1sthillman/soundboost/blob/main/PRIVACY_POLICY.md
