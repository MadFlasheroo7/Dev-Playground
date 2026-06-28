package pro.jayeshseth.devcontrols.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pro.jayeshseth.devcontrols.ui.present.Deck

/** Pack/unpack the `.cdeck` zip container. JVM (Android/desktop) use java.util.zip; web/iOS stub. */
expect object DeckArchive {
    fun pack(json: String): ByteArray
    fun unpack(bytes: ByteArray): String
}

/** Save bytes to / open bytes from a user-chosen file (SAF on Android, native dialog on desktop). */
interface DeckFileIo {
    fun save(suggestedName: String, bytes: ByteArray)
    fun open(onLoaded: (ByteArray?) -> Unit)
}

@Composable
expect fun rememberDeckFileIo(): DeckFileIo

enum class ExportKind { PDF, PPTX, PNG }

/** Renders the deck to a file format. `null` on platforms without export support. */
interface DeckExporter {
    fun export(kind: ExportKind, deck: Deck, onStatus: (String) -> Unit)
}

@Composable
expect fun rememberDeckExporter(): DeckExporter?

/** A short transient message (Toast on Android; console/snackbar elsewhere). */
@Composable
expect fun rememberToaster(): (String) -> Unit

/** Triggers a short haptic vibration pulse (Android: vibrator click, Desktop: no-op). */
@Composable
expect fun rememberHapticPulse(): () -> Unit


/**
 * While composed, puts the host into presentation mode — fullscreen + landscape — and restores the
 * previous state on exit. Android: immersive (hidden system bars) + sensor-landscape. Desktop:
 * fullscreen window. Web: fullscreen + orientation lock. iOS: stub.
 */
@Composable
expect fun PresentationMode()

/** Gathers permission (Android: RECORD_AUDIO; Desktop: directly runs content). */
@Composable
expect fun MicrophonePermissionGate(rationale: String, content: @Composable () -> Unit)


/**
 * Renders an animated SkSL/AGSL shader as a fill in [modifier]'s area. The [source] must declare
 * `uniform float2 iResolution; uniform float iTime;` (in that order) and `half4 main(float2 fc)`.
 * Falls back to [fallback] (e.g. the gradient) when shaders are unavailable (Android < 13, web/iOS)
 * or the source fails to compile — so a broken shader never crashes the deck.
 */
@Composable
expect fun ShaderBackground(source: String, modifier: Modifier, fallback: @Composable () -> Unit)

/** A few ready-to-edit background shaders for the deck editor's "code background" field. */
object ShaderPresets {
    val list: List<Pair<String, String>> = listOf(
        "Aurora" to """uniform float2 iResolution; uniform float iTime;
half4 main(float2 fc) {
  float2 uv = fc / iResolution;
  float w = 0.5 + 0.5 * sin(iTime * 0.6 + uv.x * 6.2831);
  float g = 0.5 + 0.5 * sin(iTime * 0.4 + uv.y * 4.0 + w * 3.0);
  half3 col = half3(0.05, 0.15 + 0.5 * g, 0.25 + 0.4 * w);
  return half4(col, 1.0);
}""",
        "Plasma" to """uniform float2 iResolution; uniform float iTime;
half4 main(float2 fc) {
  float2 uv = fc / iResolution * 6.0;
  float v = sin(uv.x + iTime) + sin(uv.y + iTime * 1.3) + sin((uv.x + uv.y) * 0.7 + iTime);
  half3 col = 0.5 + 0.5 * cos(half3(v) + half3(0.0, 2.0, 4.0));
  return half4(col * 0.6, 1.0);
}""",
        "Starfield" to """uniform float2 iResolution; uniform float iTime;
float hash(float2 p){ return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453); }
half4 main(float2 fc) {
  float2 uv = fc / iResolution.y;
  float2 g = floor(uv * 60.0);
  float s = step(0.985, hash(g));
  float tw = 0.5 + 0.5 * sin(iTime * 3.0 + hash(g) * 30.0);
  half3 col = half3(0.02, 0.02, 0.06) + half3(s * tw);
  return half4(col, 1.0);
}""",
    )
}

/** Returns the current system time in milliseconds since the epoch, platform-safely. */
expect fun getPlatformTimeMillis(): Long
