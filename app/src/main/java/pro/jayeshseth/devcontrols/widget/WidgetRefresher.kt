package pro.jayeshseth.devcontrols.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.AdbEndpointDiscoverer
import pro.jayeshseth.devcontrols.data.FocusCallsPrefs
import pro.jayeshseth.devcontrols.data.KioskPrefs

/**
 * Coalesces every state signal that should redraw the Glance widget into a
 * single debounced trigger. Settings.Global / Settings.System changes are
 * already wired by `DevSettingsRepository.registerWidgetRefresh()`; this class
 * covers the DataStore-backed state (Kiosk active / target, Focus Calls master
 * toggle) and the NSD-discovered ADB endpoint, all of which the widget
 * snapshot reads synchronously and would otherwise go stale.
 */
@OptIn(FlowPreview::class)
class WidgetRefresher(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        val kiosk = KioskPrefs.get(context)
        val focus = FocusCallsPrefs.get(context)
        val nsd = AdbEndpointDiscoverer.get(context)

        // Per-source: dedupe, drop initial replay, normalise to Unit so we can merge.
        fun <T> Flow<T>.signal(): Flow<Unit> =
            distinctUntilChanged().drop(1).map { Unit }

        val signals: Flow<Unit> = merge(
            kiosk.kioskActive.signal(),
            kiosk.targetPackage.signal(),
            focus.focusCallsEnabled.signal(),
            nsd.endpoint.signal(),
        )

        scope.launch {
            signals
                .debounce(DEBOUNCE_MS)
                .collectLatest {
                    runCatching { DevControlsWidget().updateAll(context) }
                        .onFailure { Log.w(TAG, "updateAll failed", it) }
                }
        }
    }

    companion object {
        private const val TAG = "WidgetRefresher"
        private const val DEBOUNCE_MS = 200L
    }
}
