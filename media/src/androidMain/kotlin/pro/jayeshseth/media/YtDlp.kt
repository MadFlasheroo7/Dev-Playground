package pro.jayeshseth.media

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * Resolves uncapped YouTube stream URLs on Android via **yt-dlp** (the youtubedl-android library, which
 * bundles Python + yt-dlp). This is the Android counterpart to mpv's yt-dlp hook on desktop: yt-dlp uses
 * the `android_vr` client (+ JS-challenge solving) to get stream URLs that aren't PoToken-capped, unlike
 * NewPipeExtractor's. First use unpacks Python and pulls the latest yt-dlp (one-time, needs network).
 */
internal object YtDlp {
    @Volatile private var initialized = false

    @Synchronized
    private fun ensureReady(context: Context) {
        if (initialized) return
        YoutubeDL.getInstance().init(context.applicationContext)
        // Best-effort: pull the newest yt-dlp so extraction keeps working as YouTube changes.
        runCatching { YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext) }
        initialized = true
    }

    /** Returns a directly-playable stream URL for [youtubeWatchUrl]. Blocking; call off the main thread. */
    fun resolveStreamUrl(context: Context, youtubeWatchUrl: String): String {
        ensureReady(context)
        val request = YoutubeDLRequest(youtubeWatchUrl).apply {
            addOption("-f", "bestaudio/best")
            // Prefer the clients that return uncapped URLs without a PoToken.
            addOption("--extractor-args", "youtube:player_client=android_vr,web_safari")
        }
        return YoutubeDL.getInstance().getInfo(request).url
            ?: throw IllegalStateException("yt-dlp returned no stream URL")
    }
}
