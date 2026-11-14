package pro.jayeshseth.devcontrols.sdui.registry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.sdui.model.SduiNode
import pro.jayeshseth.devcontrols.sdui.render.SduiRenderContext

typealias SduiCustomComposable = @Composable (node: SduiNode.Custom, context: SduiRenderContext) -> Unit

/**
 * Registry allowing native app host code to supply custom Compose implementations
 * that can be invoked dynamically from the Server-Driven UI AST via [SduiNode.Custom].
 */
class SduiComponentRegistry {
    private val registry = mutableMapOf<String, SduiCustomComposable>()

    fun register(name: String, composable: SduiCustomComposable): SduiComponentRegistry {
        registry[name] = composable
        return this
    }

    fun unregister(name: String) {
        registry.remove(name)
    }

    fun get(name: String): SduiCustomComposable? = registry[name]

    @Composable
    fun renderCustom(node: SduiNode.Custom, context: SduiRenderContext) {
        val renderer = registry[node.componentName]
        if (renderer != null) {
            renderer(node, context)
        } else {
            DefaultFallbackCustomComponent(node)
        }
    }

    companion object {
        val default = SduiComponentRegistry()
    }
}

@Composable
fun DefaultFallbackCustomComponent(node: SduiNode.Custom) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .background(Color.Yellow.copy(alpha = 0.15f))
            .padding(8.dp)
    ) {
        Text(
            text = "Custom Component: ${node.componentName}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
