package pro.jayeshseth.devcontrols.tile

class WirelessDebuggingTile : BaseToggleTile() {
    override fun isActive(): Boolean = repo.currentWirelessDebugging()
    override suspend fun setActive(enabled: Boolean) = repo.setWirelessDebugging(enabled)
}
