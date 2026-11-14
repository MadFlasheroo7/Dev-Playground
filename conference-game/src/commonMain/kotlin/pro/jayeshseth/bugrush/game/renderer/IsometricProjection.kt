package pro.jayeshseth.bugrush.game.renderer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

data class VoxelCuboid(
    val x: Float,
    val y: Float,
    val z: Float,
    val sizeX: Float,
    val sizeY: Float,
    val sizeZ: Float,
    val baseColor: Color,
    val topColor: Color = baseColor.copy(
        red = (baseColor.red * 1.15f).coerceAtMost(1f),
        green = (baseColor.green * 1.15f).coerceAtMost(1f),
        blue = (baseColor.blue * 1.15f).coerceAtMost(1f)
    ),
    val leftColor: Color = baseColor.copy(
        red = baseColor.red * 0.70f,
        green = baseColor.green * 0.70f,
        blue = baseColor.blue * 0.70f
    ),
    val rightColor: Color = baseColor.copy(
        red = baseColor.red * 0.85f,
        green = baseColor.green * 0.85f,
        blue = baseColor.blue * 0.85f
    ),
    val isFloor: Boolean = false
) {
    // Depth for painter's depth-sorting along standard isometric line of sight (X+Y+Z)
    // Floor tiles have an overwhelmingly negative base depth so they always draw underneath standing characters!
    val depth: Float
        get() = if (isFloor) -100_000f + (x + y) * 0.01f else (x + y + z * 0.8f)
}

object IsometricProjection {

    private const val SQRT2 = 1.4142135f
    const val TILE_WIDTH_MULTIPLIER = 1.0f * SQRT2 * 2f
    const val TILE_HEIGHT_MULTIPLIER = 1.0f * SQRT2 * 1.0f
    private const val DEPTH_EFFECT = (TILE_HEIGHT_MULTIPLIER / TILE_WIDTH_MULTIPLIER) * 1.5f

    /**
     * Converts 3D world space (x, y, z) to 2D screen space (px, py) relative to camera center.
     */
    fun project(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        cameraCenterX: Float,
        cameraCenterY: Float,
        screenCenterX: Float,
        screenCenterY: Float,
        zoom: Float = 0.55f,
        cameraAngleRad: Float = 0f
    ): Offset {
        val relX = worldX - cameraCenterX
        val relY = worldY - cameraCenterY
        val relZ = worldZ

        val rotX: Float
        val rotY: Float
        if (cameraAngleRad != 0f) {
            val cosA = cos(cameraAngleRad)
            val sinA = sin(cameraAngleRad)
            rotX = relX * cosA - relY * sinA
            rotY = relX * sinA + relY * cosA
        } else {
            rotX = relX
            rotY = relY
        }

        val projX = (rotX - rotY) * 0.5f * TILE_WIDTH_MULTIPLIER * zoom
        val projY = (rotX + rotY) * 0.5f * TILE_HEIGHT_MULTIPLIER * zoom - (relZ / DEPTH_EFFECT) * TILE_HEIGHT_MULTIPLIER * zoom

        return Offset(screenCenterX + projX, screenCenterY + projY)
    }

    private val reusablePath = Path()

    fun isInViewport(
        screenX: Float,
        screenY: Float,
        viewportWidth: Float,
        viewportHeight: Float,
        margin: Float = 140f
    ): Boolean {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return true
        return screenX >= -margin &&
                screenX <= viewportWidth + margin &&
                screenY >= -margin &&
                screenY <= viewportHeight + margin
    }

    /**
     * Draws an isometric cuboid with shaded top, front-left, and front-right faces.
     * Incorporates frustum culling, zero-allocation path reuse, and floor fast-path.
     */
    fun drawCuboid(
        scope: DrawScope,
        cuboid: VoxelCuboid,
        cameraCenterX: Float,
        cameraCenterY: Float,
        screenCenterX: Float,
        screenCenterY: Float,
        zoom: Float = 0.55f,
        strokeColor: Color = Color(0x33000000),
        strokeWidth: Float = 1.5f,
        viewportWidth: Float = 0f,
        viewportHeight: Float = 0f,
        cameraAngleRad: Float = 0f
    ) {
        val hx = cuboid.sizeX * 0.5f
        val hy = cuboid.sizeY * 0.5f
        val hz = cuboid.sizeZ

        val cx = cuboid.x
        val cy = cuboid.y
        val cz = cuboid.z

        // 1. Frustum Viewport Culling - Skip off-screen cuboids
        val center = project(cx, cy, cz + hz * 0.5f, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
        val cullMargin = (160f * zoom * 2f).coerceAtLeast(140f)
        if (!isInViewport(center.x, center.y, viewportWidth, viewportHeight, margin = cullMargin)) {
            return
        }

        // 2. Fast-Path for Floor Tiles (Only top quad, no stroke, zero side faces)
        if (cuboid.isFloor) {
            val topZ = cz + hz
            val p4 = project(cx - hx, cy - hy, topZ, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
            val p5 = project(cx + hx, cy - hy, topZ, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
            val p6 = project(cx + hx, cy + hy, topZ, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
            val p7 = project(cx - hx, cy + hy, topZ, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)

            reusablePath.reset()
            reusablePath.moveTo(p4.x, p4.y)
            reusablePath.lineTo(p5.x, p5.y)
            reusablePath.lineTo(p6.x, p6.y)
            reusablePath.lineTo(p7.x, p7.y)
            reusablePath.close()
            scope.drawPath(reusablePath, cuboid.topColor, style = Fill)
            return
        }

        // 3. Project 8 vertices for regular solid 3D cuboids
        // Bottom vertices (z = cz)
        val p0 = project(cx - hx, cy - hy, cz, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
        val p1 = project(cx + hx, cy - hy, cz, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
        val p2 = project(cx + hx, cy + hy, cz, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
        val p3 = project(cx - hx, cy + hy, cz, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)

        // Top vertices (z = cz + hz)
        val p4 = project(cx - hx, cy - hy, cz + hz, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
        val p5 = project(cx + hx, cy - hy, cz + hz, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
        val p6 = project(cx + hx, cy + hy, cz + hz, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
        val p7 = project(cx - hx, cy + hy, cz + hz, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)

        // 4. Determine camera view visibility for all 4 vertical faces
        val cosA = if (cameraAngleRad != 0f) cos(cameraAngleRad) else 1f
        val sinA = if (cameraAngleRad != 0f) sin(cameraAngleRad) else 0f
        val dotX = cosA + sinA
        val dotY = cosA - sinA

        // Y-axis oriented vertical faces:
        if (dotY > 0f) {
            // Front-Left Face (+Y): (p3, p2, p6, p7) - Facing camera from South-West
            reusablePath.reset()
            reusablePath.moveTo(p3.x, p3.y)
            reusablePath.lineTo(p2.x, p2.y)
            reusablePath.lineTo(p6.x, p6.y)
            reusablePath.lineTo(p7.x, p7.y)
            reusablePath.close()
            scope.drawPath(reusablePath, cuboid.leftColor, style = Fill)
            if (strokeWidth > 0f) scope.drawPath(reusablePath, strokeColor, style = Stroke(strokeWidth))
        } else if (dotY < 0f) {
            // Back-Left Face (-Y): (p1, p0, p4, p5) - Facing camera from North-East
            reusablePath.reset()
            reusablePath.moveTo(p1.x, p1.y)
            reusablePath.lineTo(p0.x, p0.y)
            reusablePath.lineTo(p4.x, p4.y)
            reusablePath.lineTo(p5.x, p5.y)
            reusablePath.close()
            scope.drawPath(reusablePath, cuboid.leftColor, style = Fill)
            if (strokeWidth > 0f) scope.drawPath(reusablePath, strokeColor, style = Stroke(strokeWidth))
        }

        // X-axis oriented vertical faces:
        if (dotX > 0f) {
            // Front-Right Face (+X): (p2, p1, p5, p6) - Facing camera from South-East
            reusablePath.reset()
            reusablePath.moveTo(p2.x, p2.y)
            reusablePath.lineTo(p1.x, p1.y)
            reusablePath.lineTo(p5.x, p5.y)
            reusablePath.lineTo(p6.x, p6.y)
            reusablePath.close()
            scope.drawPath(reusablePath, cuboid.rightColor, style = Fill)
            if (strokeWidth > 0f) scope.drawPath(reusablePath, strokeColor, style = Stroke(strokeWidth))
        } else if (dotX < 0f) {
            // Back-Right Face (-X): (p0, p3, p7, p4) - Facing camera from North-West
            reusablePath.reset()
            reusablePath.moveTo(p0.x, p0.y)
            reusablePath.lineTo(p3.x, p3.y)
            reusablePath.lineTo(p7.x, p7.y)
            reusablePath.lineTo(p4.x, p4.y)
            reusablePath.close()
            scope.drawPath(reusablePath, cuboid.rightColor, style = Fill)
            if (strokeWidth > 0f) scope.drawPath(reusablePath, strokeColor, style = Stroke(strokeWidth))
        }

        // Top Face (p4, p5, p6, p7) - Facing +Z (Up, always visible from isometric overhead view)
        reusablePath.reset()
        reusablePath.moveTo(p4.x, p4.y)
        reusablePath.lineTo(p5.x, p5.y)
        reusablePath.lineTo(p6.x, p6.y)
        reusablePath.lineTo(p7.x, p7.y)
        reusablePath.close()
        scope.drawPath(reusablePath, cuboid.topColor, style = Fill)
        if (strokeWidth > 0f) scope.drawPath(reusablePath, strokeColor, style = Stroke(strokeWidth))
    }

    /**
     * Converts 2D screen space (px, py) back to 3D world space (worldX, worldY) on a given ground plane Z.
     */
    fun unproject(
        screenX: Float,
        screenY: Float,
        groundZ: Float = 0f,
        cameraCenterX: Float,
        cameraCenterY: Float,
        screenCenterX: Float,
        screenCenterY: Float,
        zoom: Float = 0.52f,
        cameraAngleRad: Float = 0f
    ): Pair<Float, Float> {
        val dx = screenX - screenCenterX
        val dy = (screenY - screenCenterY) + (groundZ / DEPTH_EFFECT) * TILE_HEIGHT_MULTIPLIER * zoom

        val a = 0.5f * TILE_WIDTH_MULTIPLIER * zoom
        val b = 0.5f * TILE_HEIGHT_MULTIPLIER * zoom

        val termA = dx / a
        val termB = dy / b

        val rotX = 0.5f * (termB + termA)
        val rotY = 0.5f * (termB - termA)

        val unrotX: Float
        val unrotY: Float
        if (cameraAngleRad != 0f) {
            val cosA = cos(-cameraAngleRad)
            val sinA = sin(-cameraAngleRad)
            unrotX = rotX * cosA - rotY * sinA
            unrotY = rotX * sinA + rotY * cosA
        } else {
            unrotX = rotX
            unrotY = rotY
        }

        return Pair(cameraCenterX + unrotX, cameraCenterY + unrotY)
    }

    /**
     * Renders a cyber holographic targeting reticle on the isometric ground plane.
     */
    fun drawAimReticle(
        scope: DrawScope,
        worldX: Float,
        worldY: Float,
        cameraCenterX: Float,
        cameraCenterY: Float,
        screenCenterX: Float,
        screenCenterY: Float,
        zoom: Float = 0.52f,
        color: Color = Color(0xFF00E5FF),
        cameraAngleRad: Float = 0f
    ) {
        val center = project(worldX, worldY, 2f, cameraCenterX, cameraCenterY, screenCenterX, screenCenterY, zoom, cameraAngleRad)
        val r = 16f
        val rx = r * 1.4f
        val ry = r * 0.85f

        val path = Path().apply {
            moveTo(center.x, center.y - ry)
            lineTo(center.x + rx, center.y)
            lineTo(center.x, center.y + ry)
            lineTo(center.x - rx, center.y)
            close()
        }
        scope.drawPath(path, color.copy(alpha = 0.22f), style = Fill)
        scope.drawPath(path, color, style = Stroke(width = 2f))
        scope.drawCircle(color, radius = 3f, center = center)
    }
}

