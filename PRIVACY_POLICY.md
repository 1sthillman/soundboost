# Privacy Policy for Sound'ST Boost

**Effective Date:** December 9, 2026  
**Last Updated:** December 9, 2026  
**Version:** 1.3.2

---

## 1. Introduction

Sound'ST Boost ("we," "our," "the app," or "Sound'ST Boost") is a professional audio enhancement application for Android devices. We are committed to protecting your privacy and being transparent about our data practices.

**Developer:** 1sthillman  
**Contact:** https://github.com/1sthillman/soundboost

This Privacy Policy explains:
- What information we collect (and don't collect)
- How we use permissions
- Your rights and choices
- How we protect your privacy

---

## 2. Our Privacy Commitment

### We Do NOT Collect Personal Data

Sound'ST Boost is designed with **privacy-first principles**. We explicitly state that we do **NOT** collect, store, transmit, or share any personal information, including:

❌ **Never Collected:**
- Personal identification (name, email, phone number, postal address)
- Device identifiers (IMEI, serial number, MAC address, Android ID)
- Location data (GPS, IP address, network location)
- Audio recordings or microphone input
- Phone call content or metadata
- Contact lists, photos, or media files
- Browsing history or app usage patterns
- Usage analytics or telemetry data
- Advertising identifiers (Google Advertising ID)
- Behavioral data or user profiles
- Any biometric data
- Any other personal or sensitive data

### Local-Only Data Storage

The app stores settings **locally on your device only** using Android's secure storage (DataStore/SharedPreferences):

✅ **Stored Locally (On Your Device):**
- Audio settings (volume boost level, equalizer bands, bass boost, virtualizer)
- Call enhancement preferences (on/off state)
- Visual preferences (selected theme, color accent, dark mode)
- Language preference
- Auto-start on boot preference
- Custom equalizer presets
- Visualizer sensitivity settings
- Warning acknowledgments (gain warning "don't show again")

**Important:** This data:
- Never leaves your device
- Is not backed up to cloud services
- Is automatically deleted when you uninstall the app
- Cannot be accessed by other apps or services
- Is not shared with any third parties

---

## 3. Permissions Explained

Sound'ST Boost requests the following Android permissions. Each permission is explained with its purpose, necessity, and data access:

### 3.1 MODIFY_AUDIO_SETTINGS

**Permission Type:** Normal (automatic, no user prompt)  
**Purpose:** Apply audio effects to system output  
**Required:** ✅ Yes - Core functionality  
**Data Access:** None  

**What it does:**
- Applies volume boost (LoudnessEnhancer)
- Adjusts equalizer bands
- Controls bass boost and virtualizer effects
- Enables call enhancement audio optimization

**Privacy Impact:** This permission only modifies audio output settings. It does not access, record, or transmit any audio content.

---

### 3.2 RECORD_AUDIO

**Permission Type:** Dangerous (requires user approval)  
**Purpose:** Audio visualization and call enhancement  
**Required:** No - Optional features  
**Data Access:** System audio output analysis (NOT microphone input)  

**What it does:**

#### For Audio Visualization:
- Captures audio session data for real-time spectrum analysis
- Powers the visual audio spectrum analyzer on home screen
- Creates waveform visualizations

#### For Call Enhancement (NEW in v1.3.2):
- Enables microphone noise suppression during calls
- Applies automatic gain control to your voice
- Improves call audio quality for the other party

**Important Privacy Notes:**
1. **Visualization Use:** When used for visualization, this permission analyzes audio OUTPUT (what you hear), not microphone INPUT
2. **Call Enhancement Use:** When Call Enhancement is enabled, it processes microphone input in real-time to reduce background noise
3. **NO RECORDING:** Audio data is processed in real-time and immediately discarded. Nothing is saved, stored, or transmitted
4. **You Control It:** You can deny this permission and the app works (without visualizations and call enhancement)
5. **Revoke Anytime:** You can revoke this permission in Android Settings → Apps → Sound'ST Boost → Permissions

**Technical Details:**
- Uses Android `AudioRecord` API with `VOICE_COMMUNICATION` source (for call enhancement)
- Uses Android `Visualizer` API (for spectrum visualization)
- Applies `NoiseSuppressor` and `AutomaticGainControl` effects (for call enhancement)
- All processing happens on-device in real-time
- No audio data is persisted to storage

---

### 3.3 FOREGROUND_SERVICE & FOREGROUND_SERVICE_MEDIA_PLAYBACK

**Permission Type:** Normal (automatic, no user prompt)  
**Purpose:** Keep audio boost active in background  
**Required:** ✅ Yes - For persistent boost  
**Data Access:** None  

**What it does:**
- Allows audio boost to continue when screen is off
- Maintains audio effects when you switch apps
- Shows persistent notification (required by Android)

**Privacy Impact:** This permission only keeps the app's audio effects running. It does not access any data.

---

### 3.4 POST_NOTIFICATIONS (Android 13+)

**Permission Type:** Dangerous (requires user approval on Android 13+)  
**Purpose:** Display persistent status notification  
**Required:** No - Recommended  
**Data Access:** None  

**What it does:**
- Shows notification with current boost status
- Displays quick controls (bass, virtualizer adjust)
- Provides stop boost action

**Privacy Impact:** Only displays notifications about the app's own status. Does not access or display any personal data.

**You can deny this:** The app works without notifications, but you won't see the boost status or quick controls.

---

### 3.5 RECEIVE_BOOT_COMPLETED

**Permission Type:** Normal (automatic, no user prompt)  
**Purpose:** Auto-start boost after device restart  
**Required:** No - Only if you enable "Auto-start on boot"  
**Data Access:** None  

**What it does:**
- Starts the audio boost service automatically after device reboot
- Only activates if you enable "Auto-start on boot" in settings

**Privacy Impact:** Only triggers app startup after boot. Does not access any data.

**You control this:** Disabled by default. You must manually enable "Auto-start on boot" in app settings.

---

### 3.6 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS

**Permission Type:** Special (requires user interaction)  
**Purpose:** Prevent Android from killing the boost service  
**Required:** No - Optional for better reliability  
**Data Access:** None  

**What it does:**
- Requests exemption from battery optimization
- Helps maintain audio boost when device is idle
- Improves reliability on aggressive battery savers

**Privacy Impact:** Only affects power management. Does not access any data.

---

## 4. Features and Privacy Details

### 4.1 Core Audio Enhancement

**Features:**
- Volume boost (60% - 500%)
- 10-band parametric equalizer
- Bass boost and virtualizer
- Custom EQ presets

**Privacy:** Processes audio in real-time using Android's native audio effects API. No audio data is recorded, stored, or transmitted.

---

### 4.2 Call Enhancement (NEW in v1.3.2)

**Features:**
- Incoming voice clarity boost
- Background noise suppression on your microphone
- Automatic gain control for your voice
- Works with phone calls and VoIP apps (WhatsApp, Discord, Zoom, etc.)

**Privacy:**
- Processes microphone audio in real-time only when active
- Uses Android's `NoiseSuppressor` and `AutomaticGainControl` APIs
- No call audio is recorded or stored
- No call metadata is collected
- All processing happens on-device
- Can be disabled anytime in Equalizer settings

**Technical Implementation:**
- Creates `AudioRecord` session for microphone processing
- Applies noise suppression filter to outgoing audio
- Normalizes voice levels automatically
- Destroys audio session when disabled
- No audio buffers are retained

---

### 4.3 Audio Visualization

**Features:**
- Real-time spectrum analyzer
- Waveform visualizations
- Multiple visual themes (Mehtap, Awwards, Fener, Eyes, Orman, etc.)
- Beat detection and energy analysis

**Privacy:**
- Analyzes audio output (what you hear) for visualization
- Uses Android's `Visualizer` API
- Audio data is processed in memory and immediately discarded
- No audio samples are saved or transmitted
- Visualization runs only when app is in foreground

---

### 4.4 Multi-Stream Audio Management

**Features:**
- Detects and enhances multiple audio streams
- Optimizes settings for Bluetooth/wired headphones/speaker
- Smart gain recommendations per device

**Privacy:**
- Only detects audio output device type (speaker, headphones, Bluetooth)
- Does not access device pairing information
- Does not track which specific devices you connect
- No device identifiers are stored or transmitted

---

## 5. Third-Party Services

### We Use NO Third-Party Services

Sound'ST Boost is completely standalone and does **NOT** integrate any third-party services:

❌ **Not Used:**
- No analytics services (Google Analytics, Firebase Analytics, Mixpanel, etc.)
- No advertising networks (Google Ads, AdMob, Facebook Audience Network, etc.)
- No crash reporting services (Crashlytics, Sentry, Bugsnag, etc.)
- No cloud services (Firebase, AWS, Azure, etc.)
- No social media SDKs (Facebook, Twitter, etc.)
- No user authentication services (Google Sign-In, Facebook Login, etc.)
- No payment processors (in-app purchases, subscriptions)
- No A/B testing platforms
- No push notification services (FCM, OneSignal, etc.)
- No customer support chat services
- No remote configuration services

**Why?**
- We respect your privacy
- We keep the app lightweight and fast
- We avoid dependencies that could compromise privacy
- We want you to have full control

---

## 6. Data Security

### On-Device Security

Since all data stays on your device, it's protected by Android's security mechanisms:

**Storage Security:**
- Data stored in app's private directory (Android sandboxing)
- Not accessible to other apps
- Protected by device encryption (if enabled)
- Automatically deleted on app uninstall

**Runtime Security:**
- Audio processing happens in isolated memory
- No audio buffers written to disk
- All temporary data cleared on app close
- Secure API usage (Android audio framework)

**Network Security:**
- **No network access** - App does not request INTERNET permission
- Cannot transmit data even if compromised
- No telemetry or analytics collection
- Fully offline functionality

---

## 7. Children's Privacy (COPPA Compliance)

Sound'ST Boost does not knowingly collect any information from anyone, including children under 13 years of age.

**Age Restriction:** None - App suitable for all ages  
**Data Collection from Children:** None  
**Parental Consent:** Not required (no data collection)

The app is designed to be safe for users of all ages because:
- No data collection of any kind
- No online features or communication
- No advertising or in-app purchases
- No user accounts or profiles
- Fully offline operation

---

## 8. Your Rights and Choices

### 8.1 Data Access

**Right to Access:** Since we don't collect any personal data, there is no data to access.

**Where Your Settings Are:**
- All preferences stored in: `/data/data/com.soundboost/shared_prefs/` (on your device)
- Accessible only to you and the app
- Can be viewed in app settings

---

### 8.2 Data Deletion

**Right to Deletion:**
- All app data is local to your device
- Simply uninstall the app to permanently delete all data
- No data remains on any servers (we don't have servers)

**How to Delete:**
1. Android Settings → Apps → Sound'ST Boost → Uninstall
2. Or: Long-press app icon → App info → Uninstall

---

### 8.3 Permission Control

**Right to Control:**
- You can revoke any permission at any time
- Android Settings → Apps → Sound'ST Boost → Permissions
- App will disable features that require revoked permissions

**Permission Matrix:**

| Permission | Can Revoke? | Impact if Revoked |
|------------|-------------|-------------------|
| MODIFY_AUDIO_SETTINGS | No | Core audio boost won't work |
| RECORD_AUDIO | ✅ Yes | Visualization & call enhancement disabled |
| POST_NOTIFICATIONS | ✅ Yes | No status notification shown |
| RECEIVE_BOOT_COMPLETED | ✅ Yes (via settings) | No auto-start after reboot |

---

### 8.4 Data Portability

**Right to Portability:** Not applicable (no personal data collected)

**Export Settings:** Currently not supported. Settings are stored locally and deleted on uninstall.

---

## 9. Compliance with Privacy Laws

Sound'ST Boost complies with major privacy regulations:

### 9.1 GDPR (General Data Protection Regulation)

**Compliance Status:** ✅ Compliant

**How we comply:**
- **Article 5 - Lawfulness:** We collect no personal data
- **Article 6 - Legal Basis:** Not applicable (no data processing)
- **Article 7 - Consent:** Not applicable (no data processing)
- **Article 13/14 - Transparency:** This privacy policy provides full transparency
- **Article 15-20 - Data Subject Rights:** Not applicable (no personal data)
- **Article 25 - Privacy by Design:** App designed with privacy-first approach
- **Article 32 - Security:** All data local and secured by Android
- **Article 33/34 - Breach Notification:** Not applicable (no data collection)

---

### 9.2 CCPA (California Consumer Privacy Act)

**Compliance Status:** ✅ Compliant

**How we comply:**
- **§1798.100 - Right to Know:** We collect no personal information
- **§1798.105 - Right to Delete:** All data deleted on app uninstall
- **§1798.110 - Categories Collected:** None
- **§1798.115 - Sale of Personal Information:** We never sell data (we don't have any)
- **§1798.120 - Right to Opt-Out:** Not applicable (no sale of data)
- **§1798.135 - Non-Discrimination:** Not applicable

---

### 9.3 COPPA (Children's Online Privacy Protection Act)

**Compliance Status:** ✅ Compliant

**How we comply:**
- No personal information collected from children (or anyone)
- No user accounts or registration
- No online features or communication
- No behavioral advertising
- No persistent identifiers collected

---

### 9.4 Google Play Data Safety

**Data Safety Section:** All answers are "No data collected"

**Google Play Declaration:**
```
Data Collection: NO
Data Sharing: NO
Security Practices:
  - Data encrypted in transit: N/A (no network access)
  - Data encrypted at rest: YES (Android system encryption)
  - Users can request data deletion: YES (via app uninstall)
```

---

## 10. International Data Transfers

**Status:** Not applicable

Sound'ST Boost does not transfer any data internationally (or domestically) because:
- No data leaves your device
- No servers or cloud services
- Fully offline operation
- No network access

---

## 11. Changes to This Privacy Policy

### How We Update This Policy

We may update this Privacy Policy to:
- Reflect new features
- Improve clarity
- Comply with new regulations
- Address user feedback

**Change Notification:**
- Updated policy posted on this page
- "Last Updated" date changed
- Major changes: In-app notification on first launch after update
- Minor clarifications: No notification required

**Version History:**
- v1.3.2 (Dec 9, 2026): Added Call Enhancement feature details
- v1.2.0 (Aug 27, 2026): Initial comprehensive policy

---

## 12. Contact Information

### How to Reach Us

If you have questions, concerns, or requests regarding this Privacy Policy:

**Developer:** 1sthillman  
**GitHub:** https://github.com/1sthillman/soundboost  
**Issues:** https://github.com/1sthillman/soundboost/issues  
**Discussions:** https://github.com/1sthillman/soundboost/discussions

**Response Time:** We aim to respond within 7 business days

---

## 13. Open Source Transparency

### Full Code Review Available

Sound'ST Boost is **open-source software**. You can:

✅ **Review the entire source code:**
- GitHub Repository: https://github.com/1sthillman/soundboost
- License: [Specify License]
- Community audited and verified

✅ **Verify our privacy claims:**
- Check permissions in `AndroidManifest.xml`
- Review data storage in `BoostPreferences.kt`
- Inspect network usage (none!)
- Audit audio processing code

✅ **Contribute and improve:**
- Report issues
- Suggest features
- Submit pull requests
- Join discussions

**Transparency Commitment:** What you see in the code is exactly what the app does. No hidden behavior.

---

## 14. Frequently Asked Questions

### Q1: Does the app record my calls?

**A:** Absolutely NOT. Call Enhancement processes audio in real-time to reduce background noise on your microphone, but does not record, save, or transmit any call audio. The audio is processed and immediately discarded.

---

### Q2: Can you hear what I'm listening to?

**A:** NO. We have no servers, no network access, and no way to receive any data from your device. The audio visualization analyzes audio locally for display only.

---

### Q3: Why do you need RECORD_AUDIO permission?

**A:** For two optional features:
1. **Audio Visualization**: To analyze audio output and display spectrum analyzer
2. **Call Enhancement**: To process microphone input for noise reduction

You can deny this permission and the app still works (boost, equalizer, bass, virtualizer all function normally).

---

### Q4: Does the app use any analytics?

**A:** NO. We use zero analytics services. We don't know who uses the app, how often, or what features you use. Your usage is completely private.

---

### Q5: Do you sell my data?

**A:** We can't sell what we don't have. Since we collect zero data, there's nothing to sell.

---

### Q6: How do I delete my data?

**A:** Simply uninstall the app. All settings are stored locally on your device and are automatically deleted when you uninstall.

---

### Q7: Will you add ads in the future?

**A:** No. We have no plans to add advertising. The app is designed to be ad-free forever.

---

### Q8: Why don't you have a privacy dashboard?

**A:** Because we have no data to show you. A privacy dashboard would be empty since we don't collect, process, or store any personal information.

---

### Q9: Is my audio safe?

**A:** Yes. Audio processing happens entirely on your device using Android's native APIs. No audio data is ever sent anywhere. The app doesn't even have internet access.

---

### Q10: Can I use this app offline?

**A:** Yes! In fact, the app ONLY works offline. It has no online features and doesn't request internet permission.

---

## 15. Legal Basis for Processing (GDPR)

**Status:** Not applicable

Under GDPR, we are not a "data controller" or "data processor" because we do not collect or process personal data. The app operates entirely on-device without any data collection.

---

## 16. Data Protection Officer

**Status:** Not required

Sound'ST Boost is not required to appoint a Data Protection Officer because:
- We are not a public authority
- We do not engage in large-scale processing
- We do not process special categories of personal data
- We do not process any personal data at all

---

## 17. Privacy by Design and by Default

Sound'ST Boost is built with **Privacy by Design** principles:

### Design Principles

1. **Proactive not Reactive:** Built to prevent privacy issues from the start
2. **Privacy as Default:** No opt-out needed (we don't collect data by default)
3. **Privacy Embedded:** Privacy integrated into design, not added later
4. **Full Functionality:** Privacy doesn't compromise functionality
5. **End-to-End Security:** Local processing, no network transmission
6. **Visibility and Transparency:** Open source, auditable code
7. **User-Centric:** You control all settings and permissions

### Technical Implementation

- **No network stack:** App doesn't request INTERNET permission
- **Local processing:** All audio effects processed on-device
- **Minimal permissions:** Only essential permissions requested
- **Secure storage:** Android sandboxed private directory
- **No identifiers:** No device tracking or user profiling
- **Open source:** Full transparency and auditability

---

## 18. Acknowledgment and Consent

By using Sound'ST Boost, you acknowledge that you have read and understood this Privacy Policy.

**Key Points to Remember:**
✅ We collect NO personal data  
✅ All settings stored locally on your device  
✅ Audio processed in real-time, not recorded  
✅ No third-party services integrated  
✅ Fully offline operation  
✅ Open source and auditable  
✅ You control all permissions  

---

## 19. Severability

If any provision of this Privacy Policy is found to be unenforceable or invalid, that provision will be limited or eliminated to the minimum extent necessary so that the rest of this Privacy Policy will remain in full force and effect.

---

## 20. Entire Agreement

This Privacy Policy constitutes the entire agreement between you and Sound'ST Boost regarding privacy and supersedes any prior agreements.

---

**Last Updated:** December 9, 2026  
**Version:** 1.3.2  
**Effective Date:** December 9, 2026

---

## Summary (TL;DR)

**Sound'ST Boost Privacy in 10 Seconds:**

🔒 **Zero data collection**  
📱 **Everything stays on your device**  
🎧 **Audio processed in real-time, not recorded**  
🌐 **No internet, no servers, no tracking**  
🔓 **Open source - verify yourself**  
✅ **GDPR, CCPA, COPPA compliant**

**You can trust Sound'ST Boost with your privacy because we literally have no way to access your data.**

---

*This privacy policy is provided in good faith and for informational purposes. It may be updated from time to time, and continued use of the app constitutes acceptance of any changes.*
