package com.soundboost.audio

import android.util.Log

/**
 * Akıllı Ses Güçlendirme Danışmanı
 * 
 * Cihaz türüne, ses çıkışına ve kullanım senaryosuna göre
 * optimal gain seviyesi önerir ve kullanıcıyı uyarır.
 */
class SmartGainAdvisor {

    companion object {
        private const val TAG = "SmartGainAdvisor"
        
        // Gain seviyeleri (%)
        const val MIN_GAIN = 60      // Minimum (-4dB)
        const val DEFAULT_GAIN = 100  // Normal (0dB)
        const val MAX_SAFE_GAIN = 200 // Güvenli maksimum (+15dB)
        const val MAX_EXTREME_GAIN = 500 // Aşırı maksimum (+60dB) - Hoparlör hasarı riski!
        
        // Uyarı seviyeleri
        const val WARNING_THRESHOLD = 200  // Bu seviyeden sonra uyar
        const val DANGER_THRESHOLD = 300   // Bu seviyeden sonra tehlike uyarısı
        const val CRITICAL_THRESHOLD = 400 // Bu seviyeden sonra kritik uyarı
    }

    /**
     * Uyarı seviyeleri
     */
    enum class WarningLevel {
        SAFE,       // 60-200%: Güvenli alan
        CAUTION,    // 201-300%: Dikkatli kullan
        WARNING,    // 301-400%: Uyarı - Yüksek risk
        DANGER      // 401-500%: TEHLİKE - Hoparlör hasarı riski!
    }

    /**
     * Cihaz bazlı öneriler
     */
    data class GainRecommendation(
        val device: AudioOutputMonitor.OutputDevice,
        val deviceName: String?,
        val recommendedGain: Int,
        val minSafeGain: Int,
        val maxSafeGain: Int,
        val maxAbsoluteGain: Int,
        val reason: String,
        val tip: String
    )

    /**
     * Kullanıcı uyarı mesajı
     */
    data class GainWarning(
        val level: WarningLevel,
        val currentGain: Int,
        val recommendedGain: Int,
        val title: String,
        val message: String,
        val emoji: String,
        val shouldShowDialog: Boolean
    )

    /**
     * Cihaz türüne göre optimal gain önerisi
     */
    fun getRecommendationForDevice(
        device: AudioOutputMonitor.OutputDevice,
        deviceName: String?
    ): GainRecommendation {
        return when (device) {
            AudioOutputMonitor.OutputDevice.PHONE_SPEAKER -> {
                GainRecommendation(
                    device = device,
                    deviceName = "Telefon Hoparlörü",
                    recommendedGain = 180,
                    minSafeGain = 120,
                    maxSafeGain = 200,
                    maxAbsoluteGain = 350, // Küçük hoparlör, çok yüksek gain hasara yol açar
                    reason = "Küçük hoparlör, maksimum güç gerekebilir",
                    tip = "Uzun süre %200 üzerinde kullanmayın - Hoparlör aşırı ısınabilir"
                )
            }

            AudioOutputMonitor.OutputDevice.BLUETOOTH_HEADSET -> {
                GainRecommendation(
                    device = device,
                    deviceName = deviceName ?: "Bluetooth Kulaklık",
                    recommendedGain = 140,
                    minSafeGain = 100,
                    maxSafeGain = 180,
                    maxAbsoluteGain = 300, // Bluetooth kulaklıklar kendi amp'leri var
                    reason = "Bluetooth kulaklıklar kendi amplifikatörlerine sahiptir",
                    tip = "Kulak sağlığınız için %200'ün altında tutun"
                )
            }

            AudioOutputMonitor.OutputDevice.BLUETOOTH_SPEAKER -> {
                GainRecommendation(
                    device = device,
                    deviceName = deviceName ?: "Bluetooth Hoparlör",
                    recommendedGain = 160,
                    minSafeGain = 120,
                    maxSafeGain = 220,
                    maxAbsoluteGain = 400, // Bluetooth hoparlörler güçlü
                    reason = "Bluetooth hoparlörler geniş alan için tasarlanmıştır",
                    tip = "Parti modunda %250'ye kadar çıkabilirsiniz"
                )
            }

            AudioOutputMonitor.OutputDevice.WIRED_HEADSET,
            AudioOutputMonitor.OutputDevice.WIRED_HEADPHONE -> {
                GainRecommendation(
                    device = device,
                    deviceName = "Kablolu Kulaklık",
                    recommendedGain = 150,
                    minSafeGain = 100,
                    maxSafeGain = 200,
                    maxAbsoluteGain = 350,
                    reason = "Kablolu bağlantı en iyi ses kalitesini sağlar",
                    tip = "Kulak sağlığınız için %200'ün altında tutun"
                )
            }

            AudioOutputMonitor.OutputDevice.USB_HEADSET -> {
                GainRecommendation(
                    device = device,
                    deviceName = "USB-C Kulaklık",
                    recommendedGain = 140,
                    minSafeGain = 100,
                    maxSafeGain = 180,
                    maxAbsoluteGain = 300,
                    reason = "USB kulaklıklar genellikle DAC içerir",
                    tip = "Yüksek kalite için %180'in altında tutun"
                )
            }

            AudioOutputMonitor.OutputDevice.USB_DEVICE -> {
                GainRecommendation(
                    device = device,
                    deviceName = deviceName ?: "USB DAC",
                    recommendedGain = 130,
                    minSafeGain = 100,
                    maxSafeGain = 160,
                    maxAbsoluteGain = 250, // Profesyonel ekipman, az gain yeterli
                    reason = "USB DAC profesyonel ses kalitesi sağlar",
                    tip = "Distortion'dan kaçınmak için %180'in altında tutun"
                )
            }

            AudioOutputMonitor.OutputDevice.UNKNOWN -> {
                GainRecommendation(
                    device = device,
                    deviceName = "Bilinmeyen Cihaz",
                    recommendedGain = 150,
                    minSafeGain = 100,
                    maxSafeGain = 200,
                    maxAbsoluteGain = 350,
                    reason = "Standart ayarlar",
                    tip = "Dikkatli kullanın, cihazınızı tanıyamadık"
                )
            }
        }
    }

    /**
     * Mevcut gain seviyesi için uyarı al
     */
    fun getWarningForGain(
        currentGain: Int,
        recommendation: GainRecommendation
    ): GainWarning? {
        return when {
            currentGain <= WARNING_THRESHOLD -> {
                // Güvenli alan, uyarı yok
                null
            }

            currentGain in (WARNING_THRESHOLD + 1)..DANGER_THRESHOLD -> {
                GainWarning(
                    level = WarningLevel.CAUTION,
                    currentGain = currentGain,
                    recommendedGain = recommendation.recommendedGain,
                    title = "⚠️ Dikkat",
                    message = "Ses seviyesi yüksek!\n\n" +
                            "Mevcut: %${currentGain}\n" +
                            "Önerilen: %${recommendation.recommendedGain}\n\n" +
                            "${recommendation.tip}",
                    emoji = "⚠️",
                    shouldShowDialog = false // Sadece bildirim
                )
            }

            currentGain in (DANGER_THRESHOLD + 1)..CRITICAL_THRESHOLD -> {
                GainWarning(
                    level = WarningLevel.WARNING,
                    currentGain = currentGain,
                    recommendedGain = recommendation.recommendedGain,
                    title = "⚠️ UYARI!",
                    message = "ÇOK YÜKSEK SES SEVİYESİ!\n\n" +
                            "Mevcut: %${currentGain}\n" +
                            "Önerilen: %${recommendation.recommendedGain}\n\n" +
                            "RİSKLER:\n" +
                            "• Hoparlör hasarı\n" +
                            "• Ses bozulması\n" +
                            "• Kulak sağlığı (kulaklık kullanıyorsanız)\n\n" +
                            "${recommendation.tip}",
                    emoji = "⚠️",
                    shouldShowDialog = true // Dialog göster
                )
            }

            currentGain > CRITICAL_THRESHOLD -> {
                GainWarning(
                    level = WarningLevel.DANGER,
                    currentGain = currentGain,
                    recommendedGain = recommendation.recommendedGain,
                    title = "🚨 TEHLİKE!",
                    message = "KRİTİK SES SEVİYESİ!\n\n" +
                            "Mevcut: %${currentGain}\n" +
                            "Maksimum Güvenli: %${recommendation.maxSafeGain}\n\n" +
                            "⚠️ CIDDI RISKLER:\n" +
                            "• Hoparlör kalıcı hasar görebilir\n" +
                            "• Ses tamamen bozulabilir\n" +
                            "• Kulaklıkta kulak hasarı riski\n" +
                            "• Garanti kapsamı dışı hasar\n\n" +
                            "❗ DERHAL %${recommendation.maxSafeGain} ALTINA İNDİRİN!",
                    emoji = "🚨",
                    shouldShowDialog = true // Zorunlu dialog
                )
            }

            else -> null
        }
    }

    /**
     * Gain seviyesi renk kodu (UI için)
     */
    fun getGainColorCode(gain: Int): String {
        return when {
            gain <= 150 -> "#4CAF50" // Yeşil - Güvenli
            gain <= 200 -> "#8BC34A" // Açık yeşil - İyi
            gain <= 250 -> "#FFC107" // Sarı - Dikkat
            gain <= 300 -> "#FF9800" // Turuncu - Uyarı
            gain <= 400 -> "#FF5722" // Kırmızı - Tehlike
            else -> "#F44336" // Koyu kırmızı - Kritik
        }
    }

    /**
     * Gain seviyesi emoji göstergesi
     */
    fun getGainEmoji(gain: Int): String {
        return when {
            gain <= 150 -> "🟢"
            gain <= 200 -> "🟡"
            gain <= 250 -> "🟠"
            gain <= 300 -> "🔴"
            gain <= 400 -> "⚠️"
            else -> "🚨"
        }
    }

    /**
     * Senaryo bazlı öneri (gelecek özellik)
     */
    enum class UsageScenario {
        MUSIC_LISTENING,    // Müzik dinleme
        GAMING,             // Oyun oynama
        PHONE_CALL,         // Telefon görüşmesi
        VIDEO_WATCHING,     // Video izleme
        PARTY_MODE,         // Parti modu
        SLEEP_MODE          // Uyku modu
    }

    /**
     * Kullanım senaryosuna göre öneri (gelecek feature)
     */
    fun getRecommendationForScenario(
        scenario: UsageScenario,
        device: AudioOutputMonitor.OutputDevice
    ): Int {
        return when (scenario) {
            UsageScenario.MUSIC_LISTENING -> when (device) {
                AudioOutputMonitor.OutputDevice.PHONE_SPEAKER -> 160
                AudioOutputMonitor.OutputDevice.BLUETOOTH_HEADSET -> 140
                else -> 150
            }
            UsageScenario.GAMING -> when (device) {
                AudioOutputMonitor.OutputDevice.PHONE_SPEAKER -> 180
                AudioOutputMonitor.OutputDevice.BLUETOOTH_HEADSET -> 160
                else -> 170
            }
            UsageScenario.PHONE_CALL -> when (device) {
                AudioOutputMonitor.OutputDevice.PHONE_SPEAKER -> 140
                AudioOutputMonitor.OutputDevice.BLUETOOTH_HEADSET -> 130
                else -> 130
            }
            UsageScenario.VIDEO_WATCHING -> when (device) {
                AudioOutputMonitor.OutputDevice.PHONE_SPEAKER -> 170
                AudioOutputMonitor.OutputDevice.BLUETOOTH_HEADSET -> 150
                else -> 160
            }
            UsageScenario.PARTY_MODE -> when (device) {
                AudioOutputMonitor.OutputDevice.BLUETOOTH_SPEAKER -> 250
                AudioOutputMonitor.OutputDevice.PHONE_SPEAKER -> 200
                else -> 180
            }
            UsageScenario.SLEEP_MODE -> 100 // Her cihazda düşük
        }
    }

    /**
     * Log uyarıları
     */
    fun logGainChange(gain: Int, device: AudioOutputMonitor.OutputDevice) {
        val emoji = getGainEmoji(gain)
        val recommendation = getRecommendationForDevice(device, null)
        
        Log.d(TAG, "$emoji Gain değişti: %$gain")
        Log.d(TAG, "Cihaz: ${recommendation.deviceName}")
        Log.d(TAG, "Önerilen: %${recommendation.recommendedGain}")
        
        if (gain > recommendation.maxSafeGain) {
            Log.w(TAG, "⚠️ GÜVENLİ SEVİYENİN ÜZERİNDE! (Max güvenli: %${recommendation.maxSafeGain})")
        }
        
        if (gain > recommendation.maxAbsoluteGain) {
            Log.e(TAG, "🚨 CİHAZ HASAR RİSKİ! (Max limit: %${recommendation.maxAbsoluteGain})")
        }
    }
}
