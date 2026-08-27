# Privacy Policy for Sound'ST Boost

**Last Updated:** August 27, 2026

## Introduction

Sound'ST Boost ("we," "our," or "the app") is committed to protecting your privacy. This Privacy Policy explains how our Android volume booster application handles user information.

## Information We Collect

### Information We DO NOT Collect

Sound'ST Boost does **NOT** collect, store, or transmit any personal information, including but not limited to:

- Personal identification information (name, email, phone number, address)
- Device identifiers (IMEI, MAC address, Android ID)
- Location data
- Audio recordings or microphone data
- Usage analytics or statistics
- Contact lists or photos
- Any other personal data

### Local Data Storage

The app stores the following data **locally on your device only**:

- **User Preferences:** Your audio settings (volume levels, equalizer settings, bass boost, virtualizer)
- **App Settings:** Selected theme, language preference, auto-start preference
- **Audio Session State:** Current boost status and audio effects state

This data is stored using Android's SharedPreferences and DataStore, and **never leaves your device**.

## Permissions Used

The app requires the following Android permissions:

### MODIFY_AUDIO_SETTINGS
**Purpose:** To apply audio effects (volume boost, equalizer, bass boost, virtualizer)
**Required:** Yes - Core functionality
**Data Access:** None - Only modifies audio output settings

### RECORD_AUDIO
**Purpose:** To capture audio output for the visual audio spectrum analyzer
**Required:** No - Only for visualization
**Data Access:** Audio session data for visualization only (not recorded or stored)
**Note:** This permission captures system audio output, NOT microphone input

### FOREGROUND_SERVICE & FOREGROUND_SERVICE_MEDIA_PLAYBACK
**Purpose:** To keep audio boost active when the screen is off or app is in background
**Required:** Yes - For persistent audio effects
**Data Access:** None

### POST_NOTIFICATIONS (Android 13+)
**Purpose:** To show persistent notification when boost is active
**Required:** No - User can deny
**Data Access:** None

### RECEIVE_BOOT_COMPLETED
**Purpose:** To auto-start boost after device restart (if enabled by user)
**Required:** No - Only if auto-start is enabled
**Data Access:** None

## Third-Party Services

Sound'ST Boost does **NOT** use any third-party services, including:

- No analytics services (e.g., Google Analytics, Firebase Analytics)
- No advertising networks
- No crash reporting services
- No cloud services or remote servers
- No social media integration

## Data Sharing

We do **NOT** share any data with third parties because we do **NOT** collect any data.

## Data Security

All app settings and preferences are stored locally on your device using Android's secure storage mechanisms:

- Data is sandboxed within the app's private storage
- No data is transmitted over the network
- No data is accessible to other apps
- Data is automatically deleted when you uninstall the app

## Children's Privacy

Sound'ST Boost does not collect any information from anyone, including children under 13 years of age.

## Changes to This Privacy Policy

We may update this Privacy Policy from time to time. Any changes will be posted on this page with an updated "Last Updated" date.

## Your Rights

Since we do not collect any personal data:

- There is no data to access, export, or delete
- All settings are stored locally and deleted when you uninstall the app
- You have full control over all app permissions through Android settings

## Contact Us

If you have any questions about this Privacy Policy, please contact us:

- **Email:** [Your Contact Email]
- **GitHub:** https://github.com/1sthillman/soundboost

## Compliance

This app complies with:

- GDPR (General Data Protection Regulation)
- CCPA (California Consumer Privacy Act)
- COPPA (Children's Online Privacy Protection Act)
- Google Play's User Data Policy

## Open Source

Sound'ST Boost is open-source software. You can review our source code at:
https://github.com/1sthillman/soundboost

---

**Summary:** Sound'ST Boost is a privacy-friendly app that does not collect, store, or share any personal information. All data stays on your device.
