package pro.jayeshseth.devcontrols.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdbEndpoint(val host: String, val port: Int) {
    override fun toString(): String = "$host:$port"
}

class AdbEndpointDiscoverer(context: Context) {

    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _endpoint = MutableStateFlow<AdbEndpoint?>(null)
    val endpoint: Flow<AdbEndpoint?> = _endpoint.asStateFlow()

    fun lastResolved(): AdbEndpoint? = _endpoint.value

    /**
     * NsdManager rejects reusing the same ResolveListener while a previous
     * resolve is still in flight ("listener already in use"). Build a fresh
     * one per invocation.
     */
    private fun newResolveListener() = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.w(TAG, "resolve failed code=$errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            @Suppress("DEPRECATION")
            val host = serviceInfo.host?.hostAddress
            if (host != null) {
                _endpoint.value = AdbEndpoint(host, serviceInfo.port)
            }
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "start discovery failed code=$errorCode")
            running = false
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.w(TAG, "stop discovery failed code=$errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String) {}
        override fun onDiscoveryStopped(serviceType: String) {
            running = false
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            runCatching {
                @Suppress("DEPRECATION")
                nsd.resolveService(serviceInfo, newResolveListener())
            }.onFailure { Log.w(TAG, "resolveService threw", it) }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            _endpoint.value = null
        }
    }

    @Volatile private var running = false

    @Synchronized
    fun start() {
        if (running) return
        try {
            nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            running = true
        } catch (t: Throwable) {
            Log.e(TAG, "discoverServices threw", t)
        }
    }

    companion object {
        const val SERVICE_TYPE = "_adb-tls-connect._tcp."
        private const val TAG = "AdbEndpointDiscoverer"

        @Volatile
        private var instance: AdbEndpointDiscoverer? = null

        fun get(context: Context): AdbEndpointDiscoverer =
            instance ?: synchronized(this) {
                instance ?: AdbEndpointDiscoverer(context.applicationContext).also { instance = it }
            }
    }
}
