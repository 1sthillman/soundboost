package com.soundboost.data

import kotlinx.serialization.Serializable

/**
 * Audio Device Profile - Stores user preferences per audio output device type
 * 
 * PRIVACY COMPLIANT: Only stores generic device TYPE, NOT device identifiers
 * ❌ NOT stored: Bluetooth name, MAC address, serial number, pairing history
 * ✅ Stored: Device type enum + user's preferred settings
 */
@Serializable
data class DeviceProfile(
    val deviceType: AudioDeviceType,
    val volumeBoostPercent: Int = 150,  // User's preferred boost level
    val bassBoostPercent: Int = 0,
    val virtualizerPercent: Int = 0,
    val equalizerPresetName: String? = null,  // e.g. "Rock", "Vocal Clarity"
    val autoSwitch: Boolean = true  // Automatically apply this profile when device connected
)

/**
 * Generic audio device types (NO identifying information)
 */
enum class AudioDeviceType {
    PHONE_SPEAKER,      // Built-in speaker
    WIRED_HEADSET,      // 3.5mm jack or USB-C wired headphones
    BLUETOOTH_DEVICE,   // Any Bluetooth audio device
    USB_DEVICE,         // USB audio device
    UNKNOWN;            // Fallback for unsupported types
    
    fun getDisplayName(): String = when (this) {
        PHONE_SPEAKER -> "Phone Speaker"
        WIRED_HEADSET -> "Wired Headphones"
        BLUETOOTH_DEVICE -> "Bluetooth Audio"
        USB_DEVICE -> "USB Audio"
        UNKNOWN -> "Unknown Device"
    }
    
    fun getIcon(): String = when (this) {
        PHONE_SPEAKER -> "📱"
        WIRED_HEADSET -> "🎧"
        BLUETOOTH_DEVICE -> "📡"
        USB_DEVICE -> "🔌"
        UNKNOWN -> "❓"
    }
}
