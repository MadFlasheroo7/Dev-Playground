package pro.jayeshseth.devcontrols.service

import android.app.AppOpsManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.location.LocationManager
import android.location.GnssStatus
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import pro.jayeshseth.devcontrols.ui.PrivacyGlowOverlay

class PrivacyGlowService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var appOpsCallback: AppOpsManager.OnOpActiveChangedListener? = null

    private val activeOps = mutableStateListOf<String>()

    private lateinit var cameraManager: CameraManager
    private lateinit var audioManager: AudioManager
    private lateinit var locationManager: LocationManager
    private val unavailableCameras = mutableSetOf<String>()

    private var isCameraActiveByHardware = false
    private var isMicActiveByHardware = false
    private var isLocationActiveByHardware = false
    private val activeOpsByAppOps = mutableSetOf<String>()

    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraAvailable(cameraId: String) {
            super.onCameraAvailable(cameraId)
            unavailableCameras.remove(cameraId)
            updateCameraHardwareState(unavailableCameras.isNotEmpty())
        }

        override fun onCameraUnavailable(cameraId: String) {
            super.onCameraUnavailable(cameraId)
            unavailableCameras.add(cameraId)
            updateCameraHardwareState(unavailableCameras.isNotEmpty())
        }
    }

    private val audioRecordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            super.onRecordingConfigChanged(configs)
            updateMicHardwareState(configs.isNotEmpty())
        }
    }

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onStarted() {
            super.onStarted()
            updateLocationHardwareState(true)
        }

        override fun onStopped() {
            super.onStopped()
            updateLocationHardwareState(false)
        }
    }

    private val testReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val op = intent.getStringExtra("op") ?: return
            val active = intent.getBooleanExtra("active", false)
            if (op == AppOpsManager.OPSTR_CAMERA) {
                updateCameraHardwareState(active)
            } else if (op == AppOpsManager.OPSTR_RECORD_AUDIO) {
                updateMicHardwareState(active)
            } else if (op == AppOpsManager.OPSTR_FINE_LOCATION || op == AppOpsManager.OPSTR_COARSE_LOCATION) {
                updateLocationHardwareState(active)
            } else {
                updateAppOpsState(op, active)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // Stop competing overlay service to prevent cumulative opacity from blocking clicks
        val stopRetroShader = Intent(this, RetroShaderService::class.java)
        stopService(stopRetroShader)
        val stopRainyWindow = Intent(this, RainyWindowService::class.java)
        stopService(stopRainyWindow)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        try {
            cameraManager.registerAvailabilityCallback(cameraCallback, android.os.Handler(android.os.Looper.getMainLooper()))
            audioManager.registerAudioRecordingCallback(audioRecordingCallback, android.os.Handler(android.os.Looper.getMainLooper()))
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                locationManager.registerGnssStatusCallback(gnssStatusCallback, android.os.Handler(android.os.Looper.getMainLooper()))
            }
        } catch (e: Exception) {
            android.util.Log.e("PrivacyGlow", "Error registering hardware callbacks", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1337,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1337, createNotification())
        }
        startWatchingOps()
        refreshActiveOps()

        val filter = IntentFilter("pro.jayeshseth.devcontrols.TEST_GLOW")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(testReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(testReceiver, filter)
        }
    }

    private fun updateCameraHardwareState(inUse: Boolean) {
        isCameraActiveByHardware = inUse
        refreshActiveOps()
    }

    private fun updateMicHardwareState(inUse: Boolean) {
        isMicActiveByHardware = inUse
        refreshActiveOps()
    }

    private fun updateLocationHardwareState(inUse: Boolean) {
        isLocationActiveByHardware = inUse
        refreshActiveOps()
    }

    private fun updateAppOpsState(op: String, active: Boolean) {
        if (active) {
            activeOpsByAppOps.add(op)
        } else {
            activeOpsByAppOps.remove(op)
        }
        refreshActiveOps()
    }

    private fun refreshActiveOps() {
        val cameraActive = isCameraActiveByAppOps || isCameraActiveByHardware
        val micActive = isMicActiveByAppOps || isMicActiveByHardware
        val locationActive = isLocationActiveByAppOps || isLocationActiveByHardware
        
        val targetOps = mutableListOf<String>()
        if (cameraActive) targetOps.add(AppOpsManager.OPSTR_CAMERA)
        if (micActive) targetOps.add(AppOpsManager.OPSTR_RECORD_AUDIO)
        if (locationActive) targetOps.add(AppOpsManager.OPSTR_FINE_LOCATION)
        
        val toRemove = activeOps.filter { it !in targetOps }
        activeOps.removeAll(toRemove)
        
        for (op in targetOps) {
            if (op !in activeOps) activeOps.add(op)
        }

        if (activeOps.isNotEmpty()) {
            showOverlay()
        } else {
            hideOverlay()
        }
    }

    private val isCameraActiveByAppOps: Boolean
        get() = AppOpsManager.OPSTR_CAMERA in activeOpsByAppOps

    private val isMicActiveByAppOps: Boolean
        get() = AppOpsManager.OPSTR_RECORD_AUDIO in activeOpsByAppOps

    private val isLocationActiveByAppOps: Boolean
        get() = AppOpsManager.OPSTR_FINE_LOCATION in activeOpsByAppOps || 
                AppOpsManager.OPSTR_COARSE_LOCATION in activeOpsByAppOps

    private fun startWatchingOps() {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val ops = arrayOf(
            AppOpsManager.OPSTR_CAMERA,
            AppOpsManager.OPSTR_RECORD_AUDIO,
            AppOpsManager.OPSTR_FINE_LOCATION,
            AppOpsManager.OPSTR_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // First stop any existing to force-refresh
            appOpsCallback?.let { appOps.stopWatchingActive(it) }

            val callback = AppOpsManager.OnOpActiveChangedListener { op, _, _, active ->
                updateAppOpsState(op, active)
            }
            appOpsCallback = callback
            try {
                appOps.startWatchingActive(ops, mainExecutor, callback)
            } catch (e: Exception) {
                android.util.Log.e("PrivacyGlow", "Error starting AppOps watch", e)
            }
        }
    }

    private fun showOverlay() {
        if (composeView != null) return
        val size = android.graphics.Point()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            size.x = bounds.width()
            size.y = bounds.height()
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(size)
        }

        val params = WindowManager.LayoutParams(
            size.x,
            size.y,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            alpha = 0.65f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PrivacyGlowService)
            setViewTreeViewModelStoreOwner(this@PrivacyGlowService)
            setViewTreeSavedStateRegistryOwner(this@PrivacyGlowService)
            fitsSystemWindows = false
            setContent {
                PrivacyGlowOverlay(activeOps = activeOps)
            }
        }
        windowManager.addView(composeView, params)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun hideOverlay() {
        composeView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                android.util.Log.e("PrivacyGlow", "Error removing overlay", e)
            }
            composeView = null
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "privacy_glow"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Privacy Glow Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        })
            .setContentTitle("Privacy Glow Active")
            .setContentText("Monitoring camera, mic, and location usage.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(testReceiver)
        try {
            cameraManager.unregisterAvailabilityCallback(cameraCallback)
            audioManager.unregisterAudioRecordingCallback(audioRecordingCallback)
            locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
        } catch (e: Exception) {
            android.util.Log.e("PrivacyGlow", "Error unregistering hardware callbacks", e)
        }
        instance = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let { windowManager.removeView(it) }
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && appOpsCallback != null) {
            appOps.stopWatchingActive(appOpsCallback!!)
        }
    }

    companion object {
        @Volatile
        private var instance: PrivacyGlowService? = null

        fun isRunning(): Boolean = instance != null
    }
}
