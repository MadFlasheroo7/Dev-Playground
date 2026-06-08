package pro.jayeshseth.devcontrols.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * No-accounts "share fan-out": compose text / link / image once, then hand off to each
 * social app. Each helper prefers the installed app (targeted `ACTION_SEND`) and falls
 * back to a web deep-link where the platform offers one. Failures are swallowed with a
 * Toast so one missing app never crashes the flow.
 *
 * Platform reality (not our limitation): WhatsApp & X accept prefilled text; LinkedIn &
 * Facebook only accept a URL (their policy blocks text prefill); Instagram needs media
 * and never prefills a caption. True one-tap auto-posting needs the official APIs — see
 * PLAN.md Phase 2/3.
 */
object SocialShare {

    const val PKG_TWITTER = "com.twitter.android"
    const val PKG_WHATSAPP = "com.whatsapp"
    const val PKG_WHATSAPP_BIZ = "com.whatsapp.w4b"
    const val PKG_LINKEDIN = "com.linkedin.android"
    const val PKG_INSTAGRAM = "com.instagram.android"
    const val PKG_FACEBOOK = "com.facebook.katana"

    fun isInstalled(ctx: Context, pkg: String): Boolean =
        runCatching { ctx.packageManager.getPackageInfo(pkg, 0); true }.getOrDefault(false)

    /** First of [pkgs] that is installed, or null. */
    private fun firstInstalled(ctx: Context, vararg pkgs: String): String? =
        pkgs.firstOrNull { isInstalled(ctx, it) }

    private fun combine(text: String, url: String): String =
        listOf(text.trim(), url.trim()).filter { it.isNotEmpty() }.joinToString("\n")

    private fun launch(ctx: Context, intent: Intent, failMsg: String) {
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { toast(ctx, failMsg) }
    }

    private fun toast(ctx: Context, msg: String) =
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    private fun openWeb(ctx: Context, url: String) =
        launch(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No browser to open $url")

    /** Targeted `ACTION_SEND` of an image (+optional caption) to a specific app. */
    private fun sendImage(ctx: Context, pkg: String, image: Uri, caption: String?) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, image)
            if (!caption.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            setPackage(pkg)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(ctx, intent, "Couldn't open $pkg")
    }

    // -------------------- per-platform --------------------

    fun shareX(ctx: Context, text: String, url: String, image: Uri?) {
        if (image != null && isInstalled(ctx, PKG_TWITTER)) {
            sendImage(ctx, PKG_TWITTER, image, combine(text, url))
        } else {
            openWeb(ctx, "https://twitter.com/intent/tweet?text=" + Uri.encode(combine(text, url)))
        }
    }

    fun shareWhatsApp(ctx: Context, text: String, url: String, image: Uri?) {
        val caption = combine(text, url)
        val pkg = firstInstalled(ctx, PKG_WHATSAPP, PKG_WHATSAPP_BIZ)
        if (image != null && pkg != null) {
            sendImage(ctx, pkg, image, caption)
        } else {
            openWeb(ctx, "https://wa.me/?text=" + Uri.encode(caption))
        }
    }

    /** LinkedIn only accepts a URL (no text prefill by policy). */
    fun shareLinkedIn(ctx: Context, url: String) {
        if (url.isBlank()) { toast(ctx, "LinkedIn needs a link URL"); return }
        openWeb(ctx, "https://www.linkedin.com/sharing/share-offsite/?url=" + Uri.encode(url.trim()))
    }

    /** Instagram needs media; there's no caption prefill and no web post. */
    fun shareInstagram(ctx: Context, image: Uri?) {
        if (image == null) { toast(ctx, "Instagram needs an image"); return }
        if (!isInstalled(ctx, PKG_INSTAGRAM)) { toast(ctx, "Instagram isn't installed"); return }
        sendImage(ctx, PKG_INSTAGRAM, image, null)
    }

    fun shareFacebook(ctx: Context, url: String, image: Uri?) {
        if (image != null && isInstalled(ctx, PKG_FACEBOOK)) {
            sendImage(ctx, PKG_FACEBOOK, image, null)
        } else if (url.isNotBlank()) {
            openWeb(ctx, "https://www.facebook.com/sharer/sharer.php?u=" + Uri.encode(url.trim()))
        } else {
            toast(ctx, "Facebook needs a link URL or an image")
        }
    }

    /** System chooser — covers everything else (Telegram, Mastodon, email, …). */
    fun systemShare(ctx: Context, text: String, url: String, image: Uri?) {
        val body = combine(text, url)
        val send = Intent(Intent.ACTION_SEND).apply {
            if (image != null) {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, image)
                if (body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, body)
            } else {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launch(ctx, Intent.createChooser(send, "Share via"), "Nothing to share to")
    }
}
