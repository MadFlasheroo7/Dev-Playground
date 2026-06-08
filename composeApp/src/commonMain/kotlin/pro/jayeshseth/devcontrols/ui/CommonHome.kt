package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.nav.commonRoutes

/**
 * Home for non-Android platforms: the same categorized [CategoryMenu] as Android, filtered to the
 * routes that actually run here (Android-only features are hidden rather than dead-ending).
 */
@Composable
fun DefaultHome(scroll: ScrollState, expandedCats: SnapshotStateMap<String, Boolean>, onNavigate: (Route) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
        Text("DevControls", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "Compose Multiplatform build — ${commonRoutes.size} cross-platform screens. " +
                "Android-only features (camera, sensors, ADB, shaders…) appear only on the Android app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        CategoryMenu(expandedCats = expandedCats, onNavigate = onNavigate, isEnabled = { it in commonRoutes })
    }
}

/** Shown when a route isn't available on the current platform (Android-only feature). */
@Composable
fun MissingScreen(route: Route, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "\"${route.name}\" isn't available on this platform yet.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "This feature relies on Android-only capabilities.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
    }
}
