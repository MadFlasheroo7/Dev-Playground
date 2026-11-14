package pro.jayeshseth.devcontrols.ui.hyprdeck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class HyprDeckController(
    private val scope: CoroutineScope
) {
    var isAvailable by mutableStateOf(HyprIpc.isAvailable())
        private set

    var instanceSignature by mutableStateOf(HyprIpc.getInstanceSignature() ?: "Not detected")
        private set

    var versionInfo by mutableStateOf("")
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    var statusMessage by mutableStateOf<String?>(null)
        private set

    var searchQuery by mutableStateOf("")
    var selectedWorkspaceFilter by mutableStateOf<Int?>(null)
    var selectedMonitorFilter by mutableStateOf<String?>(null)
    var selectedClient by mutableStateOf<HyprClient?>(null)

    val workspaces = mutableStateListOf<HyprWorkspace>()
    val clients = mutableStateListOf<HyprClient>()
    val monitors = mutableStateListOf<HyprMonitor>()
    var activeWindow by mutableStateOf<HyprActiveWindow?>(null)
        private set

    val events = mutableStateListOf<HyprEvent>()

    private var eventJob: Job? = null
    private var refreshDebounceJob: Job? = null

    init {
        start()
    }

    fun start() {
        isAvailable = HyprIpc.isAvailable()
        instanceSignature = HyprIpc.getInstanceSignature() ?: "Not detected"
        if (isAvailable) {
            refreshAll()
            startEventListening()
        }
    }

    fun refreshAll() {
        scope.launch {
            isRefreshing = true
            try {
                // Fetch monitors
                HyprIpc.getMonitors().onSuccess { mons ->
                    monitors.clear()
                    monitors.addAll(mons)
                }

                // Fetch workspaces
                HyprIpc.getWorkspaces().onSuccess { ws ->
                    workspaces.clear()
                    workspaces.addAll(ws.sortedBy { it.id })
                }

                // Fetch clients
                HyprIpc.getClients().onSuccess { cls ->
                    clients.clear()
                    clients.addAll(cls.filter { it.mapped && !it.hidden })
                    // Re-sync selected client
                    selectedClient?.let { current ->
                        selectedClient = cls.find { it.address == current.address }
                    }
                }

                // Fetch active window
                HyprIpc.getActiveWindow().onSuccess { win ->
                    activeWindow = win
                }

                // Fetch version if not loaded
                if (versionInfo.isBlank()) {
                    HyprIpc.getVersion().onSuccess { ver ->
                        versionInfo = ver.lineSequence().firstOrNull() ?: ver
                    }
                }
            } catch (e: Exception) {
                statusMessage = "Refresh error: ${e.message}"
            } finally {
                isRefreshing = false
            }
        }
    }

    private fun startEventListening() {
        eventJob?.cancel()
        eventJob = HyprIpc.eventFlow()
            .onEach { event ->
                withContext(Dispatchers.Main) {
                    if (events.size > 150) {
                        events.removeRange(0, 30)
                    }
                    events.add(event)

                    // Trigger smart auto-refresh when window/workspace topology changes
                    when (event.name) {
                        "openwindow", "closewindow", "movewindow", "workspace",
                        "focusedmon", "activewindow", "activewindowv2",
                        "fullscreen", "changefloatingmode", "pin" -> {
                            scheduleDebouncedRefresh()
                        }
                    }
                }
            }
            .catch {
                // Ignore socket reconnect noise
            }
            .launchIn(scope)
    }

    private fun scheduleDebouncedRefresh() {
        refreshDebounceJob?.cancel()
        refreshDebounceJob = scope.launch {
            delay(120)
            refreshAll()
        }
    }

    fun focusWindow(address: String) {
        scope.launch {
            HyprIpc.focusWindow(address).onSuccess {
                statusMessage = "Focused $address"
                refreshAll()
            }.onFailure {
                statusMessage = "Focus failed: ${it.message}"
            }
        }
    }

    fun moveWindowToWorkspace(address: String, workspaceId: Int) {
        scope.launch {
            HyprIpc.moveWindowToWorkspace(address, workspaceId).onSuccess {
                statusMessage = "Moved to WS $workspaceId"
                refreshAll()
            }.onFailure {
                statusMessage = "Move failed: ${it.message}"
            }
        }
    }

    fun toggleFloating(address: String) {
        scope.launch {
            HyprIpc.toggleFloating(address).onSuccess {
                statusMessage = "Toggled floating"
                refreshAll()
            }.onFailure {
                statusMessage = "Float toggle failed: ${it.message}"
            }
        }
    }

    fun toggleFullscreen(address: String) {
        scope.launch {
            HyprIpc.toggleFullscreen(address).onSuccess {
                statusMessage = "Toggled fullscreen"
                refreshAll()
            }.onFailure {
                statusMessage = "Fullscreen toggle failed: ${it.message}"
            }
        }
    }

    fun pinWindow(address: String) {
        scope.launch {
            HyprIpc.pinWindow(address).onSuccess {
                statusMessage = "Toggled pin"
                refreshAll()
            }.onFailure {
                statusMessage = "Pin failed: ${it.message}"
            }
        }
    }

    fun closeWindow(address: String) {
        scope.launch {
            HyprIpc.closeWindow(address).onSuccess {
                statusMessage = "Closed window"
                refreshAll()
            }.onFailure {
                statusMessage = "Close failed: ${it.message}"
            }
        }
    }

    fun switchWorkspace(workspaceId: Int) {
        scope.launch {
            HyprIpc.switchWorkspace(workspaceId).onSuccess {
                statusMessage = "Switched to WS $workspaceId"
                refreshAll()
            }.onFailure {
                statusMessage = "Workspace switch failed: ${it.message}"
            }
        }
    }

    fun focusMonitor(monitorName: String) {
        scope.launch {
            HyprIpc.focusMonitor(monitorName).onSuccess {
                statusMessage = "Focused monitor $monitorName"
                refreshAll()
            }.onFailure {
                statusMessage = "Focus monitor failed: ${it.message}"
            }
        }
    }

    fun clearEvents() {
        events.clear()
    }

    val filteredClients: List<HyprClient>
        get() {
            var list = clients.toList()
            selectedWorkspaceFilter?.let { wsId ->
                list = list.filter { it.workspace.id == wsId }
            }
            selectedMonitorFilter?.let { monName ->
                val mon = monitors.find { it.name == monName }
                if (mon != null) {
                    list = list.filter { it.monitor == mon.id }
                }
            }
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.title.lowercase().contains(q) ||
                        it.clientClass.lowercase().contains(q) ||
                        it.initialClass.lowercase().contains(q) ||
                        it.pid.toString().contains(q) ||
                        it.address.lowercase().contains(q)
                }
            }
            return list
        }
}

@Composable
fun rememberHyprDeckController(): HyprDeckController {
    val scope = rememberCoroutineScope()
    return remember { HyprDeckController(scope) }
}
