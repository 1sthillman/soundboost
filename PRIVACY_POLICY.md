# Privacy Policy for Sound'ST Boost

**Last updated:** January 2027  
**Applies to:** Sound'ST Boost version 1.5.0 and later  
**Developer:** 1sthillman  
**Contact:** https://github.com/1sthillman/soundboost/issues

Sound'ST Boost ("the app") is an audio enhancement app for Android. This policy explains what the app does with your data, in plain language.

---

## 1. Summary

**The developer does not intentionally collect, receive, or store your personal data.**

- The app has no user accounts, no advertising, and no developer servers.
- Your settings are stored on your device.
- The app uses the network only for Party Mode, to talk to other devices you connect to on the same local network. This traffic is not encrypted and has no password (see Section 4).
- Audio is processed in real time. The app does not save recordings.

---

## 2. Data stored on your device

The app stores the following locally, using Android's DataStore / SharedPreferences:

- Audio settings: volume boost level, equalizer bands, bass boost, virtualizer, presets (including custom presets)
- Appearance and language preferences, auto-start-on-boot preference
- Per-app profiles and other on-device preferences (for example acknowledgements of warnings)
- **Party Mode:** device name (by default your phone's model name, can be changed), flash warning acceptance

**Note on Bluetooth device data:** Earlier versions may have stored Bluetooth device names and addresses when using per-device audio profiles. That feature is hidden in this version. If you used it in the past, that data may still be in local storage and is deleted when you uninstall the app.

This data stays on your device and is deleted when you uninstall the app.

**Android backup:** The app allows Android's automatic backup (allowBackup=true). Depending on your device and Google account settings, the app's local settings may be included in your Google account backup and restored to a new device. That backup is handled by Google, not by the developer.

---

## 3. Permissions

| Permission | Why the app uses it |
|------------|---------------------|
| MODIFY_AUDIO_SETTINGS | Apply audio effects (volume boost, equalizer, bass boost, virtualizer). |
| RECORD_AUDIO | Used for audio analysis of what your device is playing (spectrum visualizer) and, in Party Mode, to capture your device's media playback so it can be shared with devices in your room. It is not used to record your microphone for these features. You can deny it; the core boost and equalizer continue to work. |
| FOREGROUND_SERVICE and related types | Keep the audio boost and Party Mode running while the screen is off or the app is in the background, with a visible notification. |
| POST_NOTIFICATIONS | Show the status notification and quick controls. |
| RECEIVE_BOOT_COMPLETED | Start the boost after a restart, only if you turn on auto-start. |
| INTERNET, ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_MULTICAST_STATE | Party Mode: discover and connect to other devices on your local network. The app does not send data to the developer or to internet servers. |
| WAKE_LOCK | Keep Party Mode running reliably while the screen is locked. |
| FOREGROUND_SERVICE_MEDIA_PROJECTION | Party Mode host: capture the host device's audio playback to stream it to other devices in the room. |

The flashlight features use Android's flashlight (torch) control. They do not take photos or access the camera image. **The CAMERA permission was removed in version 1.5.0** because DJ Gesture Control (the originally planned camera feature) is not implemented.

Android may also show a system prompt when Party Mode captures your device's audio playback. The app captures audio only; it does not record your screen.

---

## 4. Party Mode (devices connecting to each other)

Party Mode lets several phones connect, play music together, and flash their flashlights in time with the music.

**How devices find each other:** on the same local network using Android network service discovery, or by entering the host's address manually. There is no central server.

**What can be sent between devices in a room:**

- the device name (by default your phone's model name; it can be changed in the app)
- the host's local network address (visible to devices that connect)
- flashlight timing and beat information (bass energy, beat detection)
- an audio stream captured from the host device's media playback
- the names of music files and the file contents, if the host shares music files
- playlist information, playback commands, and DJ mixer settings

**Important security notes:**

- **Party Mode traffic is not encrypted** (plain WebSocket, ws://).
- **Rooms have no password.** Anyone on the same network who discovers the room can join it and can see the data listed above, and could send commands to the room.
- Use Party Mode only on networks you trust. **Avoid public Wi-Fi.**
- Only share music you have the right to share.
- Party Mode listens for connections on your device only while hosting is turned on.
- **WebSocket frame size is limited** to prevent abuse (server: 256 KB, client: 1 MB). This is enough for normal Party Mode messages including audio streaming.

None of this data is sent to the developer or to any third-party server.

---

## 5. Flashlight and photosensitivity

The flashlight features can produce rapidly flashing light. **Flashing light can trigger seizures in people with photosensitive epilepsy.** Do not use these features if you or anyone nearby may be affected.

**Safety measures in version 1.5.0:**

- A warning dialog is shown before you can use flashlight features for the first time. You must acknowledge the warning to proceed.
- The app enforces a minimum 333 ms interval between torch flashes to reduce risk.
- Flash parameters from Party Mode are validated and clamped to safe ranges (duration: 10-1000 ms, repeat max: 20, interval min: 333 ms).

---

## 6. Third parties

The app does not include advertising, analytics, or crash-reporting services, and does not use user accounts or payments.

**TensorFlow Lite libraries** are included in the build but are not used in version 1.5.0. They were added for a planned AI vocal separation feature that is not implemented.

---

## 7. Children

The app is not designed to collect personal information from anyone, including children, and does not knowingly do so.

---

## 8. Deleting your data

Uninstall the app to delete everything it stores on your device. Data that your device has already included in a Google account backup is managed through your Google account settings.

---

## 9. Changes to this policy

When the app's data practices change, this policy will be updated here and the "Last updated" date will change.

---

## 10. Contact

Questions or requests: https://github.com/1sthillman/soundboost/issues

---

## Version history

- **v1.5.0 (January 2027):** Removed CAMERA permission (DJ Gesture Control not implemented). Added Party Mode security details (unencrypted, no password, frame size limits). Added flashlight safety features (warning dialog, 333ms rate limit, parameter validation). Hidden Call Enhancement and Bluetooth Profiles features. Clarified that TensorFlow libraries are included but unused.
- **v1.4.x (2026):** Earlier versions (details omitted for brevity).
