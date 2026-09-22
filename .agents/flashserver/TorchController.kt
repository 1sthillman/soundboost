package com.soundboost.flash

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Fiziksel torch (kamera flaşı) kontrolü. İKİNCİL çıktı — ScreenFlashOverlay birincil,
 * çünkü CAMERA izni gerektirmez ve gecikmesi öngörülebilir. Torch sadece kullanıcı
 * SettingsScreen'de açıkça etkinleştirirse devreye girer (SyncPreferences.torchEnabled).
 *
 * Not: Bazı OEM'lerde (özellikle kamera arka planda kullanılıyorken) torch API'si
 * ~50-150ms gecikmeyle tepki verebilir — bu yüzden zamanlamayı garanti etmez,
 * "best effort" ikincil efekt olarak sunulmalı.
 */
class TorchController(context: Context) {

    private val cameraManager = context.applicationContext
        .getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val torchCameraId: String? by lazy { findTorchCapableCameraId() }

    private var isAvailable = false

    init {
        isAvailable = torchCameraId != null
    }

    fun isSupported(): Boolean = isAvailable

    fun setTorch(on: Boolean) {
        val id = torchCameraId ?: return
        runCatching {
            cameraManager.setTorchMode(id, on)
        }.onFailure {
            Log.w(TAG, "Torch modu ayarlanamadi: ${it.message}")
        }
    }

    /** Tek darbe: [durationMs] süresince yanar, sonra söner. */
    suspend fun pulse(durationMs: Int) {
        setTorch(true)
        delay(durationMs.toLong())
        setTorch(false)
    }

    /** [repeatCount] kez, aralarında [intervalMs] boşlukla strobe. */
    suspend fun strobe(durationMs: Int, repeatCount: Int, intervalMs: Int) {
        repeat(repeatCount) { index ->
            pulse(durationMs)
            if (index != repeatCount - 1) delay(intervalMs.toLong())
        }
    }

    private fun findTorchCapableCameraId(): String? {
        return runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
                hasFlash == true
            }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "TorchController"

        fun isTorchApiAvailable(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
}
