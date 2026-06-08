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
import pro.jayeshseth.devcontrols.ui.present.Entrance
import pro.jayeshseth.devcontrols.ui.present.ShapeKind
import pro.jayeshseth.devcontrols.ui.present.Slide
import pro.jayeshseth.devcontrols.ui.present.SlideElement
import pro.jayeshseth.devcontrols.ui.present.TransitionIn
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

/**
 * Exports a [Deck] to a multi-page PDF or per-slide PNGs by drawing each slide onto a native
 * [Canvas] (gradients / text / shapes / bullets). Files land in MediaStore Downloads; the returned
 * content:// URIs can be opened or shared directly (no FileProvider needed).
 */
object DeckExport {
    private const val W = 1920
    private const val H = 1080

    /** Renders one complete slide (background + every element) to a freshly allocated [Bitmap]. */
    fun renderSlide(slide: Slide, w: Int = W, h: Int = H): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawSlide(Canvas(bmp), slide, w, h, includeAnimated = true)
        return bmp
    }

    /** Renders just the background gradient + non-animated elements (for the animated PPTX base). */
    private fun renderBackground(slide: Slide, w: Int = W, h: Int = H): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawSlide(Canvas(bmp), slide, w, h, includeAnimated = false)
        return bmp
    }

    /** Renders a single element on a transparent bitmap sized to its bounding box. */
    private fun renderElement(el: SlideElement, w: Int = W, h: Int = H): Bitmap {
        val ew = (el.w * w).toInt().coerceAtLeast(1); val eh = (el.h * h).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(ew, eh, Bitmap.Config.ARGB_8888)
        drawElement(Canvas(bmp), el, w, h, el.x * w, el.y * h)
        return bmp
    }

    private fun drawSlide(c: Canvas, slide: Slide, w: Int, h: Int, includeAnimated: Boolean) {
        val cols = slide.bgColors.map { it.toInt() }.let { if (it.size >= 2) it else it + it }
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), cols.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)
        slide.elements.forEach { el ->
            if (!includeAnimated && el.entrance != Entrance.NONE) return@forEach
            drawElement(c, el, w, h, 0f, 0f)
        }
    }

    /** Draws one element. [ox]/[oy] shift the origin (use the element's px offset to draw at 0,0). */
    private fun drawElement(c: Canvas, el: SlideElement, w: Int, h: Int, ox: Float, oy: Float) {
        val fontScale = h / 720f
        val left = el.x * w - ox; val top = el.y * h - oy; val ew = el.w * w; val eh = el.h * h
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
            drawSlide(page.canvas, s, W, H, includeAnimated = true)
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

    // ---------------------------------------------------------------------------------------------
    // PPTX export — each slide is rendered to a full-bleed PNG embedded in a hand-built OOXML zip.
    // Image-based (text isn't editable in PowerPoint) but visually faithful and opens everywhere.
    // ---------------------------------------------------------------------------------------------
    private const val EMU_W = 12192000  // 13.333in widescreen
    private const val EMU_H = 6858000   // 7.5in

    fun exportPptx(ctx: Context, deck: Deck): Uri? = runCatching {
        val n = deck.slides.size
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            fun put(path: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry() }
            fun putStr(path: String, s: String) = put(path, s.toByteArray(Charsets.UTF_8))

            putStr("[Content_Types].xml", contentTypes(n))
            putStr("_rels/.rels", ROOT_RELS)
            putStr("ppt/presentation.xml", presentationXml(n))
            putStr("ppt/_rels/presentation.xml.rels", presentationRels(n))
            putStr("ppt/theme/theme1.xml", THEME)
            putStr("ppt/slideMasters/slideMaster1.xml", SLIDE_MASTER)
            putStr("ppt/slideMasters/_rels/slideMaster1.xml.rels", MASTER_RELS)
            putStr("ppt/slideLayouts/slideLayout1.xml", SLIDE_LAYOUT)
            putStr("ppt/slideLayouts/_rels/slideLayout1.xml.rels", LAYOUT_RELS)
            deck.slides.forEachIndexed { i, s ->
                val idx = i + 1
                // background (gradient + static elements) = image{idx}.png -> rId1, shape id 2
                put("ppt/media/image$idx.png", png(renderBackground(s)))
                // each animated element gets its own positioned picture (so it can animate in)
                val animated = s.elements.filter { it.entrance != Entrance.NONE }
                animated.forEachIndexed { k, el ->
                    put("ppt/media/image${idx}_${k + 1}.png", png(renderElement(el)))
                }
                putStr("ppt/slides/slide$idx.xml", slideXml(s, animated))
                putStr("ppt/slides/_rels/slide$idx.xml.rels", slideRels(idx, animated.size))
            }
        }
        val uri = newDownload(ctx, "${safe(deck.title)}.pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation") ?: return null
        ctx.contentResolver.openOutputStream(uri)!!.use { it.write(baos.toByteArray()) }
        finish(ctx, uri)
        uri
    }.getOrNull()

    private const val XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>"""
    private const val NS_A = "http://schemas.openxmlformats.org/drawingml/2006/main"
    private const val NS_R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_P = "http://schemas.openxmlformats.org/presentationml/2006/main"
    private const val NS_PKG = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val RT = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    private fun contentTypes(n: Int) = buildString {
        append(XML)
        append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
        append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
        append("""<Default Extension="xml" ContentType="application/xml"/>""")
        append("""<Default Extension="png" ContentType="image/png"/>""")
        append("""<Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>""")
        append("""<Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>""")
        append("""<Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>""")
        append("""<Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>""")
        for (i in 1..n) append("""<Override PartName="/ppt/slides/slide$i.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>""")
        append("</Types>")
    }

    private val ROOT_RELS = """$XML<Relationships xmlns="$NS_PKG"><Relationship Id="rId1" Type="$RT/officeDocument" Target="ppt/presentation.xml"/></Relationships>"""

    private fun presentationXml(n: Int) = buildString {
        append(XML)
        append("""<p:presentation xmlns:a="$NS_A" xmlns:r="$NS_R" xmlns:p="$NS_P">""")
        append("""<p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rIdMaster"/></p:sldMasterIdLst>""")
        append("<p:sldIdLst>")
        for (i in 1..n) append("""<p:sldId id="${255 + i}" r:id="rId$i"/>""")
        append("</p:sldIdLst>")
        append("""<p:sldSz cx="$EMU_W" cy="$EMU_H" type="screen16x9"/>""")
        append("""<p:notesSz cx="6858000" cy="9144000"/>""")
        append("</p:presentation>")
    }

    private fun presentationRels(n: Int) = buildString {
        append("""$XML<Relationships xmlns="$NS_PKG">""")
        for (i in 1..n) append("""<Relationship Id="rId$i" Type="$RT/slide" Target="slides/slide$i.xml"/>""")
        append("""<Relationship Id="rIdMaster" Type="$RT/slideMaster" Target="slideMasters/slideMaster1.xml"/>""")
        append("""<Relationship Id="rIdTheme" Type="$RT/theme" Target="theme/theme1.xml"/>""")
        append("</Relationships>")
    }

    private val SLIDE_MASTER = """$XML<p:sldMaster xmlns:a="$NS_A" xmlns:r="$NS_R" xmlns:p="$NS_P"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/><p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst></p:sldMaster>"""

    private val MASTER_RELS = """$XML<Relationships xmlns="$NS_PKG"><Relationship Id="rId1" Type="$RT/slideLayout" Target="../slideLayouts/slideLayout1.xml"/><Relationship Id="rId2" Type="$RT/theme" Target="../theme/theme1.xml"/></Relationships>"""

    private val SLIDE_LAYOUT = """$XML<p:sldLayout xmlns:a="$NS_A" xmlns:r="$NS_R" xmlns:p="$NS_P" type="blank" preserve="1"><p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>"""

    private val LAYOUT_RELS = """$XML<Relationships xmlns="$NS_PKG"><Relationship Id="rId1" Type="$RT/slideMaster" Target="../slideMasters/slideMaster1.xml"/></Relationships>"""

    private fun png(b: Bitmap): ByteArray =
        ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

    private fun slideXml(slide: Slide, animated: List<SlideElement>): String {
        val pics = StringBuilder()
        pics.append("""<p:pic><p:nvPicPr><p:cNvPr id="2" name="bg"/><p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/></p:nvPicPr><p:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$EMU_W" cy="$EMU_H"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr></p:pic>""")
        animated.forEachIndexed { k, el ->
            val id = 3 + k; val rid = 2 + k
            val x = (el.x * EMU_W).roundToInt(); val y = (el.y * EMU_H).roundToInt()
            val cx = (el.w * EMU_W).roundToInt(); val cy = (el.h * EMU_H).roundToInt()
            pics.append("""<p:pic><p:nvPicPr><p:cNvPr id="$id" name="el$k"/><p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/></p:nvPicPr><p:blipFill><a:blip r:embed="rId$rid"/><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$cx" cy="$cy"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr></p:pic>""")
        }
        val spids = animated.indices.map { 3 + it }
        return """$XML<p:sld xmlns:a="$NS_A" xmlns:r="$NS_R" xmlns:p="$NS_P"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="0" cy="0"/><a:chOff x="0" y="0"/><a:chExt cx="0" cy="0"/></a:xfrm></p:grpSpPr>$pics</p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>${transitionXml(slide.transitionIn)}${timingXml(spids)}</p:sld>"""
    }

    private fun transitionXml(t: TransitionIn) = when (t) {
        TransitionIn.FADE -> """<p:transition spd="med"><p:fade/></p:transition>"""
        TransitionIn.PUSH -> """<p:transition spd="med"><p:push dir="l"/></p:transition>"""
        TransitionIn.ZOOM -> """<p:transition spd="med"><p:zoom dir="in"/></p:transition>"""
        TransitionIn.NONE -> ""
    }

    /** Auto-cascading fade-in build for every animated element (style.visibility + fade). */
    private fun timingXml(spids: List<Int>): String {
        if (spids.isEmpty()) return ""
        val effects = StringBuilder()
        spids.forEachIndexed { j, spid ->
            val e = 5 + j * 3
            val delay = if (j == 0) 0 else 200
            effects.append(
                """<p:par><p:cTn id="$e" presetID="10" presetClass="entr" presetSubtype="0" fill="hold" grpId="0" nodeType="afterEffect"><p:stCondLst><p:cond delay="$delay"/></p:stCondLst><p:childTnLst>""" +
                """<p:set><p:cBhvr><p:cTn id="${e + 1}" dur="1" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst></p:cTn><p:tgtEl><p:spTgt spid="$spid"/></p:tgtEl><p:attrNameLst><p:attrName>style.visibility</p:attrName></p:attrNameLst></p:cBhvr><p:to><p:strVal val="visible"/></p:to></p:set>""" +
                """<p:animEffect transition="in" filter="fade"><p:cBhvr><p:cTn id="${e + 2}" dur="400"/><p:tgtEl><p:spTgt spid="$spid"/></p:tgtEl></p:cBhvr></p:animEffect>""" +
                """</p:childTnLst></p:cTn></p:par>"""
            )
        }
        val builds = spids.joinToString("") { """<p:bldP spid="$it" grpId="0"/>""" }
        return """<p:timing><p:tnLst><p:par><p:cTn id="1" dur="indefinite" restart="never" nodeType="tmRoot"><p:childTnLst><p:seq concurrent="1" nextAc="seek"><p:cTn id="2" dur="indefinite" nodeType="mainSeq"><p:childTnLst><p:par><p:cTn id="3" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst><p:childTnLst><p:par><p:cTn id="4" fill="hold"><p:stCondLst><p:cond delay="0"/></p:stCondLst><p:childTnLst>$effects</p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn></p:par></p:childTnLst></p:cTn><p:prevCondLst><p:cond evt="onPrev" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:prevCondLst><p:nextCondLst><p:cond evt="onNext" delay="0"><p:tgtEl><p:sldTgt/></p:tgtEl></p:cond></p:nextCondLst></p:seq></p:childTnLst></p:cTn></p:par></p:tnLst><p:bldLst>$builds</p:bldLst></p:timing>"""
    }

    private fun slideRels(idx: Int, count: Int) = buildString {
        append("""$XML<Relationships xmlns="$NS_PKG">""")
        append("""<Relationship Id="rId1" Type="$RT/image" Target="../media/image$idx.png"/>""")
        for (k in 0 until count) append("""<Relationship Id="rId${2 + k}" Type="$RT/image" Target="../media/image${idx}_${k + 1}.png"/>""")
        append("""<Relationship Id="rId${2 + count}" Type="$RT/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>""")
        append("</Relationships>")
    }

    private val THEME = """$XML<a:theme xmlns:a="$NS_A" name="Office Theme"><a:themeElements><a:clrScheme name="Office"><a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1><a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="44546A"/></a:dk2><a:lt2><a:srgbClr val="E7E6E6"/></a:lt2><a:accent1><a:srgbClr val="4472C4"/></a:accent1><a:accent2><a:srgbClr val="ED7D31"/></a:accent2><a:accent3><a:srgbClr val="A5A5A5"/></a:accent3><a:accent4><a:srgbClr val="FFC000"/></a:accent4><a:accent5><a:srgbClr val="5B9BD5"/></a:accent5><a:accent6><a:srgbClr val="70AD47"/></a:accent6><a:hlink><a:srgbClr val="0563C1"/></a:hlink><a:folHlink><a:srgbClr val="954F72"/></a:folHlink></a:clrScheme><a:fontScheme name="Office"><a:majorFont><a:latin typeface="Calibri Light"/><a:ea typeface=""/><a:cs typeface=""/></a:majorFont><a:minorFont><a:latin typeface="Calibri"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont></a:fontScheme><a:fmtScheme name="Office"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="6350" cap="flat" cmpd="sng" algn="ctr"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln><a:ln w="12700" cap="flat" cmpd="sng" algn="ctr"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln><a:ln w="19050" cap="flat" cmpd="sng" algn="ctr"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:prstDash val="solid"/></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>"""
}
