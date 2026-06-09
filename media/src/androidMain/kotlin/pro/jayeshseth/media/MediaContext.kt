package pro.jayeshseth.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri

/**
 * Holds the application [Context] that the Android PoToken [WebView][android.webkit.WebView] needs.
 *
 * `:media` is a Compose-free library with a context-less public API ([createMediaSource]), but a WebView
 * requires a `Context`. Rather than thread one through every call, we capture the *application* context
 * automatically at process start via [MediaContextProvider] — so the consuming app needs no extra wiring.
 */
internal object MediaContextHolder {
    @Volatile
    var app: Context? = null
        private set

    fun set(context: Context) {
        if (app == null) app = context.applicationContext
    }
}

/**
 * A no-op [ContentProvider] declared in this library's manifest purely so the system instantiates it at
 * app launch (before `Application.onCreate`) and we can grab the application [Context]. This is the same
 * trick AndroidX `Startup`/`ProcessLifecycleOwner`/WorkManager use for zero-config initialization.
 */
class MediaContextProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { MediaContextHolder.set(it) }
        return true
    }

    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}
