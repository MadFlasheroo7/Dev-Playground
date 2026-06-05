package pro.jayeshseth.devcontrols.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

data class PairingEndpoint(val host: String, val port: Int) {
    override fun toString(): String = "$host:$port"
}

/**
 * Discovers the device's `_adb-tls-pairing._tcp` service. This service is only
 * broadcast while Settings → Developer options → Wireless debugging →
 * "Pair device with pairing code" is on screen. The discoverer is intended to
 * be started when the user opens our pairing dialog and stopped when they close
 * it.
 */
class AdbPairingDiscoverer(context: Context) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    val endpoint: Flow<PairingEndpoint?> = callbackFlow {
        trySend(null)

        // NsdManager rejects reusing a ResolveListener while a prior resolve is
        // still in flight. Build a fresh one per onServiceFound dispatch.
        fun newResolveListener() = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "pair resolve failed code=$errorCode")
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                val host = serviceInfo.host?.hostAddress
                if (host != null) {
                    trySend(PairingEndpoint(host, serviceInfo.port))
                }
            }
        }

        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "pair start discovery failed code=$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                runCatching {
                    @Suppress("DEPRECATION")
                    nsd.resolveService(serviceInfo, newResolveListener())
                }.onFailure { Log.w(TAG, "resolveService threw", it) }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                trySend(null)
            }
        }

        try {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (t: Throwable) {
            Log.e(TAG, "discoverServices threw", t)
        }

        awaitClose {
            runCatching { nsd.stopServiceDiscovery(discoveryListener) }
        }
    }.distinctUntilChanged()

    companion object {
        const val SERVICE_TYPE = "_adb-tls-pairing._tcp."
        private const val TAG = "AdbPairingDiscoverer"
    }
}
