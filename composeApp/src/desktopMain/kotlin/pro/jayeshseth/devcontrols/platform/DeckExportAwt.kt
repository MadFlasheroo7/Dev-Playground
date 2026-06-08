package pro.jayeshseth.devcontrols.platform

import pro.jayeshseth.devcontrols.ui.present.Deck
import pro.jayeshseth.devcontrols.ui.present.ElementType
import pro.jayeshseth.devcontrols.ui.present.ShapeKind
import pro.jayeshseth.devcontrols.ui.present.Slide
import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/**
 * Desktop (JVM/AWT) deck renderer + exporters. Renders each slide to a [BufferedImage] with
 * Graphics2D (gradient / text / shapes / bullets), then produces PNG bytes, a hand-built
 * image-per-slide PPTX (OOXML zip), or a minimal image-per-page PDF (JPEG XObjects).
 */
object DeckExportAwt {
    private const val W = 1920
    private const val H = 1080

    fun renderSlide(slide: Slide): BufferedImage {
        val img = BufferedImage(W, H, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val cols = slide.bgColors.map { Color(it.toInt() and 0xFFFFFF) }.let { if (it.size >= 2) it else it + it }
        g.paint = GradientPaint(0f, 0f, cols.first(), W.toFloat(), H.toFloat(), cols[1])
        g.fillRect(0, 0, W, H)

        val fontScale = H / 720f
        slide.elements.forEach { el ->
            val left = el.x * W; val top = el.y * H; val ew = el.w * W; val eh = el.h * H
            val color = Color(el.colorArgb.toInt() and 0xFFFFFF)
            when (el.type) {
                ElementType.SHAPE -> {
                    g.color = color
                    if (el.shape == ShapeKind.CIRCLE) g.fillOval(left.toInt(), top.toInt(), ew.toInt(), eh.toInt())
                    else g.fillRoundRect(left.toInt(), top.toInt(), ew.toInt(), eh.toInt(), (18 * fontScale).toInt(), (18 * fontScale).toInt())
                }
                ElementType.BULLETS -> {
                    g.color = color
                    g.font = Font(Font.SANS_SERIF, if (el.weight >= 600) Font.BOLD else Font.PLAIN, (el.fontSize * fontScale).roundToInt())
                    val fm = g.fontMetrics
                    var y = top.toInt() + fm.ascent
                    el.bullets.forEach { b -> g.drawString("•  $b", left.toInt(), y); y += (fm.height * 1.2f).toInt() }
                }
                else -> {
                    g.color = color
                    g.font = Font(Font.SANS_SERIF, if (el.weight >= 600) Font.BOLD else Font.PLAIN, (el.fontSize * fontScale).roundToInt())
                    drawWrapped(g, el.text ?: "", left, top, ew, eh, el.centerAlign)
                }
            }
        }
        g.dispose()
        return img
    }

    private fun drawWrapped(g: java.awt.Graphics2D, text: String, left: Float, top: Float, ew: Float, eh: Float, center: Boolean) {
        val fm = g.fontMetrics
        val lines = ArrayList<String>()
        var line = StringBuilder()
        for (word in text.split(" ")) {
            val trial = if (line.isEmpty()) word else "$line $word"
            if (fm.stringWidth(trial) > ew && line.isNotEmpty()) { lines.add(line.toString()); line = StringBuilder(word) }
            else line = StringBuilder(trial)
        }
        if (line.isNotEmpty()) lines.add(line.toString())
        val totalH = lines.size * fm.height
        var y = (top + (eh - totalH) / 2).toInt() + fm.ascent
        for (l in lines) {
            val x = if (center) (left + (ew - fm.stringWidth(l)) / 2).toInt() else left.toInt()
            g.drawString(l, x, y); y += fm.height
        }
    }

    fun png(slide: Slide): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(renderSlide(slide), "png", it) }.toByteArray()

    private fun jpeg(img: BufferedImage): ByteArray =
        ByteArrayOutputStream().also { ImageIO.write(img, "jpg", it) }.toByteArray()

    // ---- PDF: minimal image-per-page (JPEG XObjects) ----
    fun exportPdf(deck: Deck): ByteArray {
        val out = ByteArrayOutputStream()
        val offsets = ArrayList<Int>()
        fun w(s: String) = out.write(s.toByteArray(Charsets.ISO_8859_1))
        fun obj(n: Int) { offsets.add(out.size()); w("$n 0 obj\n") }
        w("%PDF-1.4\n")
        val n = deck.slides.size
        // 1 catalog, 2 pages, then per slide: page(3+i*3), content(4+i*3), image(5+i*3)
        obj(1); w("<</Type/Catalog/Pages 2 0 R>>\nendobj\n")
        val kids = (0 until n).joinToString(" ") { "${3 + it * 3} 0 R" }
        obj(2); w("<</Type/Pages/Kids[$kids]/Count $n>>\nendobj\n")
        deck.slides.forEachIndexed { i, s ->
            val pageN = 3 + i * 3; val contN = 4 + i * 3; val imgN = 5 + i * 3
            val jpg = jpeg(renderSlide(s))
            val content = "q\n$W 0 0 $H 0 0 cm\n/Im0 Do\nQ\n"
            obj(pageN); w("<</Type/Page/Parent 2 0 R/MediaBox[0 0 $W $H]/Resources<</XObject<</Im0 $imgN 0 R>>>>/Contents $contN 0 R>>\nendobj\n")
            obj(contN); w("<</Length ${content.length}>>\nstream\n$content\nendstream\nendobj\n")
            obj(imgN); w("<</Type/XObject/Subtype/Image/Width $W/Height $H/ColorSpace/DeviceRGB/BitsPerComponent 8/Filter/DCTDecode/Length ${jpg.size}>>\nstream\n")
            out.write(jpg); w("\nendstream\nendobj\n")
        }
        val xrefPos = out.size()
        val count = offsets.size + 1
        w("xref\n0 $count\n")
        w("0000000000 65535 f \n")
        for (off in offsets) w(off.toString().padStart(10, '0') + " 00000 n \n")
        w("trailer\n<</Size $count/Root 1 0 R>>\nstartxref\n$xrefPos\n%%EOF")
        return out.toByteArray()
    }

    // ---- PPTX: image-per-slide OOXML zip ----
    fun exportPptx(deck: Deck): ByteArray {
        val n = deck.slides.size
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            fun put(path: String, bytes: ByteArray) { zip.putNextEntry(ZipEntry(path)); zip.write(bytes); zip.closeEntry() }
            fun putS(path: String, s: String) = put(path, s.toByteArray())
            val A = "http://schemas.openxmlformats.org/drawingml/2006/main"
            val R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            val P = "http://schemas.openxmlformats.org/presentationml/2006/main"
            val PKG = "http://schemas.openxmlformats.org/package/2006/relationships"
            val RT = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            val EW = 12192000; val EH = 6858000
            val ctypes = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="png" ContentType="image/png"/><Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/><Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/><Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/><Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>""")
            for (i in 1..n) ctypes.append("""<Override PartName="/ppt/slides/slide$i.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>""")
            ctypes.append("</Types>")
            putS("[Content_Types].xml", ctypes.toString())
            putS("_rels/.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="$PKG"><Relationship Id="rId1" Type="$RT/officeDocument" Target="ppt/presentation.xml"/></Relationships>""")
            val sldIds = (1..n).joinToString("") { """<p:sldId id="${255 + it}" r:id="rId$it"/>""" }
            putS("ppt/presentation.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:presentation xmlns:a="$A" xmlns:r="$R" xmlns:p="$P"><p:sldMasterIdLst><p:sldMasterId id="2147483648" r:id="rIdM"/></p:sldMasterIdLst><p:sldIdLst>$sldIds</p:sldIdLst><p:sldSz cx="$EW" cy="$EH" type="screen16x9"/><p:notesSz cx="6858000" cy="9144000"/></p:presentation>""")
            val presRels = StringBuilder("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="$PKG">""")
            for (i in 1..n) presRels.append("""<Relationship Id="rId$i" Type="$RT/slide" Target="slides/slide$i.xml"/>""")
            presRels.append("""<Relationship Id="rIdM" Type="$RT/slideMaster" Target="slideMasters/slideMaster1.xml"/><Relationship Id="rIdT" Type="$RT/theme" Target="theme/theme1.xml"/></Relationships>""")
            putS("ppt/_rels/presentation.xml.rels", presRels.toString())
            putS("ppt/slideMasters/slideMaster1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sldMaster xmlns:a="$A" xmlns:r="$R" xmlns:p="$P"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld><p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" accent4="accent4" accent5="accent5" accent6="accent6" hlink="hlink" folHlink="folHlink"/><p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rId1"/></p:sldLayoutIdLst></p:sldMaster>""")
            putS("ppt/slideMasters/_rels/slideMaster1.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="$PKG"><Relationship Id="rId1" Type="$RT/slideLayout" Target="../slideLayouts/slideLayout1.xml"/><Relationship Id="rId2" Type="$RT/theme" Target="../theme/theme1.xml"/></Relationships>""")
            putS("ppt/slideLayouts/slideLayout1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sldLayout xmlns:a="$A" xmlns:r="$R" xmlns:p="$P" type="blank" preserve="1"><p:cSld name="Blank"><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sldLayout>""")
            putS("ppt/slideLayouts/_rels/slideLayout1.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="$PKG"><Relationship Id="rId1" Type="$RT/slideMaster" Target="../slideMasters/slideMaster1.xml"/></Relationships>""")
            putS("ppt/theme/theme1.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><a:theme xmlns:a="$A" name="Office Theme"><a:themeElements><a:clrScheme name="Office"><a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1><a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1><a:dk2><a:srgbClr val="44546A"/></a:dk2><a:lt2><a:srgbClr val="E7E6E6"/></a:lt2><a:accent1><a:srgbClr val="4472C4"/></a:accent1><a:accent2><a:srgbClr val="ED7D31"/></a:accent2><a:accent3><a:srgbClr val="A5A5A5"/></a:accent3><a:accent4><a:srgbClr val="FFC000"/></a:accent4><a:accent5><a:srgbClr val="5B9BD5"/></a:accent5><a:accent6><a:srgbClr val="70AD47"/></a:accent6><a:hlink><a:srgbClr val="0563C1"/></a:hlink><a:folHlink><a:srgbClr val="954F72"/></a:folHlink></a:clrScheme><a:fontScheme name="Office"><a:majorFont><a:latin typeface="Calibri Light"/><a:ea typeface=""/><a:cs typeface=""/></a:majorFont><a:minorFont><a:latin typeface="Calibri"/><a:ea typeface=""/><a:cs typeface=""/></a:minorFont></a:fontScheme><a:fmtScheme name="Office"><a:fillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:fillStyleLst><a:lnStyleLst><a:ln w="6350"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln><a:ln w="12700"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln><a:ln w="19050"><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:ln></a:lnStyleLst><a:effectStyleLst><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle><a:effectStyle><a:effectLst/></a:effectStyle></a:effectStyleLst><a:bgFillStyleLst><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill><a:solidFill><a:schemeClr val="phClr"/></a:solidFill></a:bgFillStyleLst></a:fmtScheme></a:themeElements></a:theme>""")
            deck.slides.forEachIndexed { i, s ->
                val idx = i + 1
                put("ppt/media/image$idx.png", ByteArrayOutputStream().also { ImageIO.write(renderSlide(s), "png", it) }.toByteArray())
                putS("ppt/slides/slide$idx.xml", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><p:sld xmlns:a="$A" xmlns:r="$R" xmlns:p="$P"><p:cSld><p:spTree><p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr><p:grpSpPr/><p:pic><p:nvPicPr><p:cNvPr id="2" name="bg"/><p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr><p:nvPr/></p:nvPicPr><p:blipFill><a:blip r:embed="rId1"/><a:stretch><a:fillRect/></a:stretch></p:blipFill><p:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$EW" cy="$EH"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></p:spPr></p:pic></p:spTree></p:cSld><p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr></p:sld>""")
                putS("ppt/slides/_rels/slide$idx.xml.rels", """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="$PKG"><Relationship Id="rId1" Type="$RT/image" Target="../media/image$idx.png"/><Relationship Id="rId2" Type="$RT/slideLayout" Target="../slideLayouts/slideLayout1.xml"/></Relationships>""")
            }
        }
        return baos.toByteArray()
    }
}
