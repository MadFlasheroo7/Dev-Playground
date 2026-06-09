package pro.jayeshseth.media

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Base64

/**
 * Kotlin port of NewPipe's `JavaScriptUtil` — the glue that turns the binary blobs returned by
 * YouTube's BotGuard endpoints (`/Create`, `/GenerateIT`) into JavaScript snippets the WebView VM can
 * consume, and back again. Reimplemented on `kotlinx-serialization-json` + `java.util.Base64` (instead
 * of NewPipe's nanojson + okio) so it has zero extra dependencies. See [PoTokenWebView].
 */

private val JSON = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Parses the raw challenge data from the `/Create` endpoint into a JSON object string that can be
 * embedded directly in a JS snippet (assigned to `data` and fed to `runBotGuard`). The outer payload is
 * sometimes "scrambled" (base64 + a per-byte +97 shift) in element 1; if so we descramble it first.
 */
fun parseChallengeData(rawChallengeData: String): String {
    val scrambled = JSON.parseToJsonElement(rawChallengeData).jsonArray

    val challengeData: JsonArray =
        if (scrambled.size > 1 && (scrambled[1] as? JsonPrimitive)?.isString == true) {
            JSON.parseToJsonElement(descramble(scrambled[1].jsonPrimitive.content)).jsonArray
        } else {
            scrambled[0].jsonArray
        }

    val messageId = challengeData[0].jsonPrimitive.content
    val interpreterHash = challengeData[3].jsonPrimitive.content
    val program = challengeData[4].jsonPrimitive.content
    val globalName = challengeData[5].jsonPrimitive.content
    val clientExperimentsStateBlob = challengeData[7].jsonPrimitive.content

    // Elements 1 & 2 are arrays carrying the interpreter JS as their first string entry.
    val safeScript = firstString(challengeData.getOrNull(1))
    val trustedUrl = firstString(challengeData.getOrNull(2))

    return buildJsonObject {
        put("messageId", messageId)
        putJsonObject("interpreterJavascript") {
            put("privateDoNotAccessOrElseSafeScriptWrappedValue", safeScript)
            put("privateDoNotAccessOrElseTrustedResourceUrlWrappedValue", trustedUrl)
        }
        put("interpreterHash", interpreterHash)
        put("program", program)
        put("globalName", globalName)
        put("clientExperimentsStateBlob", clientExperimentsStateBlob)
    }.toString()
}

/**
 * Parses the `/GenerateIT` response into a JS `Uint8Array(...)` literal for the `integrityToken` plus
 * the token's lifetime in seconds.
 */
fun parseIntegrityTokenData(rawIntegrityTokenData: String): Pair<String, Long> {
    val arr = JSON.parseToJsonElement(rawIntegrityTokenData).jsonArray
    return base64ToU8(arr[0].jsonPrimitive.content) to arr[1].jsonPrimitive.long
}

/** Encodes a string (the poToken identifier) as a JS `Uint8Array(...)` literal. */
fun stringToU8(identifier: String): String = newUint8Array(identifier.toByteArray())

/**
 * Converts the comma-separated byte string produced by JS `Uint8Array::toString()` (e.g. "97,98,99")
 * into YouTube's base64url poToken representation.
 */
fun u8ToBase64(poToken: String): String {
    val bytes = poToken.split(",").map { it.toUByte().toByte() }.toByteArray()
    return bytesToYtBase64(bytes)
}

// ---- internals -------------------------------------------------------------------------------------

private fun firstString(element: Any?): String? =
    (element as? JsonArray)?.firstOrNull { it is JsonPrimitive && it.isString }?.jsonPrimitive?.content

/** Decodes base64, then shifts every byte by +97 — YouTube's lightweight "scramble". */
private fun descramble(scrambledChallenge: String): String =
    base64ToBytes(scrambledChallenge).map { (it + 97).toByte() }.toByteArray().decodeToString()

private fun base64ToU8(base64: String): String = newUint8Array(base64ToBytes(base64))

private fun newUint8Array(contents: ByteArray): String =
    "new Uint8Array([" + contents.joinToString(",") { it.toUByte().toString() } + "])"

/** Decodes YouTube's base64 variant (`-_.` for `+/=`), tolerating missing padding. */
private fun base64ToBytes(base64: String): ByteArray {
    var mod = base64.replace('-', '+').replace('_', '/').replace('.', '=')
    while (mod.length % 4 != 0) mod += "="
    return Base64.getDecoder().decode(mod)
}

/** Standard base64 (with padding) mapped to YouTube's base64url alphabet. */
private fun bytesToYtBase64(bytes: ByteArray): String =
    Base64.getEncoder().encodeToString(bytes).replace("+", "-").replace("/", "_")
