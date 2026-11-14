package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.liveWallpaperDataStore: DataStore<Preferences> by preferencesDataStore(name = "live_wallpaper_prefs")

enum class WallpaperSceneType(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long
) {
    DEEP_NEBULA(
        id = "deep_nebula",
        title = "Deep Nebula",
        subtitle = "Gas clouds • graviton vortex • stellar dust",
        icon = "✦",
        primaryColorHex = 0xFF090615,
        secondaryColorHex = 0xFF28114B,
        accentColorHex = 0xFFC084FC
    ),
    EVENT_HORIZON(
        id = "event_horizon",
        title = "Event Horizon",
        subtitle = "Singularity • photon ring • relativistic accretion",
        icon = "◎",
        primaryColorHex = 0xFF040407,
        secondaryColorHex = 0xFF1E0D05,
        accentColorHex = 0xFFFF9E0B
    ),
    SAKURA_DUSK(
        id = "sakura_dusk",
        title = "Sakura Dusk",
        subtitle = "Mount Fuji sunset • drifting petals • gravity wind",
        icon = "🌸",
        primaryColorHex = 0xFF25142E,
        secondaryColorHex = 0xFF5B1E48,
        accentColorHex = 0xFFF472B6
    ),
    NEO_TOKYO(
        id = "neo_tokyo",
        title = "Neo Tokyo",
        subtitle = "Rain on glass • neon kanji • chromatic wash",
        icon = "⚡",
        primaryColorHex = 0xFF080B16,
        secondaryColorHex = 0xFF141936,
        accentColorHex = 0xFF06B6D4
    ),
    HYPERSPACE_4D(
        id = "hyperspace_4d",
        title = "4D Hyperspace",
        subtitle = "Rotating Tesseract • SO(4) projection • hyperplane tilt",
        icon = "🧊",
        primaryColorHex = 0xFF050811,
        secondaryColorHex = 0xFF0A2239,
        accentColorHex = 0xFF38BDF8
    ),
    EULERIAN_FLUID(
        id = "eulerian_fluid",
        title = "Eulerian Fluid",
        subtitle = "Navier-Stokes advection • touch dye plumes",
        icon = "🌊",
        primaryColorHex = 0xFF030712,
        secondaryColorHex = 0xFF111827,
        accentColorHex = 0xFF10B981
    ),
    MATRIX_RAIN(
        id = "matrix_rain",
        title = "Matrix Rain",
        subtitle = "Digital phosphor stream • glyph cascade • touch shock",
        icon = "🟩",
        primaryColorHex = 0xFF020904,
        secondaryColorHex = 0xFF05200C,
        accentColorHex = 0xFF22C55E
    ),
    AURORA_BOREALIS(
        id = "aurora_borealis",
        title = "Aurora Borealis",
        subtitle = "Ionized ribbons • solar wind sway • geomagnetic glow",
        icon = "🌌",
        primaryColorHex = 0xFF020B14,
        secondaryColorHex = 0xFF052B28,
        accentColorHex = 0xFF2DD4BF
    );

    companion object {
        fun fromId(id: String): WallpaperSceneType =
            entries.firstOrNull { it.id == id } ?: DEEP_NEBULA
    }
}

class LiveWallpaperPrefs private constructor(private val context: Context) {
    private val store get() = context.liveWallpaperDataStore

    val selectedScene: Flow<WallpaperSceneType> = store.data.map {
        WallpaperSceneType.fromId(it[KEY_SELECTED_SCENE] ?: WallpaperSceneType.DEEP_NEBULA.id)
    }

    val gyroParallaxEnabled: Flow<Boolean> = store.data.map { it[KEY_GYRO_ENABLED] ?: true }
    val gyroSensitivity: Flow<Float> = store.data.map { it[KEY_GYRO_SENSITIVITY] ?: 1.0f }

    val touchReactiveEnabled: Flow<Boolean> = store.data.map { it[KEY_TOUCH_ENABLED] ?: true }
    val touchIntensity: Flow<Float> = store.data.map { it[KEY_TOUCH_INTENSITY] ?: 1.0f }

    val launcherParallaxEnabled: Flow<Boolean> = store.data.map { it[KEY_LAUNCHER_PARALLAX] ?: true }
    val targetFps: Flow<Int> = store.data.map { it[KEY_TARGET_FPS] ?: 60 }
    val batterySaverThrottle: Flow<Boolean> = store.data.map { it[KEY_BATTERY_SAVER_THROTTLE] ?: true }
    val monetThemingEnabled: Flow<Boolean> = store.data.map { it[KEY_MONET_THEMING] ?: true }
    val particleDensity: Flow<Float> = store.data.map { it[KEY_PARTICLE_DENSITY] ?: 1.0f }
    val animationSpeed: Flow<Float> = store.data.map { it[KEY_ANIMATION_SPEED] ?: 1.0f }

    suspend fun setSelectedScene(scene: WallpaperSceneType) {
        store.edit { it[KEY_SELECTED_SCENE] = scene.id }
    }

    suspend fun setGyroParallaxEnabled(enabled: Boolean) {
        store.edit { it[KEY_GYRO_ENABLED] = enabled }
    }

    suspend fun setGyroSensitivity(value: Float) {
        store.edit { it[KEY_GYRO_SENSITIVITY] = value }
    }

    suspend fun setTouchReactiveEnabled(enabled: Boolean) {
        store.edit { it[KEY_TOUCH_ENABLED] = enabled }
    }

    suspend fun setTouchIntensity(value: Float) {
        store.edit { it[KEY_TOUCH_INTENSITY] = value }
    }

    suspend fun setLauncherParallaxEnabled(enabled: Boolean) {
        store.edit { it[KEY_LAUNCHER_PARALLAX] = enabled }
    }

    suspend fun setTargetFps(fps: Int) {
        store.edit { it[KEY_TARGET_FPS] = fps }
    }

    suspend fun setBatterySaverThrottle(enabled: Boolean) {
        store.edit { it[KEY_BATTERY_SAVER_THROTTLE] = enabled }
    }

    suspend fun setMonetThemingEnabled(enabled: Boolean) {
        store.edit { it[KEY_MONET_THEMING] = enabled }
    }

    suspend fun setParticleDensity(density: Float) {
        store.edit { it[KEY_PARTICLE_DENSITY] = density }
    }

    suspend fun setAnimationSpeed(speed: Float) {
        store.edit { it[KEY_ANIMATION_SPEED] = speed }
    }

    companion object {
        private val KEY_SELECTED_SCENE = stringPreferencesKey("selected_scene")
        private val KEY_GYRO_ENABLED = booleanPreferencesKey("gyro_enabled")
        private val KEY_GYRO_SENSITIVITY = floatPreferencesKey("gyro_sensitivity")
        private val KEY_TOUCH_ENABLED = booleanPreferencesKey("touch_enabled")
        private val KEY_TOUCH_INTENSITY = floatPreferencesKey("touch_intensity")
        private val KEY_LAUNCHER_PARALLAX = booleanPreferencesKey("launcher_parallax")
        private val KEY_TARGET_FPS = intPreferencesKey("target_fps")
        private val KEY_BATTERY_SAVER_THROTTLE = booleanPreferencesKey("battery_saver_throttle")
        private val KEY_MONET_THEMING = booleanPreferencesKey("monet_theming")
        private val KEY_PARTICLE_DENSITY = floatPreferencesKey("particle_density")
        private val KEY_ANIMATION_SPEED = floatPreferencesKey("animation_speed")

        @Volatile
        private var instance: LiveWallpaperPrefs? = null

        fun get(context: Context): LiveWallpaperPrefs =
            instance ?: synchronized(this) {
                instance ?: LiveWallpaperPrefs(context.applicationContext).also { instance = it }
            }
    }
}
