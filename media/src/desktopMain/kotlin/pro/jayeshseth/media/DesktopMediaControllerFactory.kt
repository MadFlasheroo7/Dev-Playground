package pro.jayeshseth.media

/**
 * Builds the best available desktop [MediaController]: **mpv** if its binary is installed (preferred —
 * no native lib to install, plays opus/webm headless), otherwise the **VLCJ** controller (needs native
 * libVLC). If neither backend can actually play, extraction/search still work and playback simply no-ops.
 */
fun createDesktopMediaController(): MediaController =
    if (MpvMediaController.isAvailable()) MpvMediaController() else DesktopMediaController()
