package pro.jayeshseth.bugrush

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import pro.jayeshseth.bugrush.audio.DesktopChiptuneAudioPlayer
import pro.jayeshseth.bugrush.game.ConferenceGameStateHolder
import pro.jayeshseth.bugrush.ui.ConferenceGameApp
import pro.jayeshseth.bugrush.ui.DesktopSpaceBackground

fun main() = application {
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 760.dp
    )

    val audioPlayer = remember { DesktopChiptuneAudioPlayer() }
    val spaceBackground = remember { DesktopSpaceBackground() }
    val stateHolder = remember {
        ConferenceGameStateHolder(
            soundPlayer = audioPlayer,
            spaceBackground = spaceBackground
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Kotlin Bug Rush • Tech Conference Arcade",
        state = windowState,
        onKeyEvent = stateHolder::handleKeyEvent
    ) {
        ConferenceGameApp(stateHolder = stateHolder)
    }
}
