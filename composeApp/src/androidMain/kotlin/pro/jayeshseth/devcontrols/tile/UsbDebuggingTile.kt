package pro.jayeshseth.devcontrols.tile

class UsbDebuggingTile : BaseToggleTile() {
    override fun isActive(): Boolean = repo.currentUsbDebugging()
    override suspend fun setActive(enabled: Boolean) = repo.setUsbDebugging(enabled)
}
