package pro.jayeshseth.devcontrols.nav

import androidx.compose.runtime.Composable

/** A screen renderable on any platform; receives its Back handler. */
typealias ScreenContent = @Composable (onBack: () -> Unit) -> Unit

/**
 * Screens that live in commonMain and run on every platform. Populated batch-by-batch as
 * portable screens migrate (step 4). Android-only screens are NOT here — they are dispatched
 * by [pro.jayeshseth.devcontrols.PlatformScreenHost] on Android and shown as unavailable elsewhere.
 */
val commonScreens: Map<Route, ScreenContent> = emptyMap()
