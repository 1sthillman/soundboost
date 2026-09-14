# Release Notes v1.3.3

## Critical Updates for Google Play Compliance

### Android 16 Compatibility ✅
- **Target SDK**: Updated to Android 16 (API 36) - **Required by Google Play**
- **Compile SDK**: Android 16 (API 36)
- Full compliance with Google Play's November 2026 requirement

### Android 15 Enhancements
- **16KB Page Size Support**: Enabled experimental 16KB page size for improved performance
- **Modern Edge-to-Edge**: Updated system bar APIs to use Android 15+ best practices
- Removed deprecated `setStatusBarColor()` and `setNavigationBarColor()` APIs

### Technical Changes
1. **`MainActivity.kt`**: 
   - Android 15+ conditional system bar handling
   - Backward compatibility maintained for Android 14 and below

2. **`gradle.properties`**: 
   - Added `android.experimental.enable16KbPageSize=true`

3. **`AndroidManifest.xml`**: 
   - Added `android:enableOnBackInvokedCallback="true"`

### Build Information
- **Version Code**: 21
- **Version Name**: 1.3.3
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 16)
- **Compile SDK**: 36

### Google Play Console Requirements Met
✅ Target API 36 (Android 16)
✅ 16KB page size support
✅ Deprecated API removal
✅ Edge-to-edge display support

---

## User-Facing Changes
No visible changes to app functionality or UI. This is a technical compliance update.
