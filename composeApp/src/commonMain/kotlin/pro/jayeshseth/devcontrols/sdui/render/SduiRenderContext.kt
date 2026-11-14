package pro.jayeshseth.devcontrols.sdui.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import pro.jayeshseth.devcontrols.sdui.registry.SduiComponentRegistry
import pro.jayeshseth.devcontrols.sdui.state.SduiActionRunner
import pro.jayeshseth.devcontrols.sdui.state.SduiStateStore

/**
 * Context provided down the Compose SDUI tree via CompositionLocal.
 */
data class SduiRenderContext(
    val store: SduiStateStore,
    val actionRunner: SduiActionRunner,
    val componentRegistry: SduiComponentRegistry = SduiComponentRegistry.default
)

val LocalSduiContext = compositionLocalOf<SduiRenderContext> {
    error("LocalSduiContext not provided. Wrap your UI tree with SduiView or provide LocalSduiContext.")
}

@Composable
fun currentSduiContext(): SduiRenderContext = LocalSduiContext.current
