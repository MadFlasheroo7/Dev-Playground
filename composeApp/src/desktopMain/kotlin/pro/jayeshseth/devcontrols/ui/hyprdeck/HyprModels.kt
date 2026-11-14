package pro.jayeshseth.devcontrols.ui.hyprdeck

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HyprWorkspaceRef(
    val id: Int = 0,
    val name: String = "",
)

@Serializable
data class HyprWorkspace(
    val id: Int = 0,
    val name: String = "",
    val monitor: String = "",
    val monitorID: Int = 0,
    val windows: Int = 0,
    val hasfullscreen: Boolean = false,
    val lastwindow: String = "",
    val lastwindowtitle: String = "",
)

@Serializable
data class HyprClient(
    val address: String = "",
    val mapped: Boolean = true,
    val hidden: Boolean = false,
    val at: List<Int> = emptyList(),
    val size: List<Int> = emptyList(),
    val workspace: HyprWorkspaceRef = HyprWorkspaceRef(),
    val floating: Boolean = false,
    val monitor: Int = 0,
    @SerialName("class") val clientClass: String = "",
    val title: String = "",
    val initialTitle: String = "",
    val initialClass: String = "",
    val pid: Long = 0,
    val xwayland: Boolean = false,
    val pinned: Boolean = false,
    val fullscreen: Int = 0,
) {
    val x: Int get() = at.getOrElse(0) { 0 }
    val y: Int get() = at.getOrElse(1) { 0 }
    val width: Int get() = size.getOrElse(0) { 0 }
    val height: Int get() = size.getOrElse(1) { 0 }
    val isFullscreen: Boolean get() = fullscreen > 0

    val displayName: String
        get() = when {
            title.isNotBlank() -> title
            clientClass.isNotBlank() -> clientClass
            initialClass.isNotBlank() -> initialClass
            else -> "Window $address"
        }

    val displayClass: String
        get() = when {
            clientClass.isNotBlank() -> clientClass
            initialClass.isNotBlank() -> initialClass
            else -> "Unknown"
        }
}

@Serializable
data class HyprMonitor(
    val id: Int = 0,
    val name: String = "",
    val description: String = "",
    val make: String = "",
    val model: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val refreshRate: Double = 0.0,
    val x: Int = 0,
    val y: Int = 0,
    val activeWorkspace: HyprWorkspaceRef = HyprWorkspaceRef(),
    val focused: Boolean = false,
    val scale: Double = 1.0,
)

@Serializable
data class HyprActiveWindow(
    val address: String = "",
    val title: String = "",
    @SerialName("class") val clientClass: String = "",
    val pid: Long = 0,
    val workspace: HyprWorkspaceRef = HyprWorkspaceRef(),
)

data class HyprEvent(
    val name: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val formattedTime: String
        get() {
            val d = java.time.Instant.ofEpochMilli(timestamp)
                .atZone(java.time.ZoneId.systemDefault())
            return "%02d:%02d:%02d.%03d".format(d.hour, d.minute, d.second, d.nano / 1_000_000)
        }
}
