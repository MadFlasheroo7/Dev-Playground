package pro.jayeshseth.devcontrols.ui.present

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * A tiny interpreted draw language giving a "code your background" experience without a runtime
 * Kotlin compiler. Statements map to Compose [DrawScope] calls and run every frame:
 *
 *   background(r,g,b[,a])              circle(x,y,radius,r,g,b[,a])
 *   line(x1,y1,x2,y2,width,r,g,b[,a])  rect(x,y,w,h,r,g,b[,a])
 *   repeat(count) { ... }              # comment   // comment
 *
 * Vars: t (seconds), w, h (px), i (repeat index), n (repeat count), pi. Color channels are 0..1.
 * Functions: sin cos tan abs sqrt floor ceil round sign fract mod min max pow clamp mix hypot atan2.
 */
object CanvasScript {
    class Program internal constructor(internal val stmts: List<Stmt>) {
        fun draw(ds: DrawScope, t: Float) {
            val env = HashMap<String, Float>()
            env["t"] = t; env["w"] = ds.size.width; env["h"] = ds.size.height; env["pi"] = kotlin.math.PI.toFloat()
            stmts.forEach { it.exec(ds, env) }
        }
    }

    /** Parse [code] into a [Program]; [Result.exceptionOrNull]'s message describes a syntax error. */
    fun parse(code: String): Result<Program> = runCatching { Program(Parser(code).parseBlock(top = true)) }
}

private fun interface Node { fun eval(env: Map<String, Float>): Float }

internal sealed class Stmt {
    abstract fun exec(ds: DrawScope, env: MutableMap<String, Float>)
}

private class Draw(val kind: String, val args: List<Node>) : Stmt() {
    override fun exec(ds: DrawScope, env: MutableMap<String, Float>) {
        val a = FloatArray(args.size) { args[it].eval(env) }
        fun col(off: Int) = Color(
            a[off].coerceIn(0f, 1f), a[off + 1].coerceIn(0f, 1f), a[off + 2].coerceIn(0f, 1f),
            (if (a.size > off + 3) a[off + 3] else 1f).coerceIn(0f, 1f)
        )
        when (kind) {
            "background" -> ds.drawRect(col(0))
            "circle" -> ds.drawCircle(col(3), radius = a[2], center = Offset(a[0], a[1]))
            "line" -> ds.drawLine(col(5), Offset(a[0], a[1]), Offset(a[2], a[3]), strokeWidth = a[4])
            "rect" -> ds.drawRect(col(4), topLeft = Offset(a[0], a[1]), size = Size(a[2], a[3]))
        }
    }
}

private class Repeat(val count: Node, val body: List<Stmt>) : Stmt() {
    override fun exec(ds: DrawScope, env: MutableMap<String, Float>) {
        val n = count.eval(env).toInt().coerceIn(0, 5000)
        val savedI = env["i"]; val savedN = env["n"]
        env["n"] = n.toFloat()
        for (k in 0 until n) { env["i"] = k.toFloat(); body.forEach { it.exec(ds, env) } }
        if (savedI != null) env["i"] = savedI else env.remove("i")
        if (savedN != null) env["n"] = savedN else env.remove("n")
    }
}

private val funcs: Map<String, (FloatArray) -> Float> = mapOf(
    "sin" to { a -> sin(a[0]) }, "cos" to { a -> cos(a[0]) }, "tan" to { a -> tan(a[0]) },
    "abs" to { a -> abs(a[0]) }, "sqrt" to { a -> sqrt(a[0]) }, "floor" to { a -> floor(a[0]) },
    "ceil" to { a -> ceil(a[0]) }, "round" to { a -> round(a[0]) }, "sign" to { a -> sign(a[0]) },
    "fract" to { a -> a[0] - floor(a[0]) }, "mod" to { a -> a[0] - a[1] * floor(a[0] / a[1]) },
    "min" to { a -> min(a[0], a[1]) }, "max" to { a -> max(a[0], a[1]) }, "pow" to { a -> a[0].pow(a[1]) },
    "clamp" to { a -> a[0].coerceIn(a[1], a[2]) }, "mix" to { a -> a[0] + (a[1] - a[0]) * a[2] },
    "hypot" to { a -> hypot(a[0], a[1]) }, "atan2" to { a -> atan2(a[0], a[1]) },
)

private class Parser(src: String) {
    private val s = src.lines().joinToString("\n") { line ->
        val cut = listOf(line.indexOf('#'), line.indexOf("//")).filter { it >= 0 }.minOrNull()
        if (cut != null) line.substring(0, cut) else line
    }
    private var p = 0

    private fun ws() { while (p < s.length && s[p].isWhitespace()) p++ }
    private fun peek(): Char? { ws(); return if (p < s.length) s[p] else null }
    private fun eat(c: Char) { ws(); if (p >= s.length || s[p] != c) error("expected '$c'"); p++ }
    private fun ident(): String { ws(); val st = p; while (p < s.length && (s[p].isLetterOrDigit() || s[p] == '_')) p++; if (p == st) error("expected name at $p"); return s.substring(st, p) }

    fun parseBlock(top: Boolean): List<Stmt> {
        val out = ArrayList<Stmt>()
        while (true) {
            val c = peek() ?: break
            if (c == '}') { if (top) error("unexpected '}'"); break }
            val name = ident(); eat('(')
            if (name == "repeat") {
                val count = expr(); eat(')'); eat('{')
                val body = parseBlock(top = false); eat('}')
                out.add(Repeat(count, body))
            } else {
                if (name !in setOf("background", "circle", "line", "rect")) error("unknown command '$name'")
                val args = ArrayList<Node>()
                if (peek() != ')') { args.add(expr()); while (peek() == ',') { eat(','); args.add(expr()) } }
                eat(')')
                out.add(Draw(name, args))
            }
        }
        return out
    }

    private fun expr(): Node {
        var a = term()
        while (true) {
            val c = peek()
            if (c == '+' || c == '-') { p++; val b = term(); val l = a; val op = c; a = Node { if (op == '+') l.eval(it) + b.eval(it) else l.eval(it) - b.eval(it) } } else break
        }
        return a
    }

    private fun term(): Node {
        var a = factor()
        while (true) {
            val c = peek()
            if (c == '*' || c == '/' || c == '%') { p++; val b = factor(); val l = a; val op = c; a = Node { val x = l.eval(it); val y = b.eval(it); when (op) { '*' -> x * y; '/' -> x / y; else -> x - y * floor(x / y) } } } else break
        }
        return a
    }

    private fun factor(): Node {
        val c = peek() ?: error("unexpected end")
        if (c == '-') { p++; val f = factor(); return Node { -f.eval(it) } }
        if (c == '(') { eat('('); val e = expr(); eat(')'); return e }
        if (c.isDigit() || c == '.') { ws(); val st = p; while (p < s.length && (s[p].isDigit() || s[p] == '.')) p++; val v = s.substring(st, p).toFloat(); return Node { v } }
        val name = ident()
        if (peek() == '(') {
            eat('('); val args = ArrayList<Node>()
            if (peek() != ')') { args.add(expr()); while (peek() == ',') { eat(','); args.add(expr()) } }
            eat(')')
            val fn = funcs[name] ?: error("unknown function '$name'")
            return Node { env -> fn(FloatArray(args.size) { args[it].eval(env) }) }
        }
        return Node { it[name] ?: 0f }
    }
}

/** Default starter script for the editor. */
const val CANVAS_CODE_SAMPLE = """# Compose-canvas code background. Vars: t, w, h, i, n.
background(0.05, 0.06, 0.12)
repeat(70) {
  circle(
    w * fract(sin(i * 12.9) * 43758.5),
    mod(t * 60 + i * 53, h + 20) - 10,
    3,
    0.4 + 0.5 * sin(i), 0.7, 1.0, 0.6
  )
}"""

/** Renders [code] (the mini draw language) animated; draws nothing if it fails to parse. */
@Composable
fun CanvasCodeBackground(code: String, modifier: Modifier = Modifier) {
    val program = remember(code) { CanvasScript.parse(code).getOrNull() } ?: return
    val t by produceState(0f) { while (true) { withInfiniteAnimationFrameMillis { value = it / 1000f } } }
    Canvas(modifier.fillMaxSize()) { program.draw(this, t) }
}
