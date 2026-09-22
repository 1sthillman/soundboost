package com.soundboost.sync

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
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
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) = Unit
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
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceLost(service: NsdServiceInfo) = Unit

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType != SERVICE_TYPE) return
                nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(info: NsdServiceInfo) {
                        trySend(
                            DiscoveredRoom(
                                displayName = info.serviceName.removePrefix("SoundBoost-"),
                                hostAddress = info.host.hostAddress ?: return,
                                port = info.port
                            )
                        )
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
        private const val SERVICE_TYPE = "_soundboostsync._tcp."
    }
}

data class DiscoveredRoom(
    val displayName: String,
    val hostAddress: String,
    val port: Int
)
