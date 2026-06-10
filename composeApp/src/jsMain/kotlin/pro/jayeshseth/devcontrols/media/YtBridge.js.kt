package pro.jayeshseth.devcontrols.media

// YouTube IFrame Player bridge (window.__yt is set up in index.html).
internal actual fun ytLoad(id: String) { js("window.__yt && window.__yt.load(id)") }
internal actual fun ytPlay() { js("window.__yt && window.__yt.play()") }
internal actual fun ytPause() { js("window.__yt && window.__yt.pause()") }
internal actual fun ytSeek(sec: Double) { js("window.__yt && window.__yt.seek(sec)") }
internal actual fun ytTimeMs(): Double = js("window.__yt ? window.__yt.timeMs() : 0")
internal actual fun ytDurationMs(): Double = js("window.__yt ? window.__yt.durationMs() : 0")
internal actual fun ytState(): Double = js("window.__yt ? window.__yt.state() : -1")
