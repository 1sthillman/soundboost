# SoundST Boost ⚡

**Tactical audio amplifier for Android.** Real bass boost, 3D spatial audio, and real-time EQ. Built for power users who want control, not fake sliders.

[![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Material 3](https://img.shields.io/badge/Material%203-Design-6200EA?logo=material-design&logoColor=white)](https://m3.material.io/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## What It Does

Amplify system audio by up to **160dB** using Android's native `android.media.audiofx` API. Works system-wide (all apps, even with screen off) via foreground service.

**Real effects, not fake UI:**
- **Loudness Enhancer** — volume boost (20dB max, speaker-safe limit)
- **Bass Boost** — low-frequency enhancement (0-1000 range)
- **Virtualizer** — 3D spatial audio simulation (0-1000 range)
- **Equalizer** — 3-band real-time EQ (bass/mid/treble)
- **System Volume** — one-tap max volume shortcut

**What this is NOT:**
- Not a miracle app that bypasses hardware limits
- Not a sound quality enhancer (it's a volume amplifier)
- Not guaranteed to work identically on all devices (OEM audio HAL varies)

---

## Screenshots

_(Placeholder - add 5 high-quality screenshots per MODERNIZATION_ROADMAP.md Phase 6.3)_

1. **Hero:** Tactical boost dial active, CS2-inspired visualizer
2. **EQ Panel:** 3-band equalizer with neon glow
3. **3D Audio:** Virtualizer card with spatial diagram
4. **Settings:** Dark-themed settings, multi-language support
5. **Before/After:** Volume level comparison

---

## Features

### Core
- ✅ **160dB Maximum** — Software limit (hardware limits apply)
- ✅ **System-Wide** — Works in all apps (Spotify, YouTube, games)
- ✅ **Background Boost** — Continues with screen off
- ✅ **Real-Time EQ** — 3-band equalizer (bass/mid/treble)
- ✅ **3D Audio** — Virtualizer for spatial surround effect

### UX
- ✅ **CS2-Inspired UI** — Tactical gaming aesthetic (neon orange/cyber blue)
- ✅ **Glassmorphism** — Frosted glass panels (`haze` library)
- ✅ **Spring Physics** — Natural motion (60 FPS target)
- ✅ **Accessibility** — WCAG AA contrast, 48dp touch targets, reduced motion support
- ✅ **10 Languages** — English, Turkish, German, French, Spanish, Arabic, Russian, Japanese, Korean, Chinese

### Technical
- ✅ **Material 3** — Modern design system
- ✅ **Jetpack Compose** — 100% declarative UI
- ✅ **DataStore** — Persistent preferences
- ✅ **Foreground Service** — Reliable background operation
- ✅ **ProGuard** — Optimized release build
- ✅ **Auto-Start** — Optional boot receiver (restore boost on restart)

---

## Requirements

### Development
- **Android Studio:** Ladybug (2024.2.1) or newer
- **JDK:** 11 (bundled with Android Studio)
- **Gradle:** 8.11.1 (wrapper included)

### Device
- **Minimum:** Android 7.0 (API 24)
- **Target:** Android 15 (API 35)
- **Recommended:** Android 12+ for best experience

### Play Store
- **Current:** API 35 target required
- **From Aug 31, 2026:** API 36 target mandatory (project already compliant)

---

## Installation

### For Development

1. **Clone/Download** this repository
2. **Open in Android Studio** (File → Open → select `SoundSTBoost` folder)
3. **Gradle Sync** — Studio will auto-sync dependencies (google(), mavenCentral())
4. **Run** — Click green ▶️ (Run 'app') or `Ctrl+R`

### For Release

See [Play Store Deployment](#play-store-deployment) below.

---

## Project Architecture

```
app/src/main/java/com/soundboost/
├── MainActivity.kt              # Compose host, navigation
├── MainViewModel.kt             # State management (single source of truth)
├── SoundBoostApplication.kt     # App-level init (notification channel)
├── audio/
│   ├── AudioEffectsManager.kt   # Core: LoudnessEnhancer, BassBoost, Virtualizer, EQ
│   ├── AudioVisualizer.kt       # Spectrum analyzer (decorative, no mic access)
│   ├── SystemAudioCapture.kt    # Audio capture utility
│   └── SystemVolumeController.kt # Hardware volume control
├── data/
│   ├── BoostPreferences.kt      # DataStore persistence
│   └── LanguageManager.kt       # i18n locale management
├── service/
│   ├── BoostForegroundService.kt # Background service (keeps boost active)
│   └── BootReceiver.kt          # Auto-start receiver (optional)
└── ui/
    ├── theme/                   # Material 3 theme + Design DNA tokens
    │   ├── Color.kt             # CS2-inspired color system (NO AI-purple!)
    │   ├── Type.kt              # Bold sans + monospace for stats
    │   ├── Theme.kt             # Dark mode mandatory, light mode optional
    │   └── DesignTokens.kt      # Spacing, motion, elevation specs
    ├── components/              # Reusable UI components
    │   ├── NeonSlider.kt        # Custom slider with glow
    │   ├── BoostDial.kt         # Tactical HUD dial
    │   ├── EqualizerVisualizer.kt # 32-bar spectrum
    │   └── CyberCard.kt         # Glassmorphic card
    └── screens/
        ├── HomeScreen.kt        # Main panel (dial, EQ, visualizer)
        └── SettingsScreen.kt    # Settings (auto-start, language, about)
```

**Data Flow:**
```
User Interaction → MainViewModel (state update)
                → DataStore (persist)
                → BoostForegroundService (if active)
                → AudioEffectsManager (apply to session 0)
                → System audio output
```

---

## Permissions

Only **essential permissions** are requested. **NO microphone/recording** (visualizer is decorative animation).

| Permission | Why | User Prompt? |
|------------|-----|--------------|
| `MODIFY_AUDIO_SETTINGS` | Apply audio effects to system output | No (normal permission) |
| `FOREGROUND_SERVICE` | Keep boost active in background | No (manifest permission) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Declare service type for Android 14+ | No |
| `POST_NOTIFICATIONS` | Show persistent "Boost Active" notification | Yes (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after reboot (if enabled in settings) | No |

**Privacy:** No data collection, no internet permission, no analytics. All settings stored locally via DataStore.

---

## Play Store Deployment

### 1. Generate Signing Key

```bash
# Android Studio → Build → Generate Signed Bundle/APK → Create new keystore
# Save keystore file and passwords securely (CANNOT recover if lost)
```

Create `key.properties` in project root:
```properties
storeFile=path/to/your/keystore.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

**⚠️ CRITICAL:** Never commit `key.properties` to version control. Already in `.gitignore`.

### 2. Build Release AAB

```bash
# Android Studio → Build → Generate Signed Bundle/APK
# Select: Android App Bundle (.aab)
# Build Variant: release
# Output: app/release/SoundSTBoost-v1.1.0.aab
```

Or via CLI:
```bash
./gradlew bundleRelease
```

### 3. Play Console Setup

1. **Create app** at [play.google.com/console](https://play.google.com/console)
2. **Upload AAB** to Internal Testing track first
3. **Store Listing:**
   - **App name:** SoundST Boost
   - **Short description** (80 chars): "Amplify audio 160dB. Real bass boost, 3D audio, EQ. Works system-wide."
   - **Full description:** See `PLAY_STORE_LISTING.md`
   - **App icon:** `play_store_icon_512.png` (512x512, provided)
   - **Feature graphic:** Create 1024x500 banner (see `MODERNIZATION_ROADMAP.md` Phase 6.2)
   - **Screenshots:** 5 high-quality (phone + 7" tablet)
4. **Privacy Policy:** Required for foreground service apps
   - Use template in `PRIVACY_POLICY_TEMPLATE.md`
   - Host on GitHub Pages or your website
   - Link in Play Console
5. **Data Safety Form:** Select "No data collected" (all local storage)
6. **Content Rating:** Complete questionnaire (likely "Everyone 3+")
7. **Countries:** Select target markets
8. **Pricing:** Free (ad-free, no IAP)

### 4. Testing & Release

```bash
# Internal Testing → Alpha → Beta → Production
# Start with Internal Testing (up to 100 testers)
# Test on multiple devices (Pixel, Samsung, OnePlus)
# Zero ANR/crash reports before promoting to Production
```

**Checklist before Production:**
- [ ] Tested on 3+ devices (different OEMs)
- [ ] Accessibility tested (TalkBack, reduced motion)
- [ ] All translations verified
- [ ] Privacy policy live and linked
- [ ] Screenshots uploaded (5 minimum)
- [ ] Feature graphic created
- [ ] Version code incremented (`versionCode` in `build.gradle.kts`)
- [ ] Release notes written

---

## Development Guidelines

### Design System (`.kiro/steering/design-system.md`)

**Three Dials:**
```kotlin
DESIGN_VARIANCE: 8    // Bold asymmetric layouts
MOTION_INTENSITY: 7   // High-energy animations (60 FPS)
VISUAL_DENSITY: 4     // Standard mobile density
```

**Color Consistency Lock:**
- Primary: `NeonOrange` (CS2-inspired, NOT AI-purple)
- Secondary: `CyberBlue` (electric blue, competitive)
- Accent: `NeonGreen` (EQ active, success states)
- ONE accent per screen (no random color flips)

**Typography:**
- Display/Headlines: Bold sans (900/800 weight) → upgrade to Geist Display
- Body: Normal sans (400 weight) → upgrade to Geist
- Stats/Mono: Roboto Mono (600 weight) → upgrade to Geist Mono

**Motion:**
- Spring physics for all interactions (`AnimationSpecs.bouncySpring`)
- NO `window.addEventListener('scroll')` (use Compose animation APIs)
- Reduced motion support mandatory (`LocalDensity.current.fontScale`)

**Accessibility:**
- WCAG AA contrast minimum (4.5:1 body, 3:1 large text)
- Touch targets 48dp minimum
- TalkBack semantic labels on all controls
- Reduced motion: disable non-essential animations

### Anti-Patterns (taste-skill Section 9)

**BANNED:**
- ❌ Em-dash (`—`) in UI strings (use hyphen `-`)
- ❌ AI-purple defaults (NeonPurple legacy, migrate away)
- ❌ Generic "Jane Doe" / "Acme" placeholders
- ❌ Fake data (random "99.99%" metrics)
- ❌ Hand-rolled SVG icons (use Material Icons or Phosphor)
- ❌ Div-based fake screenshots (use real device screenshots)

**REQUIRED:**
- ✅ Real audio data (or honest decorative animation)
- ✅ Specific device names (Pixel 6, Galaxy S23, OnePlus 11)
- ✅ Organic metrics (47.2%, not 50%)
- ✅ Context-appropriate copy (no "elevate", "seamless", "unleash")

### Pre-Flight Checklist (taste-skill Section 14)

Before every commit:
- [ ] Zero em-dash (`—`) in strings.xml
- [ ] Color consistency (NeonOrange used throughout)
- [ ] Button contrast (WCAG AA verified)
- [ ] Hero fits viewport (no scroll to see CTA)
- [ ] Typography scale planned (displayLarge → bodySmall)
- [ ] Motion motivated (every animation justified)
- [ ] Reduced motion support (no infinite loops without fallback)
- [ ] Dark mode enforced (light mode opt-in only)
- [ ] No AI tells (Inter font, AI-purple, generic layouts)

---

## Troubleshooting

### Gradle Sync Failed
```bash
# Solution 1: Invalidate caches
File → Invalidate Caches → Invalidate and Restart

# Solution 2: Clean build
./gradlew clean
./gradlew build

# Solution 3: Check JDK version
File → Project Structure → SDK Location → JDK 11
```

### Emulator Audio Issues
**Emulator audio drivers don't support all `audiofx` effects.** Test on real hardware.

### Notification Not Showing (Android 13+)
```kotlin
// Check permission in Settings → Apps → SoundST Boost → Notifications
// Or request at runtime (already implemented in MainActivity.kt)
```

### Boost Not Working on Device X
**OEM audio HAL limitations.** Some manufacturers (Xiaomi, Oppo) restrict global audio session access. Document known device compatibility in Play Store description.

---

## Roadmap

### v1.2.0 (Next Release)
- [ ] Real-time spectrum analyzer (requires `RECORD_AUDIO` permission)
- [ ] Per-app boost profiles
- [ ] Quick Settings tile
- [ ] Widget support
- [ ] Export/import settings

### v2.0.0 (Major)
- [ ] Parametric EQ (10-band)
- [ ] Presets (Rock, Electronic, Classical, etc.)
- [ ] Audio visualizer themes
- [ ] Bluetooth device-specific profiles
- [ ] Wear OS companion app

### Design System Evolution
- [ ] Complete Design DNA JSON (`.kiro/steering/design-system.md`)
- [ ] Geist font integration (via design-dna Phase 3)
- [ ] CS2-themed component library
- [ ] Motion pattern cookbook
- [ ] Accessibility automation testing

---

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Follow [Design Guidelines](#development-guidelines)
4. Run Pre-Flight Checklist
5. Commit (`git commit -m 'Add amazing feature'`)
6. Push (`git push origin feature/amazing-feature`)
7. Open Pull Request

**Code Style:**
- Kotlin official style guide
- Material 3 Compose patterns
- MVVM architecture
- Single Activity, Compose Navigation

---

## License

MIT License. See [LICENSE](LICENSE) for details.

---

## Acknowledgments

- **Material 3** — Design foundation
- **Jetpack Compose** — Modern UI toolkit
- **Haze Library** — Glassmorphism effects ([dev.chrisbanes.haze](https://github.com/chrisbanes/haze))
- **taste-skill** — Anti-slop design rules
- **frontend-design** — Distinctive design principles
- **design-dna** — 3-phase design methodology

---

## Honest Disclaimer

**This app amplifies audio using Android's official APIs.** Results depend on your device's DAC, amplifier, and OEM audio configuration. 160dB is the software limit; your speaker's physical limits apply.

**Use responsibly:**
- Prolonged exposure to high volume damages hearing
- Distortion occurs when pushing hardware beyond its limits
- Speaker damage possible at extreme settings
- Not a substitute for quality audio equipment

**No warranties.** Use at your own risk. The developers are not liable for hearing damage or hardware damage resulting from misuse.

---

**Built with ⚡ by power users, for power users.**

For questions, issues, or feature requests, open an issue on GitHub.

🔗 [Website](https://example.com) • [Privacy Policy](https://example.com/privacy) • [Support](https://github.com/yourusername/soundstboost/issues)
