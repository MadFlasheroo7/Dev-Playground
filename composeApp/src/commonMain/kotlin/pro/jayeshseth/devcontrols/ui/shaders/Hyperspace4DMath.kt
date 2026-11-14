package pro.jayeshseth.devcontrols.ui.shaders

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A point or vector in 4-dimensional Euclidean space R^4.
 */
data class Vec4(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
) {
    operator fun plus(other: Vec4) = Vec4(x + other.x, y + other.y, z + other.z, w + other.w)
    operator fun minus(other: Vec4) = Vec4(x - other.x, y - other.y, z - other.z, w - other.w)
    operator fun times(scalar: Float) = Vec4(x * scalar, y * scalar, z * scalar, w * scalar)
    operator fun div(scalar: Float) = Vec4(x / scalar, y / scalar, z / scalar, w / scalar)

    fun lengthSquared(): Float = x * x + y * y + z * z + w * w
    fun length(): Float = sqrt(lengthSquared())

    fun normalized(): Vec4 {
        val len = length()
        return if (len > 1e-6f) this / len else this
    }

    fun toVec3(): Vec3 = Vec3(x, y, z)
}

/**
 * A 3D vector.
 */
data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3(x / scalar, y / scalar, z / scalar)

    fun length(): Float = sqrt(x * x + y * y + z * z)
}

/**
 * An undirected edge connecting two 4D vertices.
 */
data class Edge4D(val startIndex: Int, val endIndex: Int)

/**
 * 4D Polytope Mesh definition.
 */
data class Polytope4D(
    val name: String,
    val subtitle: String,
    val description: String,
    val vertexCountLabel: String,
    val edgeCountLabel: String,
    val cellCountLabel: String,
    val eulerChar: Int,
    val vertices: List<Vec4>,
    val edges: List<Edge4D>
)

/**
 * Projection types from 4D to 3D.
 */
enum class ProjectionMode(val displayName: String, val description: String) {
    PERSPECTIVE("4D Perspective", "Camera at w = d4. Inner cells expand through outer cells as it rotates in 4D."),
    STEREOGRAPHIC("Stereographic (S³)", "Conformal projection from unit 3-sphere to 3D space. Preserves circular paths."),
    ORTHOGRAPHIC("Orthographic (Shadow)", "Direct parallel projection onto 3D hyperplane (drops w coordinate)."),
    CROSS_SECTION("4D Slice (Hyperplane)", "Intersects the 4D object with a 3D hyperplane at variable depth w0.")
}

/**
 * Color palettes for 4th-dimension depth and energy rendering.
 */
enum class HyperspacePalette(
    val displayName: String,
    val wFarColor: Color,      // Low -w (receding into 4th dimension)
    val wMidColor: Color,      // w ≈ 0 (3D hyperplane)
    val wNearColor: Color,     // High +w (approaching 4D observer)
    val vertexCoreColor: Color,
    val backgroundColor: Color
) {
    COSMIC_WARP(
        displayName = "Cosmic Warp",
        wFarColor = Color(0xFFE11D48),    // Crimson Rose
        wMidColor = Color(0xFF9333EA),    // Electric Violet
        wNearColor = Color(0xFF06B6D4),   // Cyan Flare
        vertexCoreColor = Color(0xFFFFFFFF),
        backgroundColor = Color(0xFF05030A)
    ),
    QUANTUM_MATRIX(
        displayName = "Matrix Emerald",
        wFarColor = Color(0xFF064E3B),    // Deep Pine
        wMidColor = Color(0xFF10B981),    // Jade Green
        wNearColor = Color(0xFFA7F3D0),   // Neon Mint
        vertexCoreColor = Color(0xFFF0FDF4),
        backgroundColor = Color(0xFF020904)
    ),
    SOLAR_PLASMA(
        displayName = "Solar Plasma",
        wFarColor = Color(0xFF7F1D1D),    // Dark Maroon
        wMidColor = Color(0xFFF97316),    // Flare Orange
        wNearColor = Color(0xFFFDE047),   // Radiant Gold
        vertexCoreColor = Color(0xFFFFFBEB),
        backgroundColor = Color(0xFF0C0402)
    ),
    NEON_AURORA(
        displayName = "Neon Aurora",
        wFarColor = Color(0xFF3B82F6),    // Deep Azure
        wMidColor = Color(0xFFEC4899),    // Hot Pink
        wNearColor = Color(0xFF8B5CF6),   // Lavender Purple
        vertexCoreColor = Color(0xFFFFFFFF),
        backgroundColor = Color(0xFF04030B)
    ),
    BLUEPRINT(
        displayName = "Architect Blueprint",
        wFarColor = Color(0xFF1E3A8A),    // Navy
        wMidColor = Color(0xFF38BDF8),    // Light Cyan
        wNearColor = Color(0xFFBAE6FD),   // Ice Blue
        vertexCoreColor = Color(0xFFF8FAFC),
        backgroundColor = Color(0xFF030712)
    )
}

/**
 * 4D Hyperspace geometry generator & mathematical transformation engine.
 */
object Hyperspace4DMath {

    // -------------------------------------------------------------------------
    // POLYTOPE GENERATORS
    // -------------------------------------------------------------------------

    /**
     * Tesseract (8-Cell / Hypercube)
     * 16 vertices, 32 edges, 24 square faces, 8 cubic cells.
     */
    fun createTesseract(scale: Float = 1.0f): Polytope4D {
        val s = scale * 0.9f
        val verts = ArrayList<Vec4>(16)
        for (x in listOf(-s, s)) {
            for (y in listOf(-s, s)) {
                for (z in listOf(-s, s)) {
                    for (w in listOf(-s, s)) {
                        verts.add(Vec4(x, y, z, w))
                    }
                }
            }
        }

        // Edges: vertices differing in exactly one coordinate
        val edges = ArrayList<Edge4D>()
        for (i in 0 until 16) {
            for (j in (i + 1) until 16) {
                var diffCount = 0
                val v1 = verts[i]
                val v2 = verts[j]
                if (kotlin.math.abs(v1.x - v2.x) > 0.01f) diffCount++
                if (kotlin.math.abs(v1.y - v2.y) > 0.01f) diffCount++
                if (kotlin.math.abs(v1.z - v2.z) > 0.01f) diffCount++
                if (kotlin.math.abs(v1.w - v2.w) > 0.01f) diffCount++
                if (diffCount == 1) {
                    edges.add(Edge4D(i, j))
                }
            }
        }

        return Polytope4D(
            name = "Tesseract (8-Cell)",
            subtitle = "Hypercube • 4D Orthotope",
            description = "The 4D analogue of a cube. Bound by 8 cubic cells. As it rotates in 4D, inner cubes invert through the outer cells.",
            vertexCountLabel = "16",
            edgeCountLabel = "32",
            cellCountLabel = "8 Cubes",
            eulerChar = 0,
            vertices = verts,
            edges = edges
        )
    }

    /**
     * 16-Cell (Hexadecachoron / Hyperoctahedron)
     * 8 vertices, 24 edges, 32 triangular faces, 16 tetrahedral cells. Dual of Tesseract.
     */
    fun create16Cell(scale: Float = 1.35f): Polytope4D {
        val s = scale * 1.15f
        val verts = listOf(
            Vec4(s, 0f, 0f, 0f),
            Vec4(-s, 0f, 0f, 0f),
            Vec4(0f, s, 0f, 0f),
            Vec4(0f, -s, 0f, 0f),
            Vec4(0f, 0f, s, 0f),
            Vec4(0f, 0f, -s, 0f),
            Vec4(0f, 0f, 0f, s),
            Vec4(0f, 0f, 0f, -s)
        )

        // Connect all pairs of vertices except opposite antipodal points
        val edges = ArrayList<Edge4D>()
        for (i in 0 until 8) {
            for (j in (i + 1) until 8) {
                // If not antipodal pair (e.g. 0 and 1, 2 and 3, 4 and 5, 6 and 7)
                if (i / 2 != j / 2 || i % 2 == j % 2) {
                    edges.add(Edge4D(i, j))
                }
            }
        }

        return Polytope4D(
            name = "16-Cell (Hyperoctahedron)",
            subtitle = "Hexadecachoron • Dual of Tesseract",
            description = "The regular cross-polytope of 4 dimensions. Bound by 16 regular tetrahedra. Extremely sharp, crystalline geometry.",
            vertexCountLabel = "8",
            edgeCountLabel = "24",
            cellCountLabel = "16 Tetrahedra",
            eulerChar = 0,
            vertices = verts,
            edges = edges
        )
    }

    /**
     * 24-Cell (Icositetrachoron)
     * 24 vertices, 96 edges, 96 triangular faces, 24 octahedral cells.
     * UNIQUE to 4D space — has NO analogue in 3D or any other dimension!
     */
    fun create24Cell(scale: Float = 1.0f): Polytope4D {
        val s = scale * 0.95f
        val verts = ArrayList<Vec4>(24)

        // 24 vertices: all permutations of (±s, ±s, 0, 0)
        val signs = listOf(-s, s)
        val pairs = listOf(
            Pair(0, 1), Pair(0, 2), Pair(0, 3),
            Pair(1, 2), Pair(1, 3), Pair(2, 3)
        )

        for ((idx1, idx2) in pairs) {
            for (s1 in signs) {
                for (s2 in signs) {
                    val coords = FloatArray(4) { 0f }
                    coords[idx1] = s1
                    coords[idx2] = s2
                    verts.add(Vec4(coords[0], coords[1], coords[2], coords[3]))
                }
            }
        }

        // Two vertices are connected by an edge if Euclidean distance squared == 2 * s^2
        val targetDistSq = 2f * s * s
        val edges = ArrayList<Edge4D>()
        for (i in 0 until 24) {
            for (j in (i + 1) until 24) {
                val dx = verts[i].x - verts[j].x
                val dy = verts[i].y - verts[j].y
                val dz = verts[i].z - verts[j].z
                val dw = verts[i].w - verts[j].w
                val distSq = dx * dx + dy * dy + dz * dz + dw * dw
                if (kotlin.math.abs(distSq - targetDistSq) < 0.05f * targetDistSq) {
                    edges.add(Edge4D(i, j))
                }
            }
        }

        return Polytope4D(
            name = "24-Cell (Icositetrachoron)",
            subtitle = "Exclusive 4D Polytope • Self-Dual",
            description = "Unique to 4 dimensions with no 3D counterpart! Self-dual with 24 octahedral cells, exhibiting maximal 4D hyper-symmetry.",
            vertexCountLabel = "24",
            edgeCountLabel = "96",
            cellCountLabel = "24 Octahedra",
            eulerChar = 0,
            vertices = verts,
            edges = edges
        )
    }

    /**
     * 5-Cell (4-Simplex / Hypertetrahedron)
     * 5 vertices, 10 edges, 10 triangular faces, 5 tetrahedral cells.
     */
    fun create5Cell(scale: Float = 1.35f): Polytope4D {
        val s = scale * 1.15f
        val sqrt5 = sqrt(5f)
        val verts = listOf(
            Vec4(0f, 0f, 0f, 1f) * s,
            Vec4(sqrt(8f) / 3f * s, 0f, -1f / 3f * s, -1f / sqrt5 * s),
            Vec4(-sqrt(2f) / 3f * s, sqrt(2f / 3f) * s, -1f / 3f * s, -1f / sqrt5 * s),
            Vec4(-sqrt(2f) / 3f * s, -sqrt(2f / 3f) * s, -1f / 3f * s, -1f / sqrt5 * s),
            Vec4(0f, 0f, 1f * s, -1f / sqrt5 * s)
        )

        // Complete graph K5: all 5 vertices connected to all other vertices
        val edges = ArrayList<Edge4D>()
        for (i in 0 until 5) {
            for (j in (i + 1) until 5) {
                edges.add(Edge4D(i, j))
            }
        }

        return Polytope4D(
            name = "5-Cell (Hypertetrahedron)",
            subtitle = "4-Simplex • Fundamental 4D Shape",
            description = "The simplest regular 4-polytope. Composed of 5 tetrahedral cells intersecting along 10 triangular faces.",
            vertexCountLabel = "5",
            edgeCountLabel = "10",
            cellCountLabel = "5 Tetrahedra",
            eulerChar = 0,
            vertices = verts,
            edges = edges
        )
    }

    /**
     * Clifford Torus (Flat Torus in S^3)
     * Parameterized surface in R^4 projected to nested 3D Villarceau circles.
     */
    fun createCliffordTorus(scale: Float = 1.25f, uSteps: Int = 18, vSteps: Int = 18): Polytope4D {
        val s = scale * 1.0f
        val verts = ArrayList<Vec4>(uSteps * vSteps)
        val invSqrt2 = 1f / sqrt(2f)

        for (i in 0 until uSteps) {
            val u = (i.toFloat() / uSteps) * 2f * PI.toFloat()
            for (j in 0 until vSteps) {
                val v = (j.toFloat() / vSteps) * 2f * PI.toFloat()
                // Flat torus embedded in S^3
                val x = invSqrt2 * cos(u) * s
                val y = invSqrt2 * sin(u) * s
                val z = invSqrt2 * cos(v) * s
                val w = invSqrt2 * sin(v) * s
                verts.add(Vec4(x, y, z, w))
            }
        }

        val edges = ArrayList<Edge4D>()
        for (i in 0 until uSteps) {
            val nextI = (i + 1) % uSteps
            for (j in 0 until vSteps) {
                val nextJ = (j + 1) % vSteps
                val curr = i * vSteps + j
                val right = i * vSteps + nextJ
                val down = nextI * vSteps + j

                edges.add(Edge4D(curr, right))
                edges.add(Edge4D(curr, down))
            }
        }

        return Polytope4D(
            name = "Clifford Torus",
            subtitle = "Flat Torus in S³ • Hopf Fibration",
            description = "A flat 2D torus embedded in the 3-sphere S³. Stereographic projection renders it as breathing, interlocking Villarceau tori.",
            vertexCountLabel = "${verts.size}",
            edgeCountLabel = "${edges.size}",
            cellCountLabel = "S³ Manifold",
            eulerChar = 0,
            vertices = verts,
            edges = edges
        )
    }

    /**
     * Hypersphere (S^3) Geodesic Rings
     * Meridian and latitude rings embedded in 4-space.
     */
    fun createHypersphereLattice(scale: Float = 1.2f): Polytope4D {
        val s = scale * 1.0f
        val verts = ArrayList<Vec4>()
        val edges = ArrayList<Edge4D>()

        val numRings = 7
        val pointsPerRing = 16

        for (r in 0 until numRings) {
            val psi = ((r.toFloat() / (numRings - 1)) - 0.5f) * PI.toFloat() * 0.85f
            val ringRadius = cos(psi) * s
            val w = sin(psi) * s

            val startIndex = verts.size
            for (p in 0 until pointsPerRing) {
                val angle = (p.toFloat() / pointsPerRing) * 2f * PI.toFloat()
                val x = ringRadius * cos(angle)
                val y = ringRadius * sin(angle)
                val z = 0f
                verts.add(Vec4(x, y, z, w))

                val nextP = (p + 1) % pointsPerRing
                edges.add(Edge4D(startIndex + p, startIndex + nextP))
            }

            // Also create orthogonal meridian rings
            val startOrthIndex = verts.size
            for (p in 0 until pointsPerRing) {
                val angle = (p.toFloat() / pointsPerRing) * 2f * PI.toFloat()
                val x = 0f
                val y = ringRadius * sin(angle)
                val z = ringRadius * cos(angle)
                verts.add(Vec4(x, y, z, w))

                val nextP = (p + 1) % pointsPerRing
                edges.add(Edge4D(startOrthIndex + p, startOrthIndex + nextP))
            }
        }

        return Polytope4D(
            name = "Hypersphere Lattice (S³)",
            subtitle = "3-Sphere Geodesic Manifold",
            description = "Concentric 4D hypersphere latitude rings. As w rotates, 3D cross-sections swell from a point to a great sphere and contract.",
            vertexCountLabel = "${verts.size}",
            edgeCountLabel = "${edges.size}",
            cellCountLabel = "Curved S³",
            eulerChar = 0,
            vertices = verts,
            edges = edges
        )
    }

    // -------------------------------------------------------------------------
    // SO(4) ROTATION ENGINE (6 INDEPENDENT PLANES)
    // -------------------------------------------------------------------------

    /**
     * Rotates a 4D vertex in the XY plane (3D Roll).
     */
    fun rotateXY(v: Vec4, angle: Float): Vec4 {
        val c = cos(angle); val s = sin(angle)
        return Vec4(v.x * c - v.y * s, v.x * s + v.y * c, v.z, v.w)
    }

    /**
     * Rotates a 4D vertex in the YZ plane (3D Pitch).
     */
    fun rotateYZ(v: Vec4, angle: Float): Vec4 {
        val c = cos(angle); val s = sin(angle)
        return Vec4(v.x, v.y * c - v.z * s, v.y * s + v.z * c, v.w)
    }

    /**
     * Rotates a 4D vertex in the ZX plane (3D Yaw).
     */
    fun rotateZX(v: Vec4, angle: Float): Vec4 {
        val c = cos(angle); val s = sin(angle)
        return Vec4(v.x * c + v.z * s, v.y, -v.x * s + v.z * c, v.w)
    }

    /**
     * Rotates a 4D vertex in the XW plane (4D Hyperspace Roll).
     * Causes inner structures to expand into outer structures!
     */
    fun rotateXW(v: Vec4, angle: Float): Vec4 {
        val c = cos(angle); val s = sin(angle)
        return Vec4(v.x * c - v.w * s, v.y, v.z, v.x * s + v.w * c)
    }

    /**
     * Rotates a 4D vertex in the YW plane (4D Hyperspace Pitch).
     */
    fun rotateYW(v: Vec4, angle: Float): Vec4 {
        val c = cos(angle); val s = sin(angle)
        return Vec4(v.x, v.y * c - v.w * s, v.z, v.y * s + v.w * c)
    }

    /**
     * Rotates a 4D vertex in the ZW plane (4D Hyperspace Yaw).
     */
    fun rotateZW(v: Vec4, angle: Float): Vec4 {
        val c = cos(angle); val s = sin(angle)
        return Vec4(v.x, v.y, v.z * c - v.w * s, v.z * s + v.w * c)
    }

    /**
     * Applies full SO(4) rotation across all 6 planar degrees of freedom.
     */
    fun rotateSO4(
        v: Vec4,
        angleXY: Float,
        angleYZ: Float,
        angleZX: Float,
        angleXW: Float,
        angleYW: Float,
        angleZW: Float
    ): Vec4 {
        var res = v
        if (angleXY != 0f) res = rotateXY(res, angleXY)
        if (angleYZ != 0f) res = rotateYZ(res, angleYZ)
        if (angleZX != 0f) res = rotateZX(res, angleZX)
        if (angleXW != 0f) res = rotateXW(res, angleXW)
        if (angleYW != 0f) res = rotateYW(res, angleYW)
        if (angleZW != 0f) res = rotateZW(res, angleZW)
        return res
    }

    // -------------------------------------------------------------------------
    // PROJECTION ALGORITHMS (4D -> 3D -> 2D SCREEN)
    // -------------------------------------------------------------------------

    /**
     * Projects a 4D point to 3D space based on the selected ProjectionMode.
     */
    fun project4DTo3D(
        v: Vec4,
        mode: ProjectionMode,
        d4: Float = 2.4f
    ): Vec3 {
        return when (mode) {
            ProjectionMode.PERSPECTIVE -> {
                val denom = (d4 - v.w).coerceAtLeast(0.15f)
                Vec3(v.x / denom, v.y / denom, v.z / denom)
            }
            ProjectionMode.STEREOGRAPHIC -> {
                val denom = (1.5f - v.w * 0.7f).coerceAtLeast(0.15f)
                Vec3(v.x / denom, v.y / denom, v.z / denom)
            }
            ProjectionMode.ORTHOGRAPHIC, ProjectionMode.CROSS_SECTION -> {
                Vec3(v.x, v.y, v.z)
            }
        }
    }

    /**
     * Projects a 3D point to 2D screen coordinates with perspective camera.
     */
    fun project3DToScreen(
        v: Vec3,
        viewportSize: Size,
        d3: Float = 3.5f,
        zoomScale: Float = 1.0f
    ): Offset {
        val cx = viewportSize.width / 2f
        val cy = viewportSize.height / 2f
        val baseScale = kotlin.math.min(cx, cy) * 1.55f * zoomScale
        val zDist = (d3 - v.z).coerceAtLeast(0.2f)

        val sx = cx + (v.x / zDist) * baseScale
        val sy = cy - (v.y / zDist) * baseScale // Invert y for standard Cartesian orientation
        return Offset(sx, sy)
    }

    /**
     * Slices an edge with a 3D hyperplane w = w0.
     * Returns the 3D intersection point if the edge intersects the slice, or null otherwise.
     */
    fun sliceEdgeAtW(v1: Vec4, v2: Vec4, w0: Float): Vec3? {
        val diff1 = v1.w - w0
        val diff2 = v2.w - w0
        // Intersects if vertices lie on opposite sides of the hyperplane
        if (diff1 * diff2 <= 0f) {
            val span = v2.w - v1.w
            val t = if (kotlin.math.abs(span) > 1e-5f) (w0 - v1.w) / span else 0.5f
            val clampedT = t.coerceIn(0f, 1f)
            val ix = v1.x + clampedT * (v2.x - v1.x)
            val iy = v1.y + clampedT * (v2.y - v1.y)
            val iz = v1.z + clampedT * (v2.z - v1.z)
            return Vec3(ix, iy, iz)
        }
        return null
    }

    /**
     * Interpolates color along the 4th dimension (W coordinate).
     * High +w -> wNearColor, w = 0 -> wMidColor, Low -w -> wFarColor.
     */
    fun colorForW(w: Float, palette: HyperspacePalette, maxW: Float = 1.5f): Color {
        val norm = (w / maxW).coerceIn(-1f, 1f)
        return if (norm >= 0f) {
            // Lerp between Mid and Near
            val t = norm
            lerpColor(palette.wMidColor, palette.wNearColor, t)
        } else {
            // Lerp between Far and Mid
            val t = norm + 1f // [-1, 0] -> [0, 1]
            lerpColor(palette.wFarColor, palette.wMidColor, t)
        }
    }

    private fun lerpColor(c1: Color, c2: Color, t: Float): Color {
        val clampedT = t.coerceIn(0f, 1f)
        return Color(
            red = c1.red + clampedT * (c2.red - c1.red),
            green = c1.green + clampedT * (c2.green - c1.green),
            blue = c1.blue + clampedT * (c2.blue - c1.blue),
            alpha = c1.alpha + clampedT * (c2.alpha - c1.alpha)
        )
    }
}
