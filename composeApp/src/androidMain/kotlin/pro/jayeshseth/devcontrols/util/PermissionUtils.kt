package pro.jayeshseth.devcontrols.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionUtils {
    fun hasWriteSecureSettings(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

    fun hasWriteSettings(context: Context): Boolean =
        Settings.System.canWrite(context)

    /**
     * POST_NOTIFICATIONS is a runtime permission on API 33+. Implicitly granted
     * on older versions.
     */
    fun hasPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    fun hasWatchAppOps(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            "android.permission.WATCH_APPOPS"
        ) == PackageManager.PERMISSION_GRANTED
}
