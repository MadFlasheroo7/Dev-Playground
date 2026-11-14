package pro.jayeshseth.bugrush.audio

enum class SoundEffect {
    LASER_PEW,
    RAILGUN_BLAST,
    FLAK_BURST,
    DASH_WHOOSH,
    GEM_CHIME,
    BUG_POP,
    PLAYER_HIT,
    TRIVIA_CORRECT,
    TRIVIA_WRONG,
    BOSS_SPAWN_SIREN,
    KERNEL_PANIC,
    UPGRADE_UNLOCKED,
    DEFLECT
}

interface SoundPlayer {
    fun playSfx(sfx: SoundEffect)
    fun startBgm()
    fun stopBgm()
    fun pauseBgm()
    fun resumeBgm()
    fun startTitleTheme()
    fun stopTitleTheme()
    fun toggleMute()
    val isMuted: Boolean
}

object NoOpSoundPlayer : SoundPlayer {
    override fun playSfx(sfx: SoundEffect) {}
    override fun startBgm() {}
    override fun stopBgm() {}
    override fun pauseBgm() {}
    override fun resumeBgm() {}
    override fun startTitleTheme() {}
    override fun stopTitleTheme() {}
    override fun toggleMute() {}
    override val isMuted: Boolean = false
}

