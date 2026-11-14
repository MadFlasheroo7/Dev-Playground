package pro.jayeshseth.devcontrols.sdui.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import pro.jayeshseth.devcontrols.sdui.model.SduiCanvasOp
import pro.jayeshseth.devcontrols.sdui.state.SduiStateStore

/**
 * Executes server-driven canvas draw operations onto Compose's [DrawScope].
 */
object SduiCanvasRenderer {

    fun draw(
        drawScope: DrawScope,
        ops: List<SduiCanvasOp>,
        store: SduiStateStore
    ) {
        ops.forEach { op ->
            drawOp(drawScope, op, store)
        }
    }

    private fun drawOp(
        drawScope: DrawScope,
        op: SduiCanvasOp,
        store: SduiStateStore
    ) {
        when (op) {
            is SduiCanvasOp.Rect -> {
                val color = SduiColorParser.parse(store.evalString(op.color), Color.Black)
                val x = store.evalFloat(op.x, 0f)
                val y = store.evalFloat(op.y, 0f)
                val w = store.evalFloat(op.width, 100f)
                val h = store.evalFloat(op.height, 100f)
                val style = if (op.isFill) Fill else Stroke(width = op.strokeWidth)

                if (op.cornerRadius > 0f) {
                    drawScope.drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(op.cornerRadius, op.cornerRadius),
                        style = style
                    )
                } else {
                    drawScope.drawRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(w, h),
                        style = style
                    )
                }
            }
            is SduiCanvasOp.Circle -> {
                val color = SduiColorParser.parse(store.evalString(op.color), Color.Black)
                val cx = store.evalFloat(op.centerX, 50f)
                val cy = store.evalFloat(op.centerY, 50f)
                val r = store.evalFloat(op.radius, 40f)
                val style = if (op.isFill) Fill else Stroke(width = op.strokeWidth)

                drawScope.drawCircle(
                    color = color,
                    radius = r,
                    center = Offset(cx, cy),
                    style = style
                )
            }
            is SduiCanvasOp.Line -> {
                val color = SduiColorParser.parse(store.evalString(op.color), Color.Black)
                val sx = store.evalFloat(op.startX, 0f)
                val sy = store.evalFloat(op.startY, 0f)
                val ex = store.evalFloat(op.endX, 100f)
                val ey = store.evalFloat(op.endY, 100f)
                val cap = when (op.cap.lowercase()) {
                    "round" -> StrokeCap.Round
                    "square" -> StrokeCap.Square
                    else -> StrokeCap.Butt
                }

                drawScope.drawLine(
                    color = color,
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = op.strokeWidth,
                    cap = cap
                )
            }
            is SduiCanvasOp.Arc -> {
                val color = SduiColorParser.parse(store.evalString(op.color), Color.Black)
                val x = store.evalFloat(op.x, 0f)
                val y = store.evalFloat(op.y, 0f)
                val w = store.evalFloat(op.width, 100f)
                val h = store.evalFloat(op.height, 100f)
                val startAngle = store.evalFloat(op.startAngle, 0f)
                val sweepAngle = store.evalFloat(op.sweepAngle, 90f)
                val style = if (op.isFill) Fill else Stroke(width = op.strokeWidth)

                drawScope.drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = op.useCenter,
                    topLeft = Offset(x, y),
                    size = Size(w, h),
                    style = style
                )
            }
            is SduiCanvasOp.Rotate -> {
                val degrees = store.evalFloat(op.degrees, 0f)
                val pivotX = if (op.pivotX != null) store.evalFloat(op.pivotX, 0f) else drawScope.size.width / 2f
                val pivotY = if (op.pivotY != null) store.evalFloat(op.pivotY, 0f) else drawScope.size.height / 2f

                drawScope.rotate(degrees = degrees, pivot = Offset(pivotX, pivotY)) {
                    op.ops.forEach { childOp ->
                        drawOp(this, childOp, store)
                    }
                }
            }
            is SduiCanvasOp.Translate -> {
                val tx = store.evalFloat(op.x, 0f)
                val ty = store.evalFloat(op.y, 0f)

                drawScope.translate(left = tx, top = ty) {
                    op.ops.forEach { childOp ->
                        drawOp(this, childOp, store)
                    }
                }
            }
            is SduiCanvasOp.Unknown -> {
                // Ignore unknown canvas operations gracefully
            }
        }
    }
}
