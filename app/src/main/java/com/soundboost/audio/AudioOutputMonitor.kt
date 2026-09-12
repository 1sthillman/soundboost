package com.soundboost.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Ses çıkış cihazlarını izler ve tespit eder
 * 
 * Desteklenen Cihazlar:
 * - Telefon hoparlörü (built-in speaker)
 * - Kablolu kulaklık (3.5mm jack)
 * - Bluetooth kulaklık (A2DP)
 * - Bluetooth hoparlör
 * - USB-C kulaklık/DAC
 */
class AudioOutputMonitor(private val context: Context) {

    companion object {
        private const val TAG = "AudioOutputMonitor"
    }

    enum class OutputDevice {
        PHONE_SPEAKER,      // Telefon hoparlörü
        WIRED_HEADSET,      // Kablolu kulaklık
        WIRED_HEADPHONE,    // Kablolu kulaklık (mikrofonsuz)
        BLUETOOTH_HEADSET,  // Bluetooth kulaklık
        BLUETOOTH_SPEAKER,  // Bluetooth hoparlör
        USB_HEADSET,        // USB-C kulaklık
        USB_DEVICE,         // USB DAC
        UNKNOWN
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var currentDevice: OutputDevice = OutputDevice.UNKNOWN
    private var deviceName: String? = null
    
    // Listener for device changes
    var onDeviceChanged: ((OutputDevice, String?) -> Unit)? = null

    /**
     * Mevcut ses çıkış cihazını tespit et
     */
    fun detectCurrentDevice(): Pair<OutputDevice, String?> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            
            for (device in devices) {
                when (device.type) {
                    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                        val name = device.productName?.toString() ?: "Bluetooth Device"
                        Log.d(TAG, "🎧 Bluetooth kulaklık tespit edildi: $name")
                        currentDevice = OutputDevice.BLUETOOTH_HEADSET
                        deviceName = name
                        return Pair(OutputDevice.BLUETOOTH_HEADSET, name)
                    }
                    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                        val name = device.productName?.toString() ?: "Bluetooth Headset"
                        Log.d(TAG, "📞 Bluetooth handsfree tespit edildi: $name")
                        currentDevice = OutputDevice.BLUETOOTH_HEADSET
                        deviceName = name
                        return Pair(OutputDevice.BLUETOOTH_HEADSET, name)
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                        Log.d(TAG, "🎧 Kablolu kulaklık tespit edildi")
                        currentDevice = OutputDevice.WIRED_HEADSET
                        return Pair(OutputDevice.WIRED_HEADSET, "Wired Headset")
                    }
                    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                        Log.d(TAG, "🎧 Kablolu kulaklık (mikrofonsuz) tespit edildi")
                        currentDevice = OutputDevice.WIRED_HEADPHONE
                        return Pair(OutputDevice.WIRED_HEADPHONE, "Wired Headphones")
                    }
                    AudioDeviceInfo.TYPE_USB_HEADSET -> {
                        Log.d(TAG, "🔌 USB-C kulaklık tespit edildi")
                        currentDevice = OutputDevice.USB_HEADSET
                        return Pair(OutputDevice.USB_HEADSET, "USB-C Headset")
                    }
                    AudioDeviceInfo.TYPE_USB_DEVICE -> {
                        val name = device.productName?.toString() ?: "USB DAC"
                        Log.d(TAG, "🔌 USB DAC tespit edildi: $name")
                        currentDevice = OutputDevice.USB_DEVICE
                        deviceName = name
                        return Pair(OutputDevice.USB_DEVICE, name)
                    }
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                        Log.d(TAG, "📢 Telefon hoparlörü tespit edildi")
                        currentDevice = OutputDevice.PHONE_SPEAKER
                        return Pair(OutputDevice.PHONE_SPEAKER, "Phone Speaker")
                    }
                }
            }
        } else {
            // Eski Android sürümleri için fallback
            return detectDeviceLegacy()
        }

        Log.d(TAG, "ℹ️ Varsayılan cihaz: Telefon hoparlörü")
        currentDevice = OutputDevice.PHONE_SPEAKER
        return Pair(OutputDevice.PHONE_SPEAKER, "Phone Speaker")
    }

    /**
     * Android 5.x ve altı için cihaz tespiti
     */
    private fun detectDeviceLegacy(): Pair<OutputDevice, String?> {
        return when {
            audioManager.isBluetoothA2dpOn -> {
                Log.d(TAG, "🎧 Bluetooth aktif (legacy)")
                currentDevice = OutputDevice.BLUETOOTH_HEADSET
                Pair(OutputDevice.BLUETOOTH_HEADSET, "Bluetooth Device")
            }
            audioManager.isWiredHeadsetOn -> {
                Log.d(TAG, "🎧 Kablolu kulaklık aktif (legacy)")
                currentDevice = OutputDevice.WIRED_HEADSET
                Pair(OutputDevice.WIRED_HEADSET, "Wired Headset")
            }
            else -> {
                Log.d(TAG, "📢 Telefon hoparlörü aktif (legacy)")
                currentDevice = OutputDevice.PHONE_SPEAKER
                Pair(OutputDevice.PHONE_SPEAKER, "Phone Speaker")
            }
        }
    }

    /**
     * Bluetooth codec bilgisini al (Android 8.0+)
     */
    fun getBluetoothCodec(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter?.isEnabled == true) {
                    // Codec bilgisi almak için BluetoothA2dp profili gerekli
                    // Bu bilgi genellikle developer options'tan erişilebilir
                    return "Available" // Gerçek codec tespiti için daha fazla izin gerekir
                }
            } catch (e: Exception) {
                Log.w(TAG, "Bluetooth codec bilgisi alınamadı: ${e.message}")
            }
        }
        return null
    }

    /**
     * Cihaz değişikliklerini izlemeye başla
     */
    fun startMonitoring() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
        }

        context.registerReceiver(audioDeviceReceiver, filter)
        Log.d(TAG, "📡 Ses cihazı izleme başlatıldı")

        // İlk tespit
        val (device, name) = detectCurrentDevice()
        onDeviceChanged?.invoke(device, name)
    }

    /**
     * İzlemeyi durdur
     */
    fun stopMonitoring() {
        try {
            context.unregisterReceiver(audioDeviceReceiver)
            Log.d(TAG, "📡 Ses cihazı izleme durduruldu")
        } catch (e: Exception) {
            Log.w(TAG, "Receiver zaten kayıtlı değil: ${e.message}")
        }
    }

    /**
     * Bluetooth veya kablolu cihaz değişikliklerini dinle
     */
    private val audioDeviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                AudioManager.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    val name = intent.getStringExtra("name") ?: "Headset"
                    
                    if (state == 1) {
                        Log.d(TAG, "🔌 Kulaklık takıldı: $name")
                        currentDevice = OutputDevice.WIRED_HEADSET
                        deviceName = name
                        onDeviceChanged?.invoke(OutputDevice.WIRED_HEADSET, name)
                    } else if (state == 0) {
                        Log.d(TAG, "🔌 Kulaklık çıkarıldı")
                        val (device, deviceName) = detectCurrentDevice()
                        onDeviceChanged?.invoke(device, deviceName)
                    }
                }
                
                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED,
                BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                    Log.d(TAG, "🔄 Bluetooth durumu değişti")
                    val (device, name) = detectCurrentDevice()
                    onDeviceChanged?.invoke(device, name)
                }
                
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    Log.d(TAG, "⚠️ Ses cihazı aniden kesildi")
                    val (device, name) = detectCurrentDevice()
                    onDeviceChanged?.invoke(device, name)
                }
            }
        }
    }

    /**
     * Cihaz türüne göre önerilen efekt seviyesi
     */
    fun getRecommendedGainForDevice(device: OutputDevice): Int {
        return when (device) {
            OutputDevice.PHONE_SPEAKER -> 180      // Hoparlör için maksimum
            OutputDevice.BLUETOOTH_HEADSET -> 140  // Bluetooth için orta (kendi amplifikatörü var)
            OutputDevice.BLUETOOTH_SPEAKER -> 160  // Bluetooth hoparlör için yüksek
            OutputDevice.WIRED_HEADSET -> 150      // Kablolu kulaklık için orta-yüksek
            OutputDevice.WIRED_HEADPHONE -> 150    // Kablolu kulaklık için orta-yüksek
            OutputDevice.USB_HEADSET -> 140        // USB kulaklık (genellikle DAC'li)
            OutputDevice.USB_DEVICE -> 130         // USB DAC (yüksek kalite, az gain)
            OutputDevice.UNKNOWN -> 150            // Varsayılan
        }
    }

    /**
     * Mevcut cihazı döndür
     */
    fun getCurrentDevice(): OutputDevice = currentDevice

    /**
     * Mevcut cihaz adını döndür
     */
    fun getCurrentDeviceName(): String? = deviceName
}
