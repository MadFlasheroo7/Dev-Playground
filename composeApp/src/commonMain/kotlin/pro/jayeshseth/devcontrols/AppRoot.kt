package pro.jayeshseth.devcontrols

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import pro.jayeshseth.devcontrols.nav.CommonScreen
import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.ui.theme.DevControlsTheme

/**
 * The cross-platform app shell: hold the current [Route], render the home, a commonMain screen,
 * or delegate to the platform's screen host. Scroll/expansion state is hoisted here so it survives
 * navigating into a screen and back (the home leaves/re-enters composition).
 */
@Composable
fun AppRoot(initialRoute: Route = Route.MAIN) {
    DevControlsTheme {
        var route by remember { mutableStateOf(initialRoute) }
        val homeScroll = rememberScrollState()
        val expandedCats = remember { mutableStateMapOf<String, Boolean>() }

        BackHandlerCommon(enabled = route != Route.MAIN) { route = Route.MAIN }

        if (route == Route.MAIN) {
            PlatformHome(homeScroll, expandedCats) { route = it }
        } else if (!CommonScreen(route, onBack = { route = Route.MAIN })) {
            PlatformScreenHost(route) { route = Route.MAIN }
        }
    }
}

/** Android delegates to androidx BackHandler; desktop/web/iOS are no-ops (wired later). */
@Composable
expect fun BackHandlerCommon(enabled: Boolean, onBack: () -> Unit)

/** The home menu. Android renders the full DevControlsScreen; other platforms render a default list. */
@Composable
expect fun PlatformHome(
    scroll: ScrollState,
    expandedCats: SnapshotStateMap<String, Boolean>,
    onNavigate: (Route) -> Unit,
)

/** Renders an Android-only route on Android; other platforms show a "not available here" screen. */
@Composable
expect fun PlatformScreenHost(route: Route, onBack: () -> Unit)
