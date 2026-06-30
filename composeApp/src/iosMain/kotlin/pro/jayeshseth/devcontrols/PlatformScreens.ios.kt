package pro.jayeshseth.devcontrols

import androidx.compose.runtime.Composable
import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.ui.MissingScreen
import pro.jayeshseth.devcontrols.ui.shaders.*

@Composable
actual fun BackHandlerCommon(enabled: Boolean, onBack: () -> Unit) { MissingScreen(route = Route.MAIN, onBack = onBack) }

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
