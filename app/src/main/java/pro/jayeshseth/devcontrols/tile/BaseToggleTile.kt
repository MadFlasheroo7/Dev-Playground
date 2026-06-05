package pro.jayeshseth.devcontrols.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.DevSettingsRepository
import pro.jayeshseth.devcontrols.widget.DevControlsWidget

/**
 * Shared skeleton for the four simple toggle tiles (Developer options, USB
 * debugging, Wireless debugging, Screen timeout). Subclasses supply how to read the
 * current state and how to write the new one; this base wires the [TileService]
 * lifecycle, flips the [Tile] state on click, and refreshes the Glance widget via
 * `updateAll` so all surfaces stay consistent. `KioskModeTile` / `FocusCallsTile`
 * have bespoke behaviour and do not extend this.
 */

abstract class BaseToggleTile : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    protected val repo: DevSettingsRepository by lazy {
        DevSettingsRepository.get(applicationContext)
    }

    protected abstract fun isActive(): Boolean
    protected abstract suspend fun setActive(enabled: Boolean): Result<Unit>
    protected open fun activeLabel(): String = "On"
    protected open fun inactiveLabel(): String = "Off"

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val target = !isActive()
        scope.launch {
            val result = setActive(target)
            refresh()
            if (result.isSuccess) {
                try {
                    DevControlsWidget().updateAll(applicationContext)
                } catch (t: Throwable) {
                    Log.w("BaseToggleTile", "widget update failed", t)
                }
            }
        }
    }

    protected fun refresh() {
        val tile = qsTile ?: return
        val active = isActive()
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (active) activeLabel() else inactiveLabel()
        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
