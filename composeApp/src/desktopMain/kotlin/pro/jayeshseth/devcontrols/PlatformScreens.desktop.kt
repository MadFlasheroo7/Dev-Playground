package pro.jayeshseth.devcontrols

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.ui.DefaultHome
import pro.jayeshseth.devcontrols.ui.MissingScreen
import pro.jayeshseth.devcontrols.ui.shaders.*

@Composable
actual fun BackHandlerCommon(enabled: Boolean, onBack: () -> Unit) {
    // No system back on desktop; window/Esc handling can be wired later.
}

@Composable
actual fun PlatformHome(
    scroll: ScrollState,
    expandedCats: SnapshotStateMap<String, Boolean>,
    onNavigate: (Route) -> Unit,
) {
    DefaultHome(scroll, expandedCats, onNavigate)
}

@Composable
actual fun PlatformScreenHost(route: Route, onBack: () -> Unit) {
    when (route) {
        Route.MANDELBROT -> MandelbrotLabScreenMultiplatform(onBack)
        Route.DISSOLVE -> DissolveLabScreenMultiplatform(onBack)
        Route.HALFTONE -> HalftoneLabScreenMultiplatform(onBack)
        Route.RAYMARCH -> RaymarchSdfLabScreenMultiplatform(onBack)
        Route.RIPPLE -> RippleShaderLabScreenMultiplatform(onBack)
        Route.VORONOI -> VoronoiLabScreenMultiplatform(onBack)
        Route.METABALLS -> MetaballsLabScreenMultiplatform(onBack)
        Route.TOUCH_RIPPLE -> TouchRippleWaterScreenMultiplatform(onBack)
        else -> MissingScreen(route, onBack)
    }
}
