package pro.jayeshseth.devcontrols.wallpaper

import android.app.WallpaperColors
import android.content.Context
import android.graphics.Color as AndroidColor
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.data.LiveWallpaperPrefs
import pro.jayeshseth.devcontrols.data.WallpaperSceneType

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return LiveWallpaperEngine()
    }

    inner class LiveWallpaperEngine : Engine(), SensorEventListener {

        private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private var renderJob: Job? = null
        private var prefsJob: Job? = null

        private val renderer = LiveWallpaperRenderer()
        private val drawScope = CanvasDrawScope()

        private lateinit var sensorManager: SensorManager
        private var accelerometer: Sensor? = null
        private var powerManager: PowerManager? = null

        // Mutable state tracked during runtime
        @Volatile private var isVisibleOnScreen = false
        @Volatile private var surfaceWidth = 1080
        @Volatile private var surfaceHeight = 2400

        @Volatile private var rawTiltX = 0f
        @Volatile private var rawTiltY = 0f
        @Volatile private var smoothTiltX = 0f
        @Volatile private var smoothTiltY = 0f

        @Volatile private var launcherOffset = 0.5f

        // Cached settings from DataStore
        @Volatile private var currentScene = WallpaperSceneType.DEEP_NEBULA
        @Volatile private var gyroEnabled = true
        @Volatile private var gyroSensitivity = 1.0f
        @Volatile private var touchEnabled = true
        @Volatile private var touchIntensity = 1.0f
        @Volatile private var launcherParallaxEnabled = true
        @Volatile private var targetFps = 60
        @Volatile private var batteryThrottleEnabled = true
        @Volatile private var monetEnabled = true
        @Volatile private var particleDensity = 1.0f
        @Volatile private var animationSpeed = 1.0f

        private val activeRipples = ArrayList<TouchRipple>()
        private var lastFrameNanos = 0L
        private var totalElapsedSeconds = 0f

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(true)
            setOffsetNotificationsEnabled(true)

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager

            observePrefs()
        }

        private fun observePrefs() {
            val prefs = LiveWallpaperPrefs.get(applicationContext)
            prefsJob = serviceScope.launch {
                launch {
                    prefs.selectedScene.collect { scene ->
                        val changed = currentScene != scene
                        currentScene = scene
                        if (changed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            notifyColorsChanged()
                        }
                    }
                }
                launch { prefs.gyroParallaxEnabled.collect { gyroEnabled = it } }
                launch { prefs.gyroSensitivity.collect { gyroSensitivity = it } }
                launch { prefs.touchReactiveEnabled.collect { touchEnabled = it } }
                launch { prefs.touchIntensity.collect { touchIntensity = it } }
                launch { prefs.launcherParallaxEnabled.collect { launcherParallaxEnabled = it } }
                launch { prefs.targetFps.collect { targetFps = it } }
                launch { prefs.batterySaverThrottle.collect { batteryThrottleEnabled = it } }
                launch {
                    prefs.monetThemingEnabled.collect {
                        monetEnabled = it
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            notifyColorsChanged()
                        }
                    }
                }
                launch { prefs.particleDensity.collect { particleDensity = it } }
                launch { prefs.animationSpeed.collect { animationSpeed = it } }
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            stopRendering()
            prefsJob?.cancel()
            serviceScope.cancel()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            isVisibleOnScreen = visible
            if (visible) {
                registerSensors()
                startRendering()
            } else {
                unregisterSensors()
                stopRendering()
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            if (isVisibleOnScreen) {
                startRendering()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            surfaceWidth = width
            surfaceHeight = height
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            stopRendering()
        }

        override fun onOffsetsChanged(
            xOffset: Float, yOffset: Float,
            xOffsetStep: Float, yOffsetStep: Float,
            xPixelOffset: Int, yPixelOffset: Int
        ) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset, yPixelOffset)
            if (launcherParallaxEnabled) {
                launcherOffset = xOffset.coerceIn(0f, 1f)
            }
        }

        override fun onTouchEvent(event: MotionEvent) {
            super.onTouchEvent(event)
            if (!touchEnabled) return

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    val idx = event.actionIndex
                    addRipple(event.getX(idx), event.getY(idx))
                }
                MotionEvent.ACTION_MOVE -> {
                    // Sample move points occasionally
                    if (event.historySize > 0 && activeRipples.size < 8) {
                        addRipple(event.x, event.y)
                    }
                }
            }
        }

        private fun addRipple(x: Float, y: Float) {
            synchronized(activeRipples) {
                if (activeRipples.size >= 12) {
                    activeRipples.removeAt(0)
                }
                val rippleColor = Color(currentScene.accentColorHex)
                activeRipples.add(
                    TouchRipple(
                        x = x,
                        y = y,
                        color = rippleColor
                    )
                )
            }
        }

        override fun onComputeColors(): WallpaperColors? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && monetEnabled) {
                val primary = AndroidColor.valueOf(
                    ((currentScene.primaryColorHex shr 16) and 0xFF) / 255f,
                    ((currentScene.primaryColorHex shr 8) and 0xFF) / 255f,
                    (currentScene.primaryColorHex and 0xFF) / 255f
                )
                val secondary = AndroidColor.valueOf(
                    ((currentScene.secondaryColorHex shr 16) and 0xFF) / 255f,
                    ((currentScene.secondaryColorHex shr 8) and 0xFF) / 255f,
                    (currentScene.secondaryColorHex and 0xFF) / 255f
                )
                val tertiary = AndroidColor.valueOf(
                    ((currentScene.accentColorHex shr 16) and 0xFF) / 255f,
                    ((currentScene.accentColorHex shr 8) and 0xFF) / 255f,
                    (currentScene.accentColorHex and 0xFF) / 255f
                )
                return WallpaperColors(primary, secondary, tertiary)
            }
            return super.onComputeColors()
        }

        // ---------------------------------------------------------------------
        // SENSOR FUSION (Low-Pass Filtered Tilt)
        // ---------------------------------------------------------------------
        private fun registerSensors() {
            if (gyroEnabled && accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            }
        }

        private fun unregisterSensors() {
            sensorManager.unregisterListener(this)
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // event.values[0] = x (roll), event.values[1] = y (pitch)
                // Normalize to -1..1 range approx
                rawTiltX = (-event.values[0] / 9.8f).coerceIn(-1.5f, 1.5f)
                rawTiltY = (event.values[1] / 9.8f).coerceIn(-1.5f, 1.5f)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        // ---------------------------------------------------------------------
        // RENDER LOOP
        // ---------------------------------------------------------------------
        private fun startRendering() {
            if (renderJob?.isActive == true) return
            lastFrameNanos = System.nanoTime()

            renderJob = serviceScope.launch {
                val density = Density(applicationContext.resources.displayMetrics.density)

                while (isActive && isVisibleOnScreen) {
                    val now = System.nanoTime()
                    val dtNanos = if (lastFrameNanos > 0L) (now - lastFrameNanos) else 16_666_666L
                    lastFrameNanos = now
                    val dt = (dtNanos / 1_000_000_000f).coerceIn(0.001f, 0.1f)
                    totalElapsedSeconds += dt

                    // Smooth low-pass filter on tilt
                    val alpha = (12f * dt).coerceIn(0.05f, 1f)
                    val targetTx = if (gyroEnabled) rawTiltX * gyroSensitivity else 0f
                    val targetTy = if (gyroEnabled) rawTiltY * gyroSensitivity else 0f
                    smoothTiltX += (targetTx - smoothTiltX) * alpha
                    smoothTiltY += (targetTy - smoothTiltY) * alpha

                    // Update ripple lifespans
                    synchronized(activeRipples) {
                        val iterator = activeRipples.iterator()
                        while (iterator.hasNext()) {
                            val r = iterator.next()
                            r.age += dt
                            if (r.age >= r.maxAge) {
                                iterator.remove()
                            }
                        }
                    }

                    // Render to Surface Canvas
                    drawFrame(density, dt)

                    // Target Frame Rate Throttling
                    val isPowerSave = powerManager?.isPowerSaveMode == true
                    val effectiveFps = if (isPowerSave && batteryThrottleEnabled) {
                        30.coerceAtMost(targetFps)
                    } else {
                        targetFps
                    }
                    val frameBudgetMs = (1000L / effectiveFps).coerceAtLeast(8L)
                    val elapsedMs = (System.nanoTime() - now) / 1_000_000L
                    val sleepMs = (frameBudgetMs - elapsedMs).coerceAtLeast(2L)

                    delay(sleepMs)
                }
            }
        }

        private fun stopRendering() {
            renderJob?.cancel()
            renderJob = null
        }

        private fun drawFrame(density: Density, dt: Float) {
            val holder = surfaceHolder ?: return
            if (!holder.surface.isValid) return

            val canvas = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }
            } catch (e: Exception) {
                null
            } ?: return

            try {
                val ripplesSnapshot = synchronized(activeRipples) { ArrayList(activeRipples) }

                val state = WallpaperRenderState(
                    scene = currentScene,
                    timeSeconds = totalElapsedSeconds,
                    tiltX = smoothTiltX,
                    tiltY = smoothTiltY,
                    launcherOffset = launcherOffset,
                    particleDensity = particleDensity,
                    animationSpeed = animationSpeed,
                    touchIntensity = touchIntensity,
                    activeRipples = ripplesSnapshot
                )

                val composeCanvas = ComposeCanvas(canvas)
                val size = Size(surfaceWidth.toFloat(), surfaceHeight.toFloat())

                drawScope.draw(
                    density = density,
                    layoutDirection = LayoutDirection.Ltr,
                    canvas = composeCanvas,
                    size = size
                ) {
                    renderer.render(this, state, dt)
                }
            } finally {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (ignored: Exception) {}
            }
        }
    }
}
