package pro.jayeshseth.devcontrols.sdui.eval

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/**
 * Sandboxed Expression Evaluator for Server-Driven UI.
 *
 * Supports:
 * - Arithmetic (+, -, *, /, %)
 * - Comparisons (==, !=, <, <=, >, >=)
 * - Boolean logic (&&, ||, !)
 * - Ternary operator (cond ? then : else)
 * - String concatenation and interpolation ("Hello ${name}")
 * - Dotted property access ("user.address.city")
 * - Math functions (min, max, abs, round, floor, ceil, clamp, len)
 */
object SduiExpressionEvaluator {

    /**
     * Evaluates a string that may contain `${...}` interpolation patterns, or evaluates
     * a pure expression.
     */
    fun evaluate(raw: String?, context: Map<String, Any?>): Any? {
        if (raw == null) return null
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        // Case 1: Pure single expression "${...}"
        if (trimmed.startsWith("\${") && trimmed.endsWith("}")) {
            val end = findMatchingBrace(trimmed, 2)
            if (end == trimmed.length - 1) {
                val innerExpr = trimmed.substring(2, trimmed.length - 1).trim()
                return evaluateExpression(innerExpr, context)
            }
        }

        // Case 2: String with interpolation "${...}" embedded
        if (trimmed.contains("\${")) {
            val sb = StringBuilder()
            var i = 0
            while (i < trimmed.length) {
                val start = trimmed.indexOf("\${", i)
                if (start == -1) {
                    sb.append(trimmed.substring(i))
                    break
                }
                sb.append(trimmed.substring(i, start))
                val end = findMatchingBrace(trimmed, start + 2)
                if (end != -1) {
                    val expr = trimmed.substring(start + 2, end).trim()
                    val result = evaluateExpression(expr, context)
                    sb.append(formatValue(result))
                    i = end + 1
                } else {
                    sb.append(trimmed.substring(start))
                    break
                }
            }
            return sb.toString()
        }

        // Case 3: Try evaluating as direct expression if it looks like one (e.g. boolean/number/ternary/math)
        return try {
            val tokens = tokenize(trimmed)
            if (tokens.isEmpty()) return raw
            // Single identifier check
            if (tokens.size == 1 && tokens[0] is Token.Ident) {
                val name = (tokens[0] as Token.Ident).name
                when (name) {
                    "true" -> true
                    "false" -> false
                    "null" -> null
                    else -> if (hasProperty(context, name)) resolveIdentifier(name, context) else raw
                }
            } else {
                val parser = Parser(tokens, context)
                val result = parser.parse()
                if (parser.hasRemainingTokens()) {
                    raw
                } else {
                    result ?: raw
                }
            }
        } catch (_: Exception) {
            raw // fallback to raw string literal if not a valid expression
        }
    }

    fun hasProperty(context: Map<String, Any?>, path: String): Boolean {
        val parts = path.split('.')
        var curr: Any? = if (context.containsKey(parts[0])) context[parts[0]] else return false
        for (p in 1 until parts.size) {
            if (curr is Map<*, *>) {
                if (curr.containsKey(parts[p])) {
                    curr = curr[parts[p]]
                } else {
                    return false
                }
            } else {
                return false
            }
        }
        return true
    }

    fun resolveIdentifier(path: String, context: Map<String, Any?>): Any? {
        val parts = path.split('.')
        var curr: Any? = context[parts[0]]
        for (p in 1 until parts.size) {
            if (curr is Map<*, *>) {
                curr = curr[parts[p]]
            } else {
                return null
            }
        }
        return curr
    }

    fun evaluateBoolean(raw: String?, context: Map<String, Any?>, defaultValue: Boolean = false): Boolean {
        if (raw == null) return defaultValue
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return defaultValue
        val tokens = tokenize(trimmed)
        if (tokens.size == 1 && tokens[0] is Token.Ident) {
            val name = (tokens[0] as Token.Ident).name
            if (name == "true") return true
            if (name == "false") return false
            if (!hasProperty(context, name)) return defaultValue
        }
        val result = evaluate(raw, context)
        return isTruthy(result)
    }

    fun evaluateFloat(raw: String?, context: Map<String, Any?>, defaultValue: Float = 0f): Float {
        if (raw == null) return defaultValue
        val result = evaluate(raw, context)
        return toDouble(result)?.toFloat() ?: defaultValue
    }

    fun evaluateDouble(raw: String?, context: Map<String, Any?>, defaultValue: Double = 0.0): Double {
        if (raw == null) return defaultValue
        val result = evaluate(raw, context)
        return toDouble(result) ?: defaultValue
    }

    fun evaluateString(raw: String?, context: Map<String, Any?>, defaultValue: String = ""): String {
        if (raw == null) return defaultValue
        val result = evaluate(raw, context)
        return if (result != null) formatValue(result) else defaultValue
    }

    fun evaluateExpression(expr: String, context: Map<String, Any?>): Any? {
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) return null
        val parser = Parser(tokens, context)
        return parser.parse()
    }

    private fun findMatchingBrace(text: String, startIdx: Int): Int {
        var depth = 1
        for (i in startIdx until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun formatValue(v: Any?): String {
        return when (v) {
            null -> ""
            is Double -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
            is Float -> if (v % 1f == 0f) v.toLong().toString() else v.toString()
            else -> v.toString()
        }
    }

    fun isTruthy(v: Any?): Boolean {
        return when (v) {
            null -> false
            is Boolean -> v
            is Number -> v.toDouble() != 0.0
            is String -> v.isNotEmpty() && v != "false" && v != "0"
            is Collection<*> -> v.isNotEmpty()
            is Map<*, *> -> v.isNotEmpty()
            else -> true
        }
    }

    fun toDouble(v: Any?): Double? {
        return when (v) {
            null -> null
            is Number -> v.toDouble()
            is Boolean -> if (v) 1.0 else 0.0
            is String -> v.trim().toDoubleOrNull()
            else -> null
        }
    }

    // --- Lexer & Parser ---

    private sealed interface Token {
        data class Num(val value: Double) : Token
        data class Str(val value: String) : Token
        data class Ident(val name: String) : Token
        data class Op(val symbol: String) : Token
        object LParen : Token
        object RParen : Token
        object Comma : Token
        object Question : Token
        object Colon : Token
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val n = input.length

        while (i < n) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c == '(' -> { tokens.add(Token.LParen); i++ }
                c == ')' -> { tokens.add(Token.RParen); i++ }
                c == ',' -> { tokens.add(Token.Comma); i++ }
                c == '?' -> { tokens.add(Token.Question); i++ }
                c == ':' -> { tokens.add(Token.Colon); i++ }
                c == '\'' || c == '"' -> {
                    val quote = c
                    i++
                    val start = i
                    val sb = StringBuilder()
                    while (i < n && input[i] != quote) {
                        if (input[i] == '\\' && i + 1 < n) {
                            i++
                            sb.append(input[i])
                        } else {
                            sb.append(input[i])
                        }
                        i++
                    }
                    if (i < n) i++ // skip closing quote
                    tokens.add(Token.Str(sb.toString()))
                }
                c.isDigit() || (c == '.' && i + 1 < n && input[i + 1].isDigit()) -> {
                    val start = i
                    while (i < n && (input[i].isDigit() || input[i] == '.')) {
                        i++
                    }
                    val numStr = input.substring(start, i)
                    tokens.add(Token.Num(numStr.toDoubleOrNull() ?: 0.0))
                }
                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < n && (input[i].isLetterOrDigit() || input[i] == '_' || input[i] == '.')) {
                        i++
                    }
                    val name = input.substring(start, i)
                    tokens.add(Token.Ident(name))
                }
                c == '&' && i + 1 < n && input[i + 1] == '&' -> {
                    tokens.add(Token.Op("&&"))
                    i += 2
                }
                c == '|' && i + 1 < n && input[i + 1] == '|' -> {
                    tokens.add(Token.Op("||"))
                    i += 2
                }
                c == '=' && i + 1 < n && input[i + 1] == '=' -> {
                    tokens.add(Token.Op("=="))
                    i += 2
                }
                c == '!' && i + 1 < n && input[i + 1] == '=' -> {
                    tokens.add(Token.Op("!="))
                    i += 2
                }
                c == '<' && i + 1 < n && input[i + 1] == '=' -> {
                    tokens.add(Token.Op("<="))
                    i += 2
                }
                c == '>' && i + 1 < n && input[i + 1] == '=' -> {
                    tokens.add(Token.Op(">="))
                    i += 2
                }
                c in "+-*/%!><" -> {
                    tokens.add(Token.Op(c.toString()))
                    i++
                }
                else -> i++
            }
        }
        return tokens
    }

    private class Parser(
        private val tokens: List<Token>,
        private val context: Map<String, Any?>
    ) {
        private var pos = 0

        fun parse(): Any? {
            val result = parseTernary()
            return result
        }

        fun hasRemainingTokens(): Boolean = pos < tokens.size

        private fun peek(): Token? = if (pos < tokens.size) tokens[pos] else null
        private fun next(): Token? = if (pos < tokens.size) tokens[pos++] else null

        // ternary: cond ? thenExpr : elseExpr
        private fun parseTernary(): Any? {
            val cond = parseLogicalOr()
            if (peek() is Token.Question) {
                next() // consume '?'
                val thenBranch = parseTernary()
                if (peek() is Token.Colon) {
                    next() // consume ':'
                    val elseBranch = parseTernary()
                    return if (isTruthy(cond)) thenBranch else elseBranch
                }
                return if (isTruthy(cond)) thenBranch else null
            }
            return cond
        }

        private fun parseLogicalOr(): Any? {
            var left = parseLogicalAnd()
            while (peek() == Token.Op("||")) {
                next()
                val right = parseLogicalAnd()
                left = isTruthy(left) || isTruthy(right)
            }
            return left
        }

        private fun parseLogicalAnd(): Any? {
            var left = parseEquality()
            while (peek() == Token.Op("&&")) {
                next()
                val right = parseEquality()
                left = isTruthy(left) && isTruthy(right)
            }
            return left
        }

        private fun parseEquality(): Any? {
            var left = parseRelational()
            while (true) {
                val tok = peek()
                if (tok == Token.Op("==")) {
                    next()
                    val right = parseRelational()
                    left = compareEqual(left, right)
                } else if (tok == Token.Op("!=")) {
                    next()
                    val right = parseRelational()
                    left = !compareEqual(left, right)
                } else {
                    break
                }
            }
            return left
        }

        private fun parseRelational(): Any? {
            var left = parseAdditive()
            while (true) {
                val tok = peek()
                if (tok is Token.Op && tok.symbol in listOf("<", "<=", ">", ">=")) {
                    next()
                    val right = parseAdditive()
                    val lNum = toDouble(left) ?: 0.0
                    val rNum = toDouble(right) ?: 0.0
                    left = when (tok.symbol) {
                        "<" -> lNum < rNum
                        "<=" -> lNum <= rNum
                        ">" -> lNum > rNum
                        ">=" -> lNum >= rNum
                        else -> false
                    }
                } else {
                    break
                }
            }
            return left
        }

        private fun parseAdditive(): Any? {
            var left = parseMultiplicative()
            while (true) {
                val tok = peek()
                if (tok == Token.Op("+")) {
                    next()
                    val right = parseMultiplicative()
                    left = if (left is String || right is String) {
                        formatValue(left) + formatValue(right)
                    } else {
                        val lNum = toDouble(left) ?: 0.0
                        val rNum = toDouble(right) ?: 0.0
                        lNum + rNum
                    }
                } else if (tok == Token.Op("-")) {
                    next()
                    val right = parseMultiplicative()
                    val lNum = toDouble(left) ?: 0.0
                    val rNum = toDouble(right) ?: 0.0
                    left = lNum - rNum
                } else {
                    break
                }
            }
            return left
        }

        private fun parseMultiplicative(): Any? {
            var left = parseUnary()
            while (true) {
                val tok = peek()
                if (tok == Token.Op("*")) {
                    next()
                    val right = parseUnary()
                    val lNum = toDouble(left) ?: 0.0
                    val rNum = toDouble(right) ?: 0.0
                    left = lNum * rNum
                } else if (tok == Token.Op("/")) {
                    next()
                    val right = parseUnary()
                    val lNum = toDouble(left) ?: 0.0
                    val rNum = toDouble(right) ?: 1.0
                    left = if (rNum == 0.0) 0.0 else lNum / rNum
                } else if (tok == Token.Op("%")) {
                    next()
                    val right = parseUnary()
                    val lNum = toDouble(left) ?: 0.0
                    val rNum = toDouble(right) ?: 1.0
                    left = if (rNum == 0.0) 0.0 else lNum % rNum
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): Any? {
            val tok = peek()
            if (tok == Token.Op("!")) {
                next()
                return !isTruthy(parseUnary())
            }
            if (tok == Token.Op("-")) {
                next()
                val v = toDouble(parseUnary()) ?: 0.0
                return -v
            }
            if (tok == Token.Op("+")) {
                next()
                return toDouble(parseUnary()) ?: 0.0
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Any? {
            val tok = next() ?: return null
            return when (tok) {
                is Token.Num -> tok.value
                is Token.Str -> tok.value
                is Token.LParen -> {
                    val res = parseTernary()
                    if (peek() is Token.RParen) next()
                    res
                }
                is Token.Ident -> {
                    // Check if it's a function call e.g. min(...)
                    if (peek() is Token.LParen) {
                        next() // consume '('
                        val args = mutableListOf<Any?>()
                        if (peek() !is Token.RParen) {
                            args.add(parseTernary())
                            while (peek() is Token.Comma) {
                                next() // consume ','
                                args.add(parseTernary())
                            }
                        }
                        if (peek() is Token.RParen) next()
                        evaluateFunction(tok.name, args)
                    } else {
                        when (tok.name) {
                            "true" -> true
                            "false" -> false
                            "null" -> null
                            else -> resolveIdentifier(tok.name)
                        }
                    }
                }
                else -> null
            }
        }

        private fun evaluateFunction(name: String, args: List<Any?>): Any? {
            return when (name.lowercase()) {
                "min" -> {
                    val a = toDouble(args.getOrNull(0)) ?: 0.0
                    val b = toDouble(args.getOrNull(1)) ?: 0.0
                    min(a, b)
                }
                "max" -> {
                    val a = toDouble(args.getOrNull(0)) ?: 0.0
                    val b = toDouble(args.getOrNull(1)) ?: 0.0
                    max(a, b)
                }
                "abs" -> abs(toDouble(args.getOrNull(0)) ?: 0.0)
                "round" -> round(toDouble(args.getOrNull(0)) ?: 0.0)
                "floor" -> floor(toDouble(args.getOrNull(0)) ?: 0.0)
                "ceil" -> ceil(toDouble(args.getOrNull(0)) ?: 0.0)
                "clamp" -> {
                    val v = toDouble(args.getOrNull(0)) ?: 0.0
                    val minV = toDouble(args.getOrNull(1)) ?: 0.0
                    val maxV = toDouble(args.getOrNull(2)) ?: 0.0
                    max(minV, min(v, maxV))
                }
                "len" -> (args.getOrNull(0)?.toString() ?: "").length.toDouble()
                "contains" -> {
                    val str = args.getOrNull(0)?.toString() ?: ""
                    val sub = args.getOrNull(1)?.toString() ?: ""
                    str.contains(sub)
                }
                else -> null
            }
        }

        private fun resolveIdentifier(path: String): Any? {
            val parts = path.split('.')
            var curr: Any? = context[parts[0]]
            for (p in 1 until parts.size) {
                if (curr is Map<*, *>) {
                    curr = curr[parts[p]]
                } else {
                    return null
                }
            }
            return curr
        }

        private fun compareEqual(a: Any?, b: Any?): Boolean {
            if (a == null && b == null) return true
            if (a == null || b == null) return false
            val aNum = toDouble(a)
            val bNum = toDouble(b)
            if (aNum != null && bNum != null) {
                return aNum == bNum
            }
            return a.toString() == b.toString()
        }
    }
}
