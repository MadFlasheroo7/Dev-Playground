package pro.jayeshseth.devcontrols.sdui.render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pro.jayeshseth.devcontrols.sdui.model.SduiDocument
import pro.jayeshseth.devcontrols.sdui.model.defaultSduiJson
import pro.jayeshseth.devcontrols.sdui.registry.SduiComponentRegistry
import pro.jayeshseth.devcontrols.sdui.state.SduiActionRunner
import pro.jayeshseth.devcontrols.sdui.state.SduiStateStore
import pro.jayeshseth.devcontrols.sdui.state.rememberSduiStateStore

/**
 * Top-level Composable that mounts and renders a Server-Driven UI [SduiDocument].
 */
@Composable
fun SduiView(
    document: SduiDocument,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {},
    onNativeEvent: (String, Map<String, String>) -> Unit = { _, _ -> },
    componentRegistry: SduiComponentRegistry = SduiComponentRegistry.default
) {
    val store = rememberSduiStateStore(document.initialState)
    val coroutineScope = rememberCoroutineScope()
    val actionRunner = remember(store, coroutineScope) {
        SduiActionRunner(
            store = store,
            coroutineScope = coroutineScope,
            onNavigate = onNavigate,
            onNativeEvent = onNativeEvent
        )
    }

    SduiViewWithStore(
        document = document,
        store = store,
        actionRunner = actionRunner,
        componentRegistry = componentRegistry,
        modifier = modifier
    )
}

/**
 * Renders an SDUI document with an externally provided [SduiStateStore] and [SduiActionRunner].
 * Useful for studio inspectors and playgrounds.
 */
@Composable
fun SduiViewWithStore(
    document: SduiDocument,
    store: SduiStateStore,
    actionRunner: SduiActionRunner,
    modifier: Modifier = Modifier,
    componentRegistry: SduiComponentRegistry = SduiComponentRegistry.default
) {
    val context = remember(store, actionRunner, componentRegistry) {
        SduiRenderContext(
            store = store,
            actionRunner = actionRunner,
            componentRegistry = componentRegistry
        )
    }

    CompositionLocalProvider(LocalSduiContext provides context) {
        Box(modifier = modifier) {
            SduiNodeRenderer(document.root)
            SduiBottomSheetHost(document.bottomSheet)
        }
    }
}

/**
 * Convenience Composable that parses raw JSON string and renders the SDUI tree,
 * displaying a graceful error banner if JSON is invalid.
 */
@Composable
fun SduiViewFromJson(
    jsonString: String,
    modifier: Modifier = Modifier,
    onNavigate: (String) -> Unit = {},
    onNativeEvent: (String, Map<String, String>) -> Unit = { _, _ -> }
) {
    val parsedResult = remember(jsonString) {
        try {
            val doc = defaultSduiJson.decodeFromString<SduiDocument>(jsonString)
            Result.success(doc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    parsedResult.fold(
        onSuccess = { doc ->
            SduiView(
                document = doc,
                modifier = modifier,
                onNavigate = onNavigate,
                onNativeEvent = onNativeEvent
            )
        },
        onFailure = { err ->
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Color.Red.copy(alpha = 0.1f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "SDUI Parse Error: ${err.message}",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }
        }
    )
}
