package pro.jayeshseth.devcontrols.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.FocusCallsPrefs
import pro.jayeshseth.devcontrols.util.ForegroundAppDetector

/** Focus Calls panel section: manage the allow-list of apps whose incoming calls bypass kiosk/DND blocking. */
@Composable
fun FocusCallsSection(onManageAllowList: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { FocusCallsPrefs.get(context) }
    val scope = rememberCoroutineScope()

    val enabled by prefs.focusCallsEnabled.collectAsState(initial = false)
    val autoAnswer by prefs.autoAnswerEnabled.collectAsState(initial = false)
    val useRelaunch by prefs.archUseRelaunch.collectAsState(initial = true)
    val useDialer by prefs.archUseDialer.collectAsState(initial = false)
    val allowed by prefs.allowedPackages.collectAsState(initial = emptySet())

    val hasUsage = ForegroundAppDetector.hasUsageStatsPermission(context)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Focus Calls", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "When you're in an allow-listed app and a call comes in, this app keeps you in that app instead of throwing you into the phone UI.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enabled", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = enabled,
                    onCheckedChange = { scope.launch { prefs.setFocusCallsEnabled(it) } }
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Text("Architecture", style = MaterialTheme.typography.titleSmall)
            ArchRow(
                label = "Re-launch your app after answer",
                description = "Keep system dialer. Brief in-call UI flash, then snap back. No default-app changes.",
                selected = useRelaunch,
                onSelect = { scope.launch { prefs.selectRelaunchArch() } }
            )
            ArchRow(
                label = "Become default phone app",
                description = "Replaces the system dialer. No full-screen call UI, only heads-up notifications. Grant the role via:\n adb shell cmd role add-role-holder android.app.role.DIALER pro.jayeshseth.devcontrols",
                selected = useDialer,
                onSelect = { scope.launch { prefs.selectDialerArch() } }
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-answer", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Pick up automatically when you're in an allow-listed app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoAnswer,
                    onCheckedChange = { scope.launch { prefs.setAutoAnswer(it) } }
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onManageAllowList,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Manage allow-list (${allowed.size} apps)") }

            if (!hasUsage) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "PACKAGE_USAGE_STATS not granted. Run:\n adb shell appops set pro.jayeshseth.devcontrols GET_USAGE_STATS allow",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ArchRow(
    label: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = if (description.contains("adb")) FontFamily.Monospace else null
            )
        }
    }
}
