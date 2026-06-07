package pro.jayeshseth.devcontrols.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import pro.jayeshseth.devcontrols.ui.present.Deck
import pro.jayeshseth.devcontrols.ui.present.ElementType
import pro.jayeshseth.devcontrols.ui.present.ShapeKind
import pro.jayeshseth.devcontrols.ui.present.Slide

/**
 * Exports a [Deck] to a multi-page PDF or per-slide PNGs by drawing each slide onto a native
 * [Canvas] (gradients / text / shapes / bullets). Files land in MediaStore Downloads; the returned
 * content:// URIs can be opened or shared directly (no FileProvider needed).
 */
object DeckExport {
    private const val W = 1920
    private const val H = 1080

    /** Renders one slide to a freshly allocated [Bitmap] at the given size. */
    fun renderSlide(slide: Slide, w: Int = W, h: Int = H): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawSlide(Canvas(bmp), slide, w, h)
        return bmp
    }

    private fun drawSlide(c: Canvas, slide: Slide, w: Int, h: Int) {
        // background gradient (top-left -> bottom-right)
        val cols = slide.bgColors.map { it.toInt() }.let { if (it.size >= 2) it else it + it }
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), cols.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        val fontScale = h / 720f
        slide.elements.forEach { el ->
            val left = el.x * w; val top = el.y * h; val ew = el.w * w; val eh = el.h * h
            when (el.type) {
                ElementType.SHAPE -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = el.colorArgb.toInt() }
                    val r = RectF(left, top, left + ew, top + eh)
                    if (el.shape == ShapeKind.CIRCLE) c.drawOval(r, p)
                    else c.drawRoundRect(r, 18f * fontScale, 18f * fontScale, p)
                }
                ElementType.BULLETS -> {
                    val tp = textPaint(el.colorArgb.toInt(), el.fontSize * fontScale, el.weight)
                    var y = top
                    el.bullets.forEach { b ->
                        val lay = layout("•  $b", tp, ew.toInt(), false)
                        c.save(); c.translate(left, y); lay.draw(c); c.restore()
                        y += lay.height + 12f * fontScale
                    }
                }
                else -> { // TITLE / SUBTITLE / BODY
                    val tp = textPaint(el.colorArgb.toInt(), el.fontSize * fontScale, el.weight)
                    val lay = layout(el.text ?: "", tp, ew.toInt(), el.centerAlign)
                    c.save(); c.translate(left, top + (eh - lay.height) / 2f); lay.draw(c); c.restore()
                }
            }
        }
    }

    private fun textPaint(color: Int, sizePx: Float, weight: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = sizePx
        typeface = if (weight >= 600) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun layout(text: String, tp: TextPaint, width: Int, center: Boolean): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, tp, width.coerceAtLeast(1))
            .setAlignment(if (center) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL)
            .build()

    /** Writes a multi-page PDF (one page per slide). Returns the content URI or null on failure. */
    fun exportPdf(ctx: Context, deck: Deck): Uri? = runCatching {
        val doc = PdfDocument()
        deck.slides.forEachIndexed { i, s ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, i + 1).create())
            drawSlide(page.canvas, s, W, H)
            doc.finishPage(page)
        }
        val uri = newDownload(ctx, "${safe(deck.title)}.pdf", "application/pdf") ?: return null
        ctx.contentResolver.openOutputStream(uri)!!.use { doc.writeTo(it) }
        doc.close()
        finish(ctx, uri)
        uri
    }.getOrNull()

    /** Writes one PNG per slide. Returns the content URIs (may be empty on failure). */
    fun exportPng(ctx: Context, deck: Deck): List<Uri> = deck.slides.mapIndexedNotNull { i, s ->
        runCatching {
            val bmp = renderSlide(s)
            val uri = newDownload(ctx, "${safe(deck.title)}_${i + 1}.png", "image/png") ?: return@runCatching null
            ctx.contentResolver.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            finish(ctx, uri)
            uri
        }.getOrNull()
    }

    private fun safe(s: String) = s.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "deck" }

    private fun newDownload(ctx: Context, name: String, mime: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return ctx.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
    }

    private fun finish(ctx: Context, uri: Uri) {
        val values = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
        ctx.contentResolver.update(uri, values, null, null)
    }
}
