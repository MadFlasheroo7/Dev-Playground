package pro.jayeshseth.devcontrols.ui

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Immersive mode — hide/show system bars, sticky behavior, and FLAG_SECURE (blocks screenshots). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmersiveLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    var hideStatus by remember { mutableStateOf(false) }
    var hideNav by remember { mutableStateOf(false) }
    var sticky by remember { mutableStateOf(true) }
    var secure by remember { mutableStateOf(false) }

    fun apply() {
        val window = activity?.window ?: return
        val controller = WindowInsetsControllerCompat(window, view)
        controller.systemBarsBehavior = if (sticky)
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        else WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        if (hideStatus) controller.hide(WindowInsetsCompat.Type.statusBars()) else controller.show(WindowInsetsCompat.Type.statusBars())
        if (hideNav) controller.hide(WindowInsetsCompat.Type.navigationBars()) else controller.show(WindowInsetsCompat.Type.navigationBars())
    }

    DisposableEffect(hideStatus, hideNav, sticky) { apply(); onDispose { } }
    DisposableEffect(secure) {
        val window = activity?.window
        if (secure) window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { }
    }
    // restore bars on leave
    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let {
                WindowInsetsControllerCompat(it, view).show(WindowInsetsCompat.Type.systemBars())
                it.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Immersive Mode") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            Row3("Hide status bar", hideStatus) { hideStatus = it }
            Row3("Hide nav bar", hideNav) { hideNav = it }
            Row3("Sticky (swipe reveals)", sticky) { sticky = it }
            Row3("FLAG_SECURE (block screenshots)", secure) { secure = it }
            Spacer(Modifier.height(12.dp))
            Text("Try a screenshot with FLAG_SECURE on — it'll be blank. Swipe from an edge to reveal hidden bars in sticky mode.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Row3(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().height(52.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
