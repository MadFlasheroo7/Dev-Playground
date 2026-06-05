package pro.jayeshseth.devcontrols.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import pro.jayeshseth.devcontrols.data.AdbEndpointDiscoverer
import pro.jayeshseth.devcontrols.data.DevSettingsRepository
import pro.jayeshseth.devcontrols.data.FocusCallsPrefs
import pro.jayeshseth.devcontrols.data.KioskPrefs
import pro.jayeshseth.devcontrols.util.WifiUtils
import kotlinx.coroutines.flow.first

class DevControlsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = DevSettingsRepository.get(context)
        val focus = FocusCallsPrefs.get(context)
        val kiosk = KioskPrefs.get(context)
        val discoverer = AdbEndpointDiscoverer.get(context)

        val (kioskPkg, kioskLabel) = kiosk.targetSnapshot()
        val snapshot = WidgetSnapshot(
            devOpts = repo.currentDeveloperOptions(),
            usb = repo.currentUsbDebugging(),
            wireless = repo.currentWirelessDebugging(),
            never = repo.currentNeverTimeout(),
            kioskTargetLabel = kioskLabel,
            kioskTargetPicked = kioskPkg != null,
            kioskActive = kiosk.isKioskActiveOnce(),
            focusCalls = focus.focusCallsEnabled.first(),
            wifiIp = WifiUtils.currentWifiIpv4(context),
            endpoint = discoverer.lastResolved()?.toString()
        )

        provideContent { WidgetContent(snapshot) }
    }
}

private data class WidgetSnapshot(
    val devOpts: Boolean,
    val usb: Boolean,
    val wireless: Boolean,
    val never: Boolean,
    val kioskTargetLabel: String?,
    val kioskTargetPicked: Boolean,
    val kioskActive: Boolean,
    val focusCalls: Boolean,
    val wifiIp: String?,
    val endpoint: String?,
)

private fun WidgetSnapshot.endpointText(): String = when {
    endpoint != null -> endpoint
    !wireless -> "Enable wireless debugging"
    wifiIp != null -> "$wifiIp:… discovering"
    else -> "No Wi-Fi"
}

@Composable
private fun WidgetContent(s: WidgetSnapshot) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .padding(12.dp)
    ) {
        Text(
            text = "Dev Controls",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(8.dp))
        EndpointRow(s.endpointText())
        Spacer(GlanceModifier.height(8.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            ToggleCell("Dev", s.devOpts, ToggleKeys.DEV_OPTIONS, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            ToggleCell("USB", s.usb, ToggleKeys.USB_DEBUGGING, GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            ToggleCell("Wi-Fi ADB", s.wireless, ToggleKeys.WIRELESS_DEBUGGING, GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(6.dp))
            ToggleCell("No Timeout", s.never, ToggleKeys.NEVER_TIMEOUT, GlanceModifier.defaultWeight())
        }
        Spacer(GlanceModifier.height(6.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            ActionCell(
                label = if (s.kioskActive) "Kiosk ON" else "Kiosk",
                stateText = when {
                    s.kioskActive -> "Tap to stop"
                    s.kioskTargetPicked -> s.kioskTargetLabel ?: "Tap to start"
                    else -> "Pick target"
                },
                isPrimed = s.kioskActive || s.kioskTargetPicked,
                toggleKey = ToggleKeys.KIOSK_MODE,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(GlanceModifier.width(6.dp))
            ToggleCell("Focus Calls", s.focusCalls, ToggleKeys.FOCUS_CALLS, GlanceModifier.defaultWeight())
        }
    }
}

@Composable
private fun EndpointRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A))
            .cornerRadius(8.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clickable(
                actionRunCallback<CopyEndpointAction>(
                    actionParametersOf(EndpointKeys.KEY_ENDPOINT to text)
                )
            )
    ) {
        Text(
            text = "ADB",
            style = TextStyle(
                color = ColorProvider(Color(0xFF9AA0A6)),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = text,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 13.sp
            )
        )
    }
}

@Composable
private fun ActionCell(
    label: String,
    stateText: String,
    isPrimed: Boolean,
    toggleKey: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    val bg = if (isPrimed) Color(0xFF3949AB) else Color(0xFF2A2A2A)
    val labelColor = if (isPrimed) Color.White else Color(0xFFCFCFCF)
    val stateColor = if (isPrimed) Color(0xFFBFC9FF) else Color(0xFF9AA0A6)
    Box(
        modifier = modifier
            .height(58.dp)
            .background(bg)
            .cornerRadius(10.dp)
            .clickable(
                actionRunCallback<ToggleActionCallback>(
                    actionParametersOf(ToggleKeys.KEY_TOGGLE to toggleKey)
                )
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(labelColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = stateText,
                style = TextStyle(
                    color = ColorProvider(stateColor),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ToggleCell(
    label: String,
    isOn: Boolean,
    toggleKey: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    val bg = if (isOn) Color(0xFF1B6F3C) else Color(0xFF2A2A2A)
    val labelColor = if (isOn) Color.White else Color(0xFFCFCFCF)
    val stateText = if (isOn) "ON" else "OFF"
    val stateColor = if (isOn) Color(0xFFB7F7C2) else Color(0xFF9AA0A6)
    Box(
        modifier = modifier
            .height(58.dp)
            .background(bg)
            .cornerRadius(10.dp)
            .clickable(
                actionRunCallback<ToggleActionCallback>(
                    actionParametersOf(ToggleKeys.KEY_TOGGLE to toggleKey)
                )
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = label,
                style = TextStyle(
                    color = ColorProvider(labelColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
            Text(
                text = stateText,
                style = TextStyle(
                    color = ColorProvider(stateColor),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
