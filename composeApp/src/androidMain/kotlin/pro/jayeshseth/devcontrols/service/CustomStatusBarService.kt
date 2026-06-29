package pro.jayeshseth.devcontrols.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomStatusBarService : Service() {

    private lateinit var windowManager: WindowManager
    private var triggerView: View? = null
    private var panelView: FrameLayout? = null
    private var isPanelOpen = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createTriggerBar()
        instance = this
        isRunning = true
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createTriggerBar() {
        val windowHeight = 15.dpToPx(this) // 15dp height is perfect to catch swipes without blocking 48dp app buttons
        val windowWidth = WindowManager.LayoutParams.MATCH_PARENT

        val params = WindowManager.LayoutParams(
            windowWidth,
            windowHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

        val composeTriggerView = ComposeView(this).apply {
            setContent {
                val showArea by showTriggerArea.collectAsState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (showArea) Color.White.copy(alpha = 0.25f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (showArea) {
                        // Subtle indicator text/line in the middle when visible
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(3.dp)
                                .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(1.5.dp))
                        )
                    }
                }
            }
        }

        // Set lifecycle owners for the trigger ComposeView
        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.start()
        composeTriggerView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeTriggerView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        composeTriggerView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                collapseSystemStatusBar(this@CustomStatusBarService)
                if (!isPanelOpen) {
                    openNotificationPanel()
                }
            }
            false
        }

        triggerView = composeTriggerView
        windowManager.addView(triggerView, params)
    }

    private fun openNotificationPanel() {
        if (isPanelOpen) return
        isPanelOpen = true

        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarHeight = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 24.dpToPx(this)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        panelView = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        val composeView = ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    NotificationPanelUI(
                        statusBarHeight = statusBarHeight,
                        onDismiss = {
                            collapsePanel()
                        }
                    )
                }
            }
        }

        // Set lifecycle owners for ComposeView to work inside a Service
        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.start()
        
        val viewModelStore = androidx.lifecycle.ViewModelStore()
        val viewModelStoreOwner = object : androidx.lifecycle.ViewModelStoreOwner {
            override val viewModelStore: androidx.lifecycle.ViewModelStore = viewModelStore
        }

        panelView?.let { root ->
            root.setViewTreeLifecycleOwner(lifecycleOwner)
            root.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            root.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        }

        composeView.setViewTreeLifecycleOwner(lifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(viewModelStoreOwner)

        panelView?.addView(composeView)
        windowManager.addView(panelView, params)
    }

    private fun collapsePanel() {
        if (!isPanelOpen) return
        isPanelOpen = false
        panelView?.let {
            windowManager.removeView(it)
            panelView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        triggerView?.let { windowManager.removeView(it) }
        collapsePanel()
        instance = null
        isRunning = false
    }

    companion object {
        var isRunning = false
            private set
        private var instance: CustomStatusBarService? = null
        val showTriggerArea = kotlinx.coroutines.flow.MutableStateFlow(false)

        fun openPanel() {
            instance?.openNotificationPanel()
        }
    }

    private fun DpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

private fun Int.dpToPx(context: Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}

/** The Jetpack Compose UI for the system-wide notification panel. */
@Composable
fun NotificationPanelUI(statusBarHeight: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Drag-down and slide-up offset
    val offsetY = remember { Animatable(-1000f) }

    LaunchedEffect(Unit) {
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    fun dismissPanel() {
        coroutineScope.launch {
            offsetY.animateTo(
                targetValue = -1200f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { dismissPanel() }
    ) {
        Column(
            modifier = Modifier
                .offset { IntOffset(0, offsetY.value.toInt()) }
                .fillMaxWidth()
                .fillMaxHeight() // Full height, edge-to-edge
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0C0D14),
                            Color(0xFF141620)
                        )
                    )
                )
                .clickable(enabled = false) {} // Consume clicks to prevent dismiss
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        collapseSystemStatusBar(context)
                        coroutineScope.launch {
                            val newOffset = (offsetY.value + delta).coerceAtMost(0f)
                            offsetY.snapTo(newOffset)
                        }
                    },
                    onDragStopped = { velocity ->
                        if (offsetY.value < -250f || velocity < -500f) {
                            dismissPanel()
                        } else {
                            coroutineScope.launch {
                                offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            }
                        }
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = with(LocalDensity.current) { statusBarHeight.toDp() })
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
            Spacer(Modifier.height(8.dp))
            
            // 1. Header: Clock, Date, Battery
            HeaderSection()

            Spacer(Modifier.height(24.dp))

            // 2. Quick Settings Tiles
            QuickSettingsGrid()

            Spacer(Modifier.height(24.dp))

            // 3. Brightness Slider
            BrightnessControlSlider()

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Notifications",
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(12.dp))

            // 4. Notifications List
            NotificationListView(onDismiss = { dismissPanel() })
        }
    }
}
}

@Composable
fun HeaderSection() {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
            kotlinx.coroutines.delay(1000)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = currentDate,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📶", color = Color.White, fontSize = 16.sp)
            Text("🔋 82%", color = Color.White, fontSize = 14.sp)
        }
    }
}

@Composable
fun QuickSettingsGrid() {
    val context = LocalContext.current
    var wifiState by remember { mutableStateOf(true) }
    var bluetoothState by remember { mutableStateOf(false) }
    var flashlightState by remember { mutableStateOf(false) }
    var dndState by remember { mutableStateOf(false) }

    // Flashlight Toggle Logic
    fun toggleFlashlight(enable: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        val cameraId = cameraManager?.cameraIdList?.firstOrNull()
        if (cameraId != null) {
            runCatching {
                cameraManager.setTorchMode(cameraId, enable)
                flashlightState = enable
            }
        }
    }

    // DND Toggle Logic
    fun toggleDnd(enable: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager?.isNotificationPolicyAccessGranted == true) {
                val filter = if (enable) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
                notificationManager.setInterruptionFilter(filter)
                dndState = enable
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickSettingTile(
            icon = "📶",
            label = "Wi-Fi",
            isActive = wifiState,
            onClick = { wifiState = !wifiState }
        )
        QuickSettingTile(
            icon = "🛜",
            label = "Bluetooth",
            isActive = bluetoothState,
            onClick = { bluetoothState = !bluetoothState }
        )
        QuickSettingTile(
            icon = "🔦",
            label = "Flashlight",
            isActive = flashlightState,
            onClick = { toggleFlashlight(!flashlightState) }
        )
        QuickSettingTile(
            icon = "🌙",
            label = "DND",
            isActive = dndState,
            onClick = { toggleDnd(!dndState) }
        )
    }
}

@Composable
fun QuickSettingTile(
    icon: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) MaterialTheme.colorScheme.primary
                    else Color.White.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 22.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun BrightnessControlSlider() {
    val context = LocalContext.current
    var brightness by remember {
        mutableFloatStateOf(
            runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
            }.getOrDefault(0.5f)
        )
    }

    fun setSystemBrightness(value: Float) {
        brightness = value
        if (Settings.System.canWrite(context)) {
            val intVal = (value * 255).toInt()
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, intVal)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("☀️", fontSize = 18.sp, color = Color.White)
        Spacer(Modifier.width(12.dp))
        Slider(
            value = brightness,
            onValueChange = { setSystemBrightness(it) },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun NotificationListView(onDismiss: () -> Unit) {
    val notifications by CustomNotificationListener.notifications.collectAsState()

    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No active notifications",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            notifications.forEach { notification ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.08f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notification.title,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = notification.text,
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                        
                        if (notification.isClearable) {
                            IconButton(
                                onClick = { CustomNotificationListener.dismissNotification(notification.key) }
                            ) {
                                Text("✕", color = Color.White, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Custom LifecycleOwner to support ComposeView inside an Android Service. */
class ServiceLifecycleOwner : androidx.lifecycle.LifecycleOwner, androidx.savedstate.SavedStateRegistryOwner {
    private val lifecycleRegistry = androidx.lifecycle.LifecycleRegistry(this)
    private val savedStateRegistryController = androidx.savedstate.SavedStateRegistryController.create(this)

    fun start() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(androidx.lifecycle.Lifecycle.Event.ON_RESUME)
    }

    override val lifecycle: androidx.lifecycle.Lifecycle = lifecycleRegistry
    override val savedStateRegistry: androidx.savedstate.SavedStateRegistry = savedStateRegistryController.savedStateRegistry
}

@SuppressLint("WrongConstant")
private fun collapseSystemStatusBar(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val collapseMethod = statusBarManager.getMethod("collapsePanels")
        collapseMethod.invoke(statusBarService)
    } catch (e: Exception) {
        // Fallback for older versions or restricted systems
        runCatching {
            @Suppress("DEPRECATION")
            context.sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
        }
    }
}
