package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pro.jayeshseth.devcontrols.nav.Route
import pro.jayeshseth.devcontrols.nav.commonRoutes

/** Default home for non-Android platforms: title + the portable screens available so far. */
@Composable
fun DefaultHome(onNavigate: (Route) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("DevControls", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Compose Multiplatform build. ${commonRoutes.size} portable screens available; " +
                "Android-only features run on the Android app.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        commonRoutes.sortedBy { it.name }.forEach { route ->
            Button(onClick = { onNavigate(route) }, modifier = Modifier) {
                Text(route.name)
            }
        }
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
