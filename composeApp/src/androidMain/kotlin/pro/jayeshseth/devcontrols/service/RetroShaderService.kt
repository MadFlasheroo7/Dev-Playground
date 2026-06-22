package pro.jayeshseth.devcontrols.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.ColorSpace
import android.graphics.PixelFormat
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
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
import androidx.compose.runtime.mutableStateOf
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
import pro.jayeshseth.devcontrols.data.RetroShaderPrefs

private const val RETRO_OVERLAY_AGSL = """
    uniform shader content;
    uniform float2 iResolution;
    uniform float iTime;
    uniform float scanlineIntensity;
    uniform float noiseIntensity;
    uniform float curvature;
    uniform float tintMode; // 0=none, 1=green, 2=amber, 3=gameboy
    uniform float useWarp;  // 0.0 = translucent overlay, 1.0 = warp content

    float rand(float2 co) {
        return fract(sin(dot(co, float2(12.9898, 78.233))) * 43758.5453);
    }

    float sdRoundedBox(float2 p, float2 b, float r) {
        float2 q = abs(p) - b + r;
        return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
    }

    half4 main(float2 fragCoord) {
        float2 p = fragCoord - iResolution * 0.5;
        float2 halfSize = iResolution * 0.5;
        
        // 1. CRT Bezel / Rounded Screen Corners
        float bezelRadius = 30.0 + curvature * 90.0;
        float inset = 1.0 + curvature * 25.0; 
        float d = sdRoundedBox(p, halfSize - inset, bezelRadius);
        
        if (d > 0.0) {
            return half4(0.0, 0.0, 0.0, 1.0); // Outside active screen: solid black
        }

        float2 uv = fragCoord / iResolution;
        half4 finalColor = half4(0.0);

        if (useWarp > 0.5) {
            // Apply CRT Curvature/Distortion to the captured screen!
            float2 cc = uv - 0.5;
            float r2 = dot(cc, cc);
            float2 warpedUv = 0.5 + cc * (1.0 + curvature * r2 * 1.5);
            
            if (warpedUv.x >= 0.0 && warpedUv.x <= 1.0 && warpedUv.y >= 0.0 && warpedUv.y <= 1.0) {
                // Sample the warped screen pixels!
                finalColor = content.eval(warpedUv * iResolution);
            } else {
                finalColor = half4(0.0, 0.0, 0.0, 1.0);
            }
        }

        // 2. Bezel Inner Shadow (Vignette)
        float edgeShadow = smoothstep(-35.0 - curvature * 30.0, 0.0, d);
        if (useWarp > 0.5) {
            finalColor.rgb = mix(finalColor.rgb, half3(0.0), edgeShadow * 0.75);
        } else {
            finalColor = mix(finalColor, half4(0.0, 0.0, 0.0, 1.0), edgeShadow * 0.75);
        }

        // 3. Scanline grid overlay
        float scanline = sin(fragCoord.y / 5.0 + iTime * 3.5);
        float sFactor = (scanline * 0.5 + 0.5);
        float lineAlpha = scanlineIntensity * 0.55 * sFactor;
        
        if (useWarp > 0.5) {
            finalColor.rgb = mix(finalColor.rgb, half3(0.0), lineAlpha);
        } else {
            finalColor = mix(finalColor, half4(0.0, 0.0, 0.0, 1.0), lineAlpha);
        }

        // 4. VHS Jitter / Static Noise
        float2 blockCoord = floor(fragCoord / 4.0);
        float noise = rand(blockCoord + iTime * 2.0);
        float noiseAlpha = noiseIntensity * 0.22;
        if (noiseAlpha > 0.0) {
            half3 noiseColor = (noise > 0.5) ? half3(1.0) : half3(0.0);
            if (useWarp > 0.5) {
                finalColor.rgb = mix(finalColor.rgb, noiseColor, noiseAlpha);
            } else {
                finalColor = mix(finalColor, half4(noiseColor, 1.0), noiseAlpha);
            }
        }

        // 5. Color Tinting (Green/Amber/GameBoy)
        if (tintMode > 0.5) {
            half3 tintColor = half3(0.0);
            float tintAlpha = 0.0;
            if (tintMode < 1.5) {
                tintColor = half3(0.0, 1.0, 0.0); // green
                tintAlpha = 0.15;
            } else if (tintMode < 2.5) {
                tintColor = half3(1.0, 0.65, 0.0); // amber
                tintAlpha = 0.15;
            } else {
                tintColor = half3(0.55, 0.68, 0.21); // gameboy
                tintAlpha = 0.32;
                
                float2 grid = fract(fragCoord / 12.0);
                if (grid.x < 0.15 || grid.y < 0.15) {
                    if (useWarp > 0.5) {
                        finalColor.rgb = mix(finalColor.rgb, half3(0.18, 0.22, 0.08), 0.65);
                    } else {
                        finalColor = mix(finalColor, half4(0.18, 0.22, 0.08, 1.0), 0.65);
                    }
                }
            }
            
            if (useWarp > 0.5) {
                float luma = dot(finalColor.rgb, half3(0.299, 0.587, 0.114));
                finalColor.rgb = mix(half3(luma), tintColor * luma * 1.5, tintAlpha * 2.0);
            } else {
                finalColor = mix(finalColor, half4(tintColor, 1.0), tintAlpha);
            }
        }

        if (useWarp > 0.5) {
            finalColor.a = 1.0;
        }

        return finalColor;
    }
"""

class RetroShaderService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private fun updateOverlayAlpha(newAlpha: Float) {
        val view = composeView ?: return
        val params = windowParams ?: return
        params.alpha = newAlpha
        try {
            windowManager.updateViewLayout(view, params)
        } catch (e: Exception) {
            android.util.Log.e("RetroShaderService", "Failed to update overlay alpha", e)
        }
    }

    // Screen Capture (MediaProjection) objects
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    @Volatile
    private var screenBitmap: Bitmap? = null
    @Volatile
    private var bitmapShader: BitmapShader? = null
    private var isWarpEnabled by mutableStateOf(false)

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("RetroShaderService", "onCreate called")
        instance = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Stop competing overlay service to prevent cumulative opacity from blocking clicks
        val stopPrivacyGlow = Intent(this, PrivacyGlowService::class.java)
        stopService(stopPrivacyGlow)
        val stopRainyWindow = Intent(this, RainyWindowService::class.java)
        stopService(stopRainyWindow)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1338,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1338, createNotification())
        }

        showOverlay()
    }

    private fun showOverlay() {
        android.util.Log.d("RetroShaderService", "showOverlay called")
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
        windowParams = params

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@RetroShaderService)
            setViewTreeViewModelStoreOwner(this@RetroShaderService)
            setViewTreeSavedStateRegistryOwner(this@RetroShaderService)
            fitsSystemWindows = false
            setContent {
                RetroOverlayContent()
            }
        }

        windowManager.addView(composeView, params)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    @Composable
    private fun RetroOverlayContent() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val prefs = remember { RetroShaderPrefs.get(applicationContext) }
        val curvature by prefs.curvature.collectAsState(initial = 0.12f)
        val scanlineIntensity by prefs.scanlineIntensity.collectAsState(initial = 0.6f)
        val noiseIntensity by prefs.noiseIntensity.collectAsState(initial = 0.3f)
        val phosphorMode by prefs.phosphorMode.collectAsState(initial = 0f)
        val warpPref by prefs.warpEnabled.collectAsState(initial = false)

        var time by remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) {
            val start = System.currentTimeMillis()
            while (true) {
                withFrameNanos {
                    time = (System.currentTimeMillis() - start) / 1000f
                }
            }
        }

        val shader = remember { RuntimeShader(RETRO_OVERLAY_AGSL) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", time)
            shader.setFloatUniform("curvature", curvature)
            shader.setFloatUniform("scanlineIntensity", scanlineIntensity)
            shader.setFloatUniform("noiseIntensity", noiseIntensity)
            shader.setFloatUniform("tintMode", phosphorMode)
            
            val activeWarp = warpPref && isWarpEnabled
            shader.setFloatUniform("useWarp", if (activeWarp) 1.0f else 0.0f)

            val currentShader = synchronized(this@RetroShaderService) { bitmapShader }
            if (activeWarp && currentShader != null) {
                shader.setInputShader("content", currentShader)
            } else {
                val dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                shader.setInputShader("content", BitmapShader(dummy, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            }

            drawRect(brush = ShaderBrush(shader))
        }
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        android.util.Log.d("RetroShaderService", "startScreenCapture called: resultCode=$resultCode")
        stopScreenCapture()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1338,
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or
                            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            }
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val proj = projectionManager.getMediaProjection(resultCode, data)
            mediaProjection = proj
            proj?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopScreenCapture()
                }
            }, android.os.Handler(android.os.Looper.getMainLooper()))
            
            val size = android.graphics.Point()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealSize(size)
            val width = size.x / 2
            val height = size.y / 2
            val dpi = resources.displayMetrics.densityDpi

            imageReader = ImageReader.newInstance(
                width,
                height,
                PixelFormat.RGBA_8888,
                2,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
            ).apply {
                var loggedFirstFrame = false
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        if (!loggedFirstFrame) {
                            android.util.Log.d("RetroShaderService", "First screen capture frame acquired successfully!")
                            loggedFirstFrame = true
                        }
                        try {
                            val hardwareBuffer = image.hardwareBuffer
                            if (hardwareBuffer != null) {
                                val newBitmap = Bitmap.wrapHardwareBuffer(
                                    hardwareBuffer,
                                    ColorSpace.get(ColorSpace.Named.SRGB)
                                )
                                 if (newBitmap != null) {
                                    val matrix = android.graphics.Matrix()
                                    matrix.postScale(2.0f, 2.0f)
                                    val newShader = BitmapShader(newBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                                    newShader.setLocalMatrix(matrix)
                                    synchronized(this@RetroShaderService) {
                                        screenBitmap = newBitmap
                                        bitmapShader = newShader
                                    }
                                }
                                hardwareBuffer.close()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RetroShaderService", "Error reading hardware buffer", e)
                        } finally {
                            image.close()
                        }
                    }
                }, android.os.Handler(android.os.Looper.getMainLooper()))
            }

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "retro_screen_capture",
                width,
                height,
                dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            isWarpEnabled = true

            // Bypass untrusted touch blocking using root to support alpha = 1.0f on rooted devices
            Thread {
                var hasRoot = false
                try {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                    val reader = process.inputStream.bufferedReader()
                    val output = reader.readLine() ?: ""
                    process.waitFor()
                    hasRoot = output.contains("uid=0") || output.contains("root")
                } catch (e: Exception) {
                    // Not rooted
                }

                if (hasRoot) {
                    try {
                        val p1 = Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put global block_untrusted_touches 0"))
                        p1.waitFor()
                        val p2 = Runtime.getRuntime().exec(arrayOf("su", "-c", "am compat disable BLOCK_UNTRUSTED_TOUCHES $packageName"))
                        p2.waitFor()
                    } catch (e: Exception) {
                        android.util.Log.e("RetroShaderService", "Failed to disable touch blocking", e)
                    }
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        updateOverlayAlpha(1.0f)
                    }
                } else {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        updateOverlayAlpha(0.8f) // Maximum alpha before system touch filter blocks clicks
                    }
                }
            }.start()

        } catch (e: Exception) {
            android.util.Log.e("RetroShaderService", "Failed to initialize screen capture", e)
            isWarpEnabled = false
        }
    }

    private fun stopScreenCapture() {
        android.util.Log.d("RetroShaderService", "stopScreenCapture called")
        isWarpEnabled = false
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection?.stop()
            mediaProjection = null
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    1338,
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }

            updateOverlayAlpha(0.65f)

            // Re-enable untrusted touch blocking
            Thread {
                try {
                    val p1 = Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put global block_untrusted_touches 2"))
                    p1.waitFor()
                    val p2 = Runtime.getRuntime().exec(arrayOf("su", "-c", "am compat default BLOCK_UNTRUSTED_TOUCHES $packageName"))
                    p2.waitFor()
                } catch (e: Exception) {
                    // Ignore
                }
            }.start()
        } catch (e: Exception) {
            android.util.Log.e("RetroShaderService", "Error stopping screen capture", e)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "retro_shader_glow"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Retro OS Shader Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        })
            .setContentTitle("Retro OS Overlay Active")
            .setContentText("CRT scanlines, curvature, and phosphor grid active.")
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("RetroShaderService", "onStartCommand: intentNotNull=${intent != null}")
        if (intent != null) {
            val enableWarp = intent.getBooleanExtra("enable_warp", false)
            android.util.Log.d("RetroShaderService", "onStartCommand: enableWarp=$enableWarp")
            if (enableWarp) {
                val resultCode = intent.getIntExtra("result_code", 0)
                val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra("projection_data", Intent::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra("projection_data")
                }
                if (data != null && resultCode != 0) {
                    startScreenCapture(resultCode, data)
                }
            } else {
                stopScreenCapture()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        android.util.Log.d("RetroShaderService", "onDestroy called")
        stopScreenCapture()
        super.onDestroy()
        instance = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let { windowManager.removeView(it) }
    }

    companion object {
        @Volatile
        private var instance: RetroShaderService? = null

        fun isRunning(): Boolean = instance != null
    }
}
