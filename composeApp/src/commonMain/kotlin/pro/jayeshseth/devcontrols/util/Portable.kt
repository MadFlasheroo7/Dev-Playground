package pro.jayeshseth.devcontrols.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Small multiplatform helpers replacing JVM-only idioms (`java.util.Calendar`, `System.currentTimeMillis`,
 * `String.format("%.2f"/"%02d"/"%02x")`) that don't exist on Kotlin/Wasm, so the shared screens compile
 * for web as well as android/desktop.
 */

/** Current epoch millis (multiplatform replacement for `System.currentTimeMillis()`). */
fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** Current wall-clock date/time in [zone] (default: system zone). */
fun nowLocal(zone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    Clock.System.now().toLocalDateTime(zone)

/** Convert epoch millis to a [LocalDateTime] in [zone]. */
fun millisToLocal(ms: Long, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime =
    Instant.fromEpochMilliseconds(ms).toLocalDateTime(zone)

/** `"%.${decimals}f"` — fixed-point decimal formatting (multiplatform). */
fun fmt(value: Double, decimals: Int): String {
    if (value.isNaN()) return "NaN"
    if (value.isInfinite()) return if (value > 0) "Inf" else "-Inf"
    val neg = value < 0
    var factor = 1L
    repeat(decimals) { factor *= 10 }
    val scaled = (abs(value) * factor).roundToLong()
    val intPart = scaled / factor
    val sign = if (neg && (intPart != 0L || scaled % factor != 0L)) "-" else ""
    return if (decimals == 0) "$sign$intPart"
    else "$sign$intPart.${(scaled % factor).toString().padStart(decimals, '0')}"
}

/** `"%.${decimals}f"` for a Float arg. */
fun fmt(value: Float, decimals: Int): String = fmt(value.toDouble(), decimals)

/** `"%0${width}d"` — left-pad an int with zeros. */
fun Int.pad(width: Int): String = (if (this < 0) "-" else "") + abs(this).toString().padStart(width, '0')
fun Long.pad(width: Int): String = (if (this < 0) "-" else "") + abs(this).toString().padStart(width, '0')

private const val HEX = "0123456789abcdef"

/** Lowercase 2-digit hex of a byte value (`"%02x"`). */
fun hex2(value: Int): String {
    val b = value and 0xFF
    return "${HEX[b ushr 4]}${HEX[b and 0xF]}"
}
fun hex2(value: Byte): String = hex2(value.toInt())

/** Uppercase hex of an int, zero-padded to [width] (`"%0${width}X"`). */
fun hexUpper(value: Int, width: Int): String =
    value.toUInt().toString(16).uppercase().padStart(width, '0')

/** Lowercase hex string of a byte array. */
fun ByteArray.toHex(): String = joinToString("") { hex2(it.toInt()) }
