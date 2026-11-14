package pro.jayeshseth.bottomsheet

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewRootForInspector
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import java.util.UUID

@Composable
internal actual fun ModalSheetWindow(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val layoutDirection = LocalLayoutDirection.current
    val composition = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)
    val dialogId = remember { UUID.randomUUID() }

    val dialog = remember(view) {
        ModalSheetDialogWrapper(
            onDismissRequest = { currentOnDismiss() },
            composeView = view,
            layoutDirection = layoutDirection,
            dialogId = dialogId
        ).apply {
            setContent(composition) {
                Box(Modifier.fillMaxSize()) {
                    currentContent()
                }
            }
        }
    }

    DisposableEffect(dialog) {
        dialog.show()
        onDispose {
            dialog.dismiss()
            dialog.disposeComposition()
        }
    }

    SideEffect {
        dialog.update(
            onDismissRequest = { currentOnDismiss() },
            layoutDirection = layoutDirection
        )
    }
}

private class ModalSheetDialogLayout(context: android.content.Context, override val window: Window) :
    AbstractComposeView(context), DialogWindowProvider {

    private var content: @Composable () -> Unit by mutableStateOf({})

    override var shouldCreateCompositionOnAttachedToWindow: Boolean = false
        private set

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        setParentCompositionContext(parent)
        this.content = content
        shouldCreateCompositionOnAttachedToWindow = true
        createComposition()
    }

    @Composable
    override fun Content() {
        content()
    }
}

private class ModalSheetDialogWrapper(
    private var onDismissRequest: () -> Unit,
    private val composeView: View,
    layoutDirection: LayoutDirection,
    dialogId: UUID,
) : ComponentDialog(
    android.view.ContextThemeWrapper(
        composeView.context,
        androidx.compose.material3.R.style.EdgeToEdgeFloatingDialogWindowTheme
    )
), ViewRootForInspector {

    private val dialogLayout: ModalSheetDialogLayout

    override val subCompositionView: AbstractComposeView
        get() = dialogLayout

    init {
        val window = window ?: error("Dialog has no window")
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        dialogLayout = ModalSheetDialogLayout(context, window).apply {
            clipChildren = false
        }

        setContentView(dialogLayout)
        dialogLayout.setViewTreeLifecycleOwner(composeView.findViewTreeLifecycleOwner())
        dialogLayout.setViewTreeViewModelStoreOwner(composeView.findViewTreeViewModelStoreOwner())
        dialogLayout.setViewTreeSavedStateRegistryOwner(
            composeView.findViewTreeSavedStateRegistryOwner()
        )

        update(onDismissRequest, layoutDirection)

        onBackPressedDispatcher.addCallback(this) {
            this@ModalSheetDialogWrapper.onDismissRequest()
        }
    }

    fun setContent(parent: CompositionContext, content: @Composable () -> Unit) {
        dialogLayout.setContent(parent, content)
    }

    fun update(
        onDismissRequest: () -> Unit,
        layoutDirection: LayoutDirection
    ) {
        this.onDismissRequest = onDismissRequest
        dialogLayout.layoutDirection = when (layoutDirection) {
            LayoutDirection.Ltr -> android.util.LayoutDirection.LTR
            LayoutDirection.Rtl -> android.util.LayoutDirection.RTL
        }
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        window?.setSoftInputMode(
            if (Build.VERSION.SDK_INT >= 30) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }
        )
    }

    fun disposeComposition() {
        dialogLayout.disposeComposition()
    }

    override fun cancel() {
        // Dismissal lifecycle is controlled by BottomSheetState
    }
}
