package pro.jayeshseth.devcontrols.kiosk

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log

/**
 * Toggles the three system animation scales (`window_animation_scale`,
 * `transition_animation_scale`, `animator_duration_scale`) between off (0)
 * and the user's prior values. The prior values are cached in SharedPreferences
 * so we can restore them even after a process death.
 *
 * Requires WRITE_SECURE_SETTINGS, which we already hold.
 */
object AnimationScales {

    private const val TAG = "AnimationScales"
    private const val PREFS = "anim_scale_backup"
    private const val K_WIN = "window_animation_scale"
    private const val K_TRANS = "transition_animation_scale"
    private const val K_ANIM = "animator_duration_scale"
    private const val K_SAVED = "has_saved"

    fun disable(context: Context) {
        val resolver = context.contentResolver
        val backup = backup(context)
        try {
            if (!backup.getBoolean(K_SAVED, false)) {
                val w = Settings.Global.getFloat(resolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f)
                val t = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
                val a = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                backup.edit()
                    .putFloat(K_WIN, w)
                    .putFloat(K_TRANS, t)
                    .putFloat(K_ANIM, a)
                    .putBoolean(K_SAVED, true)
                    .apply()
            }
            Settings.Global.putFloat(resolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
            Settings.Global.putFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        } catch (t: Throwable) {
            Log.w(TAG, "disable failed", t)
        }
    }

    fun restore(context: Context) {
        val resolver = context.contentResolver
        val backup = backup(context)
        if (!backup.getBoolean(K_SAVED, false)) return
        try {
            Settings.Global.putFloat(
                resolver, Settings.Global.WINDOW_ANIMATION_SCALE,
                backup.getFloat(K_WIN, 1f)
            )
            Settings.Global.putFloat(
                resolver, Settings.Global.TRANSITION_ANIMATION_SCALE,
                backup.getFloat(K_TRANS, 1f)
            )
            Settings.Global.putFloat(
                resolver, Settings.Global.ANIMATOR_DURATION_SCALE,
                backup.getFloat(K_ANIM, 1f)
            )
            backup.edit().putBoolean(K_SAVED, false).apply()
        } catch (t: Throwable) {
            Log.w(TAG, "restore failed", t)
        }
    }

    private fun backup(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
