package pro.jayeshseth.devcontrols.platform

// Browser Fullscreen API. requestFullscreen() needs transient user activation (the tap that opened the
// deck), and returns a promise we intentionally ignore.
internal actual fun enterFullscreen() {
    js("if (document.documentElement.requestFullscreen) { document.documentElement.requestFullscreen().catch(function(){}); }")
}
internal actual fun exitFullscreen() {
    js("if (document.fullscreenElement && document.exitFullscreen) { document.exitFullscreen().catch(function(){}); }")
}
