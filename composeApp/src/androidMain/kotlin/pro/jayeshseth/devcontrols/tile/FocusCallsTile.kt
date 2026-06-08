package pro.jayeshseth.devcontrols.tile

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import pro.jayeshseth.devcontrols.data.FocusCallsPrefs

class FocusCallsTile : BaseToggleTile() {
    private val prefs by lazy { FocusCallsPrefs.get(applicationContext) }

    override fun isActive(): Boolean = runBlocking { prefs.focusCallsEnabled.first() }
    override suspend fun setActive(enabled: Boolean): Result<Unit> = runCatching {
        prefs.setFocusCallsEnabled(enabled)
    }

    override fun activeLabel(): String = "Active"
    override fun inactiveLabel(): String = "Off"
}
