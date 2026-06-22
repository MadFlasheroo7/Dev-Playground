package pro.jayeshseth.devcontrols.vectorize

import pro.jayeshseth.devcontrols.ui.PathSegment
import kotlin.math.max

/**
 * A raster-to-vector tracer, written as pure Kotlin (no Compose, no platform APIs) so it is
 * trivially unit-testable and runnable off the main thread. The pipeline:
 *
 *   downsample -> label (luminance threshold OR colour quantize) -> connected components ->
 *   Moore-neighbour contour trace -> Douglas-Peucker simplify -> (optional) Catmull-Rom smoothing
 *   -> emit [PathSegment]s grouped by colour.
 *
 * Coordinates are emitted in the downscaled pixel space, which becomes the SVG viewBox. They are
 * snapped to integers because [pro.jayeshseth.devcontrols.ui.serializeSvgPath] truncates to int —
 * snapping keeps the rendered preview and the generated source text identical.
 */

// ---------------------------------------------------------------------------
// Public data types
// ---------------------------------------------------------------------------

/** Packed ARGB raster (0xAARRGGBB per pixel, row-major). */
class Raster(val width: Int, val height: Int, val argb: IntArray) {
    fun alpha(i: Int): Int = (argb[i] ushr 24) and 0xFF
    fun red(i: Int): Int = (argb[i] ushr 16) and 0xFF
    fun green(i: Int): Int = (argb[i] ushr 8) and 0xFF
    fun blue(i: Int): Int = argb[i] and 0xFF
    fun luminance(i: Int): Float = (0.2126f * red(i) + 0.7152f * green(i) + 0.0722f * blue(i)) / 255f
}

enum class Mode { LUMINANCE, QUANTIZE }

data class VectorizeParams(
    val maxDimension: Int = 256,
    val mode: Mode = Mode.LUMINANCE,
    val threshold: Float = 0.5f,   // luminance cutoff (dark pixels become the traced shape)
    val colorCount: Int = 4,       // palette size in QUANTIZE mode
    val epsilon: Float = 1.5f,     // Douglas-Peucker tolerance in px
    val smoothing: Boolean = false,
    val minRegionArea: Int = 16,   // drop specks smaller than this many px
    val alphaCutoff: Float = 0.5f, // pixels below this alpha are background
)

/** One filled colour group: all contours that share a fill, concatenated into one segment list. */
data class PathGroup(val fillArgb: Long, val segments: List<PathSegment>, val area: Int)

data class ConversionStats(
    val srcW: Int,
    val srcH: Int,
    val scaledW: Int,
    val scaledH: Int,
    val regionCount: Int,
    val rawVertexCount: Int,
    val simplifiedVertexCount: Int,
    val totalSegments: Int,
    val elapsedMs: Long,
)

data class VectorResult(
    val viewBoxW: Int,
    val viewBoxH: Int,
    val groups: List<PathGroup>,
    val stats: ConversionStats,
)

// ---------------------------------------------------------------------------
// Internal scratch types
// ---------------------------------------------------------------------------

private class Pt(val x: Int, val y: Int)
private class PtF(val x: Float, val y: Float)

private class RegionMeta(
    val id: Int,
    val classId: Int,
    val startIdx: Int,
    val area: Int,
    val sumR: Long,
    val sumG: Long,
    val sumB: Long,
)

// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------

fun vectorize(src: Raster, params: VectorizeParams, log: (String) -> Unit, now: () -> Long): VectorResult {
    val t0 = now()
    val raster = downsample(src, params.maxDimension)
    val w = raster.width
    val h = raster.height
    if (raster !== src) log("Downscaled ${src.width}x${src.height} -> ${w}x$h")

    val classOf: IntArray = when (params.mode) {
        Mode.LUMINANCE -> labelLuminance(raster, params).also { log("Luminance threshold ${(params.threshold * 100).toInt()}% -> 1 foreground class") }
        Mode.QUANTIZE -> labelQuantize(raster, params).also { log("Posterized to ${params.colorCount} colours") }
    }

    val (regionId, regions) = connectedComponents(classOf, w, h, raster, params.minRegionArea)
    log("Found ${regions.size} region(s) after dropping specks < ${params.minRegionArea}px")

    // Per-class aggregate colour (so each colour group has one representative fill).
    val classCount = (classOf.maxOrNull() ?: -1) + 1
    val csR = LongArray(classCount)
    val csG = LongArray(classCount)
    val csB = LongArray(classCount)
    val csN = LongArray(classCount)
    for (r in regions) {
        csR[r.classId] += r.sumR; csG[r.classId] += r.sumG; csB[r.classId] += r.sumB; csN[r.classId] += r.area
    }
    val classColor = LongArray(classCount) { c ->
        if (csN[c] == 0L) 0xFF000000L
        else packArgb((csR[c] / csN[c]).toInt(), (csG[c] / csN[c]).toInt(), (csB[c] / csN[c]).toInt())
    }

    var rawV = 0
    var simpV = 0
    data class Shape(val classId: Int, val segments: List<PathSegment>, val area: Int)
    val shapes = ArrayList<Shape>(regions.size)
    for (region in regions) {
        val ring = traceContour(regionId, w, h, region.id, region.startIdx)
        rawV += ring.size
        val simplified = simplifyClosed(ring, params.epsilon)
        simpV += simplified.size
        val segs = ringToSegments(simplified, params.smoothing)
        if (segs.isNotEmpty()) shapes.add(Shape(region.classId, segs, region.area))
    }

    val groups = shapes
        .groupBy { it.classId }
        .map { (cls, list) ->
            PathGroup(
                fillArgb = classColor[cls],
                segments = list.flatMap { it.segments },
                area = list.sumOf { it.area },
            )
        }
        .sortedByDescending { it.area } // paint large/background fills first

    val stats = ConversionStats(
        srcW = src.width, srcH = src.height, scaledW = w, scaledH = h,
        regionCount = regions.size, rawVertexCount = rawV, simplifiedVertexCount = simpV,
        totalSegments = groups.sumOf { it.segments.size }, elapsedMs = now() - t0,
    )
    log("Emitted ${groups.size} colour group(s), ${stats.totalSegments} segments in ${stats.elapsedMs}ms")
    return VectorResult(w, h, groups, stats)
}

// ---------------------------------------------------------------------------
// Stage A — downscale (box average)
// ---------------------------------------------------------------------------

private fun downsample(src: Raster, maxDim: Int): Raster {
    val longest = max(src.width, src.height)
    if (longest <= maxDim) return src
    val scale = maxDim.toFloat() / longest
    val dw = max(1, (src.width * scale).toInt())
    val dh = max(1, (src.height * scale).toInt())
    val out = IntArray(dw * dh)
    for (dy in 0 until dh) {
        val sy0 = dy * src.height / dh
        val sy1 = max(sy0 + 1, (dy + 1) * src.height / dh)
        for (dx in 0 until dw) {
            val sx0 = dx * src.width / dw
            val sx1 = max(sx0 + 1, (dx + 1) * src.width / dw)
            var a = 0L; var r = 0L; var g = 0L; var b = 0L; var n = 0L
            for (sy in sy0 until sy1) {
                val row = sy * src.width
                for (sx in sx0 until sx1) {
                    val i = row + sx
                    a += src.alpha(i); r += src.red(i); g += src.green(i); b += src.blue(i); n++
                }
            }
            out[dy * dw + dx] = ((a / n).toInt() shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
        }
    }
    return Raster(dw, dh, out)
}

// ---------------------------------------------------------------------------
// Stage B — labelling
// ---------------------------------------------------------------------------

/** Two-class: dark pixels (below the luminance threshold) become class 0; everything else is background. */
private fun labelLuminance(raster: Raster, params: VectorizeParams): IntArray {
    val cut = params.alphaCutoff * 255f
    return IntArray(raster.width * raster.height) { i ->
        if (raster.alpha(i) < cut) -1
        else if (raster.luminance(i) < params.threshold) 0
        else -1
    }
}

/** Coarse 4-level/channel histogram -> top-k buckets -> assign each opaque pixel to the nearest. */
private fun labelQuantize(raster: Raster, params: VectorizeParams): IntArray {
    val k = params.colorCount.coerceIn(2, 16)
    val cut = params.alphaCutoff * 255f
    val n = raster.width * raster.height

    val histN = IntArray(64)
    val histR = LongArray(64); val histG = LongArray(64); val histB = LongArray(64)
    fun bucketOf(i: Int) = ((raster.red(i) ushr 6) shl 4) or ((raster.green(i) ushr 6) shl 2) or (raster.blue(i) ushr 6)
    for (i in 0 until n) {
        if (raster.alpha(i) < cut) continue
        val b = bucketOf(i)
        histN[b]++; histR[b] += raster.red(i); histG[b] += raster.green(i); histB[b] += raster.blue(i)
    }
    val chosen = (0 until 64).filter { histN[it] > 0 }.sortedByDescending { histN[it] }.take(k)
    if (chosen.isEmpty()) return IntArray(n) { -1 }
    val pr = IntArray(chosen.size); val pg = IntArray(chosen.size); val pb = IntArray(chosen.size)
    chosen.forEachIndexed { idx, bk ->
        pr[idx] = (histR[bk] / histN[bk]).toInt(); pg[idx] = (histG[bk] / histN[bk]).toInt(); pb[idx] = (histB[bk] / histN[bk]).toInt()
    }
    return IntArray(n) { i ->
        if (raster.alpha(i) < cut) return@IntArray -1
        val r = raster.red(i); val g = raster.green(i); val b = raster.blue(i)
        var best = 0; var bestD = Int.MAX_VALUE
        for (p in chosen.indices) {
            val dr = r - pr[p]; val dg = g - pg[p]; val db = b - pb[p]
            val d = dr * dr + dg * dg + db * db
            if (d < bestD) { bestD = d; best = p }
        }
        best
    }
}

// ---------------------------------------------------------------------------
// Stage B2 — connected components (4-connected flood fill, iterative stack)
// ---------------------------------------------------------------------------

private fun connectedComponents(
    classOf: IntArray, w: Int, h: Int, raster: Raster, minArea: Int,
): Pair<IntArray, List<RegionMeta>> {
    val n = w * h
    val regionId = IntArray(n) { -1 }
    val regions = ArrayList<RegionMeta>()
    val stack = IntArray(n)
    var nextId = 0
    for (start in 0 until n) {
        if (regionId[start] != -1) continue
        val cls = classOf[start]
        if (cls < 0) continue
        val rid = nextId++
        var sp = 0
        stack[sp++] = start
        regionId[start] = rid
        var area = 0; var sr = 0L; var sg = 0L; var sb = 0L
        var minIdx = start
        while (sp > 0) {
            val p = stack[--sp]
            area++; sr += raster.red(p); sg += raster.green(p); sb += raster.blue(p)
            if (p < minIdx) minIdx = p
            val px = p % w; val py = p / w
            if (px > 0) { val q = p - 1; if (regionId[q] == -1 && classOf[q] == cls) { regionId[q] = rid; stack[sp++] = q } }
            if (px < w - 1) { val q = p + 1; if (regionId[q] == -1 && classOf[q] == cls) { regionId[q] = rid; stack[sp++] = q } }
            if (py > 0) { val q = p - w; if (regionId[q] == -1 && classOf[q] == cls) { regionId[q] = rid; stack[sp++] = q } }
            if (py < h - 1) { val q = p + w; if (regionId[q] == -1 && classOf[q] == cls) { regionId[q] = rid; stack[sp++] = q } }
        }
        if (area >= minArea) regions.add(RegionMeta(rid, cls, minIdx, area, sr, sg, sb))
    }
    return regionId to regions
}

// ---------------------------------------------------------------------------
// Stage C — Moore-neighbour contour tracing
// ---------------------------------------------------------------------------

// Clockwise 8-neighbourhood (screen coords, y downward): E, SE, S, SW, W, NW, N, NE.
private val MOORE = arrayOf(
    intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(0, 1), intArrayOf(-1, 1),
    intArrayOf(-1, 0), intArrayOf(-1, -1), intArrayOf(0, -1), intArrayOf(1, -1),
)

private fun traceContour(regionId: IntArray, w: Int, h: Int, id: Int, startIdx: Int): List<Pt> {
    fun inside(x: Int, y: Int): Boolean = x in 0 until w && y in 0 until h && regionId[y * w + x] == id
    val sx = startIdx % w; val sy = startIdx / w
    val out = ArrayList<Pt>()
    out.add(Pt(sx, sy))
    var cx = sx; var cy = sy
    var backtrack = 4 // entered the start pixel from the west (guaranteed background)
    val maxSteps = 4 * w * h + 16
    var steps = 0
    while (steps++ < maxSteps) {
        var found = -1
        for (kk in 1..8) {
            val d = (backtrack + kk) and 7
            if (inside(cx + MOORE[d][0], cy + MOORE[d][1])) { found = d; break }
        }
        if (found == -1) break // isolated pixel
        cx += MOORE[found][0]; cy += MOORE[found][1]
        backtrack = (found + 4) and 7 // direction back to where we came from
        if (cx == sx && cy == sy) break // returned to start: ring closed
        out.add(Pt(cx, cy))
    }
    return out
}

// ---------------------------------------------------------------------------
// Stage D — Douglas-Peucker simplification (closed-ring aware, iterative)
// ---------------------------------------------------------------------------

private fun simplifyClosed(points: List<Pt>, eps: Float): List<PtF> {
    if (points.size < 4) return points.map { PtF(it.x.toFloat(), it.y.toFloat()) }
    // Split the ring at the vertex farthest from points[0] so DP has two genuine open polylines.
    var far = 0; var maxD = -1L
    val p0 = points[0]
    for (i in 1 until points.size) {
        val dx = (points[i].x - p0.x).toLong(); val dy = (points[i].y - p0.y).toLong()
        val d = dx * dx + dy * dy
        if (d > maxD) { maxD = d; far = i }
    }
    val firstHalf = points.subList(0, far + 1)
    val secondHalf = ArrayList<Pt>(points.size - far + 1).apply {
        addAll(points.subList(far, points.size)); add(points[0])
    }
    val a = dpOpen(firstHalf, eps)
    val b = dpOpen(secondHalf, eps)
    // a ends at the split vertex (== b start); b ends at points[0] (== a start). Drop both shared joins.
    val result = ArrayList<PtF>(a.size + b.size)
    result.addAll(a)
    for (i in 1 until b.size - 1) result.add(b[i])
    return result
}

private fun dpOpen(points: List<Pt>, eps: Float): List<PtF> {
    val n = points.size
    if (n < 3) return points.map { PtF(it.x.toFloat(), it.y.toFloat()) }
    val keep = BooleanArray(n)
    keep[0] = true; keep[n - 1] = true
    val stack = ArrayDeque<IntArray>()
    stack.addLast(intArrayOf(0, n - 1))
    val eps2 = eps * eps
    while (stack.isNotEmpty()) {
        val (s, e) = stack.removeLast()
        if (e <= s + 1) continue
        val ax = points[s].x.toFloat(); val ay = points[s].y.toFloat()
        val bx = points[e].x.toFloat(); val by = points[e].y.toFloat()
        val ex = bx - ax; val ey = by - ay
        val len2 = ex * ex + ey * ey
        var idx = -1; var maxDist2 = eps2
        for (m in (s + 1) until e) {
            val px = points[m].x.toFloat(); val py = points[m].y.toFloat()
            val dist2 = if (len2 == 0f) {
                val dx = px - ax; val dy = py - ay; dx * dx + dy * dy
            } else {
                val cross = (px - ax) * ey - (py - ay) * ex
                cross * cross / len2
            }
            if (dist2 > maxDist2) { maxDist2 = dist2; idx = m }
        }
        if (idx != -1) {
            keep[idx] = true
            stack.addLast(intArrayOf(s, idx)); stack.addLast(intArrayOf(idx, e))
        }
    }
    val res = ArrayList<PtF>()
    for (i in 0 until n) if (keep[i]) res.add(PtF(points[i].x.toFloat(), points[i].y.toFloat()))
    return res
}

// ---------------------------------------------------------------------------
// Stages E/F — emit PathSegments (straight or Catmull-Rom -> cubic Bezier)
// ---------------------------------------------------------------------------

private fun snap(v: Float): Float = (v + 0.5f).toInt().toFloat()

private fun ringToSegments(points: List<PtF>, smoothing: Boolean): List<PathSegment> {
    if (points.size < 3) return emptyList()
    val segs = ArrayList<PathSegment>(points.size + 2)
    if (!smoothing) {
        segs.add(PathSegment.MoveTo(snap(points[0].x), snap(points[0].y)))
        for (i in 1 until points.size) segs.add(PathSegment.LineTo(snap(points[i].x), snap(points[i].y)))
        segs.add(PathSegment.Close)
    } else {
        val n = points.size
        fun p(i: Int): PtF = points[((i % n) + n) % n]
        segs.add(PathSegment.MoveTo(snap(p(0).x), snap(p(0).y)))
        for (i in 0 until n) {
            val p0 = p(i - 1); val p1 = p(i); val p2 = p(i + 1); val p3 = p(i + 2)
            val c1x = p1.x + (p2.x - p0.x) / 6f
            val c1y = p1.y + (p2.y - p0.y) / 6f
            val c2x = p2.x - (p3.x - p1.x) / 6f
            val c2y = p2.y - (p3.y - p1.y) / 6f
            segs.add(PathSegment.CubicTo(snap(c1x), snap(c1y), snap(c2x), snap(c2y), snap(p2.x), snap(p2.y)))
        }
        segs.add(PathSegment.Close)
    }
    return segs
}

// ---------------------------------------------------------------------------
// helpers
// ---------------------------------------------------------------------------

private fun packArgb(r: Int, g: Int, b: Int): Long =
    0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
