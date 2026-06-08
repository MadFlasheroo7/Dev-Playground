package pro.jayeshseth.devcontrols

import android.app.Application
import android.util.Log
import org.conscrypt.Conscrypt
import pro.jayeshseth.devcontrols.call.CallNotifications
import pro.jayeshseth.devcontrols.data.AdbEndpointDiscoverer
import pro.jayeshseth.devcontrols.data.DevSettingsRepository
import pro.jayeshseth.devcontrols.kiosk.KioskNotification
import pro.jayeshseth.devcontrols.widget.WidgetRefresher
import java.security.Security

/**
 * Process entry point. On [onCreate] it: installs the Conscrypt security provider
 * (required for the TLS-PSK handshake during embedded-ADB pairing), creates the
 * notification channels, registers the widget-refresh ContentObservers
 * ([DevSettingsRepository.registerWidgetRefresh]), starts [WidgetRefresher]
 * (DataStore+NSD → updateAll), and starts [AdbEndpointDiscoverer] so the widget
 * can display the wireless-debug endpoint. See ARCHITECTURE.md → cross-cutting patterns.
 */
class DevControlsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { Security.insertProviderAt(Conscrypt.newProvider(), 1) }
            .onFailure { Log.w("DevControlsApp", "Conscrypt insert failed", it) }
        CallNotifications.ensureChannel(this)
        KioskNotification.ensureChannel(this)
        DevSettingsRepository.get(this).registerWidgetRefresh()
        AdbEndpointDiscoverer.get(this).start()
        WidgetRefresher(this).start()
    }
}
