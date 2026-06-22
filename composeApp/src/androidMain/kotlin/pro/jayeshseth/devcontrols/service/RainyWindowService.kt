package pro.jayeshseth.devcontrols.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.RuntimeShader
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
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
import pro.jayeshseth.devcontrols.data.RainyWindowPrefs

private const val RAIN_OVERLAY_AGSL = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float rainIntensity;
    uniform float glassMisty;
    uniform float dropSpeed;

    float N21(float2 p) {
        p = fract(p * float2(123.34, 345.45));
        p += dot(p, p + 34.345);
        return fract(p.x * p.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution;
        
        // Darkened border vignette/mist
        float2 cc = uv - 0.5;
        float r2 = dot(cc, cc);
        float mistAmount = glassMisty * 0.32 * (0.3 + r2 * 0.75);
        
        half4 finalColor = half4(0.12, 0.15, 0.20, mistAmount); // blue-grey mist base
        
        float t = iTime * dropSpeed * 0.8;
        
        // Layer 1 (Large drops)
        float2 uv1 = uv * float2(5.0, 2.5);
        float2 id1 = floor(uv1);
        float2 grid1 = fract(uv1) - 0.5;
        float n1 = N21(id1);
        float t1 = t + n1 * 6.28;
        float x1 = (n1 - 0.5) * 0.7;
        float y1 = fract(t1);
        float2 p1 = float2(grid1.x - x1, grid1.y - (0.5 - y1));
        float d1 = length(p1 * float2(1.0, 2.0));
        float drop1 = smoothstep(0.12 * rainIntensity, 0.05 * rainIntensity, d1);
        
        // Layer 2 (Medium drops)
        float2 uv2 = uv * float2(8.0, 4.0) + float2(1.5, 0.7);
        float2 id2 = floor(uv2);
        float2 grid2 = fract(uv2) - 0.5;
        float n2 = N21(id2);
        float t2 = t * 1.3 + n2 * 6.28;
        float x2 = (n2 - 0.5) * 0.75;
        float y2 = fract(t2);
        float2 p2 = float2(grid2.x - x2, grid2.y - (0.5 - y2));
        float d2 = length(p2 * float2(1.0, 2.0));
        float drop2 = smoothstep(0.08 * rainIntensity, 0.03 * rainIntensity, d2);
        
        float dropMask = max(drop1, drop2);
        
        if (dropMask > 0.0) {
            // Drops are drawn as semi-transparent highlights
            // They clear the mist (reducing finalColor.a) but add a bright water refraction highlight
            finalColor.a = max(0.05, finalColor.a * (1.0 - dropMask));
            finalColor.rgb = mix(finalColor.rgb, half3(0.85, 0.9, 0.95), dropMask * 0.25);
            finalColor.a += dropMask * 0.15;
        }
        
        return finalColor;
    }
"""

class RainyWindowService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Stop competing overlay services to prevent touch blocking
        val stopPrivacyGlow = Intent(this, PrivacyGlowService::class.java)
        stopService(stopPrivacyGlow)
        val stopRetroShader = Intent(this, RetroShaderService::class.java)
        stopService(stopRetroShader)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1339,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1339, createNotification())
        }

        showOverlay()
    }

    private fun showOverlay() {
        val size = android.graphics.Point()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealSize(size)

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
            setViewTreeLifecycleOwner(this@RainyWindowService)
            setViewTreeViewModelStoreOwner(this@RainyWindowService)
            setViewTreeSavedStateRegistryOwner(this@RainyWindowService)
            fitsSystemWindows = false
            setContent {
                RainyOverlayContent()
            }
        }

        windowManager.addView(composeView, params)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    @Composable
    private fun RainyOverlayContent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val prefs = remember { RainyWindowPrefs.get(applicationContext) }
        val rainIntensity by prefs.rainIntensity.collectAsState(initial = 0.5f)
        val glassMisty by prefs.glassMisty.collectAsState(initial = 0.3f)
        val dropSpeed by prefs.dropSpeed.collectAsState(initial = 1.0f)

        var time by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            val start = System.currentTimeMillis()
            while (true) {
                withFrameNanos {
                    time = (System.currentTimeMillis() - start) / 1000f
                }
            }
        }

        val shader = remember { RuntimeShader(RAIN_OVERLAY_AGSL) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", time)
            shader.setFloatUniform("rainIntensity", rainIntensity)
            shader.setFloatUniform("glassMisty", glassMisty)
            shader.setFloatUniform("dropSpeed", dropSpeed)

            drawRect(brush = ShaderBrush(shader))
        }
    }

    private fun createNotification(): Notification {
        val channelId = "rainy_window_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Rainy Window Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        })
            .setContentTitle("Rainy Window Overlay Active")
            .setContentText("Cozy raindrop shaders running OS-wide.")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let { windowManager.removeView(it) }
    }

    companion object {
        @Volatile
        private var instance: RainyWindowService? = null

        fun isRunning(): Boolean = instance != null
    }
}
