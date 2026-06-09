package pro.jayeshseth.devcontrols.ui.present

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle

private val KeywordColor = Color(0xFFFF7AA8)
private val TypeColor = Color(0xFF5FD0E0)
private val FuncColor = Color(0xFFFFC857)
private val NumberColor = Color(0xFFB5E853)
private val CommentColor = Color(0xFF7C8499)
private val PunctColor = Color(0xFF9AA4C0)

/**
 * Tokenizing syntax highlighter for a code [VisualTransformation]. Recolors keywords, types/vars,
 * builtin functions, numbers, comments (`//`, `#`, `/* */`) and punctuation. Length is unchanged,
 * so the offset mapping is identity (cursor/selection stay correct).
 */
class CodeHighlighter(
    private val keywords: Set<String>,
    private val types: Set<String>,
    private val builtins: Set<String>,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(highlight(text.text), OffsetMapping.Identity)

    private fun highlight(src: String): AnnotatedString = buildAnnotatedString {
        var i = 0
        val n = src.length
        while (i < n) {
            val c = src[i]
            when {
                (c == '/' && i + 1 < n && src[i + 1] == '/') || c == '#' -> {
                    val st = i; while (i < n && src[i] != '\n') i++
                    withStyle(SpanStyle(color = CommentColor)) { append(src.substring(st, i)) }
                }
                c == '/' && i + 1 < n && src[i + 1] == '*' -> {
                    val st = i; i += 2
                    while (i + 1 < n && !(src[i] == '*' && src[i + 1] == '/')) i++
                    i = minOf(n, i + 2)
                    withStyle(SpanStyle(color = CommentColor)) { append(src.substring(st, i)) }
                }
                c.isDigit() || (c == '.' && i + 1 < n && src[i + 1].isDigit()) -> {
                    val st = i; while (i < n && (src[i].isDigit() || src[i] == '.')) i++
                    if (i < n && (src[i] == 'f' || src[i] == 'F')) i++
                    withStyle(SpanStyle(color = NumberColor)) { append(src.substring(st, i)) }
                }
                c.isLetter() || c == '_' -> {
                    val st = i; while (i < n && (src[i].isLetterOrDigit() || src[i] == '_')) i++
                    val w = src.substring(st, i)
                    val style = when (w) {
                        in keywords -> SpanStyle(color = KeywordColor, fontWeight = FontWeight.Bold)
                        in types -> SpanStyle(color = TypeColor)
                        in builtins -> SpanStyle(color = FuncColor)
                        else -> null
                    }
                    if (style != null) withStyle(style) { append(w) } else append(w)
                }
                c in "(){},;=+-*/.<>%!&|" -> { withStyle(SpanStyle(color = PunctColor)) { append(c.toString()) }; i++ }
                else -> { append(c.toString()); i++ }
            }
        }
    }
}

/** SkSL/AGSL highlighter for the shader background field. */
val ShaderHighlighter = CodeHighlighter(
    keywords = setOf("uniform", "const", "in", "out", "inout", "return", "if", "else", "for", "while",
        "struct", "void", "layout", "discard", "true", "false"),
    types = setOf("float", "float2", "float3", "float4", "half", "half2", "half3", "half4",
        "int", "int2", "int3", "int4", "bool", "mat2", "mat3", "mat4",
        "float2x2", "float3x3", "float4x4", "shader", "colorFilter"),
    builtins = setOf("main", "sin", "cos", "tan", "asin", "acos", "atan", "radians", "degrees",
        "pow", "exp", "log", "exp2", "log2", "sqrt", "inversesqrt", "abs", "sign", "floor", "ceil",
        "fract", "mod", "min", "max", "clamp", "mix", "step", "smoothstep", "length", "distance",
        "dot", "cross", "normalize", "reflect", "refract", "sample"),
)

/** Highlighter for the CanvasScript mini draw language. */
val CanvasCodeHighlighter = CodeHighlighter(
    keywords = setOf("repeat"),
    types = setOf("t", "w", "h", "i", "n", "pi"),
    builtins = setOf("background", "circle", "line", "rect", "sin", "cos", "tan", "abs", "sqrt",
        "floor", "ceil", "round", "sign", "fract", "mod", "min", "max", "pow", "clamp", "mix",
        "hypot", "atan2"),
)
