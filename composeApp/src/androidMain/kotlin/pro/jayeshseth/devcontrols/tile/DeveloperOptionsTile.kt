package pro.jayeshseth.devcontrols.tile

class DeveloperOptionsTile : BaseToggleTile() {
    override fun isActive(): Boolean = repo.currentDeveloperOptions()
    override suspend fun setActive(enabled: Boolean) = repo.setDeveloperOptions(enabled)
}
