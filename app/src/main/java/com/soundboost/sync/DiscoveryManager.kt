package com.soundboost.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Aynı Wi-Fi ağındaki cihazların birbirini bulması için NSD (Network Service Discovery).
 * NSD başarısız olursa (bazı router/AP izole modları) SyncRoomScreen host adresini
 * QR kod olarak gösterir, client kamera ile okuyup manuel bağlanır — bu yüzden
 * NSD burada "en iyi çaba" (best-effort), tek yol değil.
 */
class DiscoveryManager(context: Context) {

    private val nsdManager = context.applicationContext
        .getSystemService(Context.NSD_SERVICE) as NsdManager

    private var registrationListener: NsdManager.RegistrationListener? = null

    /** Host tarafı: odayı ağda "com.soundboost.sync" servis tipiyle duyurur. */
    fun advertise(roomName: String, port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "SoundBoost-$roomName"
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "NSD service registered: ${info.serviceName}")
            }
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD registration failed: errorCode=$errorCode")
            }
            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(TAG, "NSD service unregistered")
            }
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "NSD unregistration failed: errorCode=$errorCode")
            }
        }
        registrationListener = listener
        runCatching { nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, listener) }
    }

    fun stopAdvertising() {
        registrationListener?.let { runCatching { nsdManager.unregisterService(it) } }
        registrationListener = null
    }

    /** Client tarafı: ağdaki SoundBoost odalarını akış olarak döner. */
    fun discoverRooms(): Flow<DiscoveredRoom> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "NSD discovery started")
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "NSD discovery start failed: errorCode=$errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "NSD discovery stop failed: errorCode=$errorCode")
            }
            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "NSD discovery stopped")
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "NSD service lost: ${service.serviceName}")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType != SERVICE_TYPE) return
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "NSD resolve failed: ${info.serviceName}, errorCode=$errorCode")
                    }
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        val hostAddress = info.host?.hostAddress
                        if (hostAddress != null) {
                            trySend(
                                DiscoveredRoom(
                                    displayName = info.serviceName.removePrefix("SoundBoost-"),
                                    hostAddress = hostAddress,
                                    port = info.port
                                )
                            )
                            Log.d(TAG, "NSD resolved: ${info.serviceName} at $hostAddress:${info.port}")
                        }
                    }
                })
            }
        }

        runCatching { nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener) }

        awaitClose {
            runCatching { nsdManager.stopServiceDiscovery(discoveryListener) }
        }
    }

    companion object {
        private const val TAG = "DiscoveryManager"
        private const val SERVICE_TYPE = "_soundboostsync._tcp."
    }
}

data class DiscoveredRoom(
    val displayName: String,
    val hostAddress: String,
    val port: Int
)
