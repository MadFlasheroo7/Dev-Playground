package pro.jayeshseth.devcontrols.ui

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/** Floating Bubble — a draggable chat-head drawn in a TYPE_APPLICATION_OVERLAY window. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingBubbleLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var shown by remember { mutableStateOf(false) }
    val holder = remember { BubbleHolder(context.applicationContext) }

    DisposableEffect(Unit) { onDispose { holder.hide() } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Floating Bubble") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        modifier = Modifier.fillMaxSize()
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize().padding(16.dp)) {
            canDraw = Settings.canDrawOverlays(context)
            if (!canDraw) {
                Text("Needs the “Display over other apps” permission.", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text("Grant overlay permission") }
            } else {
                Button(onClick = {
                    if (shown) { holder.hide(); shown = false } else { holder.show(); shown = true }
                }) { Text(if (shown) "Hide bubble" else "Show bubble") }
                Spacer(Modifier.height(12.dp))
                Text("The bubble floats above all apps. Drag it around; leave this screen and it auto-dismisses. " +
                    "Tap it to pop it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private class BubbleHolder(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null

    fun show() {
        if (view != null) return
        val size = (64 * context.resources.displayMetrics.density).roundToInt()
        val bubble = TextView(context).apply {
            text = "●"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xFF3D5AFE.toInt()) }
        }
        val params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 40; y = 240 }

        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0; var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downX = e.rawX; downY = e.rawY; startX = params.x; startY = params.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (e.rawX - downX).toInt(); params.y = startY + (e.rawY - downY).toInt()
                    if (abs(e.rawX - downX) > 12 || abs(e.rawY - downY) > 12) moved = true
                    runCatching { wm.updateViewLayout(bubble, params) }; true
                }
                MotionEvent.ACTION_UP -> { if (!moved) hide(); true }
                else -> false
            }
        }
        runCatching { wm.addView(bubble, params); view = bubble }
    }

    fun hide() { view?.let { runCatching { wm.removeView(it) } }; view = null }
}
