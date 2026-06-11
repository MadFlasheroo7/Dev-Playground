package pro.jayeshseth.devcontrols

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.ui.DefaultHome
import pro.jayeshseth.devcontrols.ui.MissingScreen

@Composable
actual fun BackHandlerCommon(enabled: Boolean, onBack: () -> Unit) { /* iOS uses swipe-back / nav chrome */ }

@Composable
actual fun PlatformHome(
    scroll: ScrollState,
    expandedCats: SnapshotStateMap<String, Boolean>,
    onNavigate: (Route) -> Unit,
) { DefaultHome(scroll, expandedCats, onNavigate) }

@Composable
actual fun PlatformScreenHost(route: Route, onBack: () -> Unit) { MissingScreen(route, onBack) }
