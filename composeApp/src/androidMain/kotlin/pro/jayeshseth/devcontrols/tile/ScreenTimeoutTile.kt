package pro.jayeshseth.devcontrols.tile

class ScreenTimeoutTile : BaseToggleTile() {
    override fun isActive(): Boolean = repo.currentNeverTimeout()
    override suspend fun setActive(enabled: Boolean) = repo.setNeverTimeout(enabled)
    override fun activeLabel(): String = "Never"
    override fun inactiveLabel(): String = "Default"
}
