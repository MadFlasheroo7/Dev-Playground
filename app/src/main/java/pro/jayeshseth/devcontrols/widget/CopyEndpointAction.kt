package pro.jayeshseth.devcontrols.widget

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

object EndpointKeys {
    val KEY_ENDPOINT = ActionParameters.Key<String>("endpoint")
}

class CopyEndpointAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val endpoint = parameters[EndpointKeys.KEY_ENDPOINT] ?: return
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ADB endpoint", endpoint))
        Toast.makeText(context, "Copied $endpoint", Toast.LENGTH_SHORT).show()
    }
}
