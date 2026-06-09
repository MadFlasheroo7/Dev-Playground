package pro.jayeshseth.media

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import java.time.Instant

/**
 * # Android PoToken provider (BotGuard via WebView)
 *
 * YouTube requires a **Proof-of-Origin Token (poToken)** to serve real stream URLs. A poToken is minted
 * by running YouTube's obfuscated **BotGuard** attestation VM — which only runs in a genuine JS/DOM
 * environment. The proven approach (used by the official NewPipe app) is to host a tiny headless
 * [WebView], load YouTube's BotGuard interpreter into it, and call it to mint tokens.
 *
 * This file is a faithful Kotlin-coroutine port of NewPipe's `PoTokenWebView` + `PoTokenProviderImpl`
 * (originally RxJava). The end-to-end flow:
 *
 * 1. **Init** ([PoTokenWebView.newGenerator]) — load [PO_TOKEN_HTML] into a WebView, then:
 *    a. POST `/Create` → a "challenge" (the BotGuard program + interpreter JS), parsed by
 *       [parseChallengeData] and fed to the JS `runBotGuard()`, which runs the VM and returns a
 *       `botguardResponse` + `webPoSignalOutput`.
 *    b. POST `/GenerateIT` with that response → an `integrityToken` (+ a lifetime), parsed by
 *       [parseIntegrityTokenData] and stashed in the JS context.
 * 2. **Mint** ([PoTokenWebView.generatePoToken]) — call the JS `obtainPoToken(signal, integrity, id)`
 *    for a given identifier (the visitorData for the "streaming" token, the videoId for the "player"
 *    token) and base64url-encode the resulting bytes.
 *
 * The generator is registered with the extractor via [installYoutubePoTokenProvider] →
 * `YoutubeStreamExtractor.setPoTokenProvider`. All failures degrade gracefully: the provider returns
 * `null` (extractor falls back / surfaces an empty-stream error) rather than crashing.
 */

internal class PoTokenException(message: String) : Exception(message)

/** Thrown when the system WebView is too old/broken to run the BotGuard JS (e.g. a syntax error). */
internal class BadWebViewException(message: String) : Exception(message)

internal fun buildExceptionForJsError(error: String): Exception =
    if (error.contains("SyntaxError")) BadWebViewException(error) else PoTokenException(error)

/**
 * A single headless WebView that has loaded BotGuard and holds a live `integrityToken`. Reused to mint
 * many poTokens until [isExpired]. Construct via [newGenerator] only (it guarantees main-thread setup).
 */
@SuppressLint("SetJavaScriptEnabled")
internal class PoTokenWebView private constructor(
    context: Context,
    private val initDeferred: CompletableDeferred<PoTokenWebView>,
) {
    private val webView = WebView(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val deferredsLock = Any()
    private val poTokenDeferreds = HashMap<String, CompletableDeferred<String>>()
    private lateinit var expirationInstant: Instant

    init {
        with(webView.settings) {
            javaScriptEnabled = true
            userAgentString = USER_AGENT
            blockNetworkLoads = true // the WebView itself never needs the network; we do all I/O
        }
        webView.addJavascriptInterface(this, JS_INTERFACE)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(m: ConsoleMessage): Boolean {
                if (m.message().contains("Uncaught")) {
                    // Everything fallible in the JS is wrapped in try/catch, so an uncaught error means
                    // the WebView's JS engine itself is too old to parse the BotGuard code.
                    failInit(BadWebViewException("\"${m.message()}\", source: ${m.sourceId()} (${m.lineNumber()})"))
                }
                return super.onConsoleMessage(m)
            }
        }
    }

    private fun loadHtmlAndObtainBotguard() {
        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
            // append a call so BotGuard download kicks off once the page is parsed
            PO_TOKEN_HTML.replaceFirst("</script>", "\n$JS_INTERFACE.downloadAndRunBotguard()</script>"),
            "text/html",
            "utf-8",
            null,
        )
    }

    //region JS bridge — methods below are invoked by the WebView on its own (background) JS thread
    @JavascriptInterface
    fun downloadAndRunBotguard() {
        botguardRequest(CREATE_URL, "[ \"$REQUEST_KEY\" ]") { body ->
            val parsedChallengeData = parseChallengeData(body)
            webView.evaluateJavascript(
                """try {
                    data = $parsedChallengeData
                    runBotGuard(data).then(function (result) {
                        this.webPoSignalOutput = result.webPoSignalOutput
                        $JS_INTERFACE.onRunBotguardResult(result.botguardResponse)
                    }, function (error) {
                        $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                    })
                } catch (error) {
                    $JS_INTERFACE.onJsInitializationError(error + "\n" + error.stack)
                }""",
                null,
            )
        }
    }

    @JavascriptInterface
    fun onJsInitializationError(error: String) {
        Log.e(TAG, "Initialization error from JavaScript: $error")
        failInit(buildExceptionForJsError(error))
    }

    @JavascriptInterface
    fun onRunBotguardResult(botguardResponse: String) {
        botguardRequest(GENERATE_IT_URL, "[ \"$REQUEST_KEY\", \"$botguardResponse\" ]") { body ->
            val (integrityToken, expirationSeconds) = parseIntegrityTokenData(body)
            // leave a 10-minute safety margin before the real expiry
            expirationInstant = Instant.now().plusSeconds(expirationSeconds - 600)
            webView.evaluateJavascript("this.integrityToken = $integrityToken") {
                Log.d(TAG, "PoToken init finished (expires in ${expirationSeconds}s)")
                initDeferred.complete(this)
            }
        }
    }

    @JavascriptInterface
    fun onObtainPoTokenResult(identifier: String, poTokenU8: String) {
        val deferred = popDeferred(identifier) ?: return
        runCatching { u8ToBase64(poTokenU8) }
            .onSuccess { deferred.complete(it) }
            .onFailure { deferred.completeExceptionally(it) }
    }

    @JavascriptInterface
    fun onObtainPoTokenError(identifier: String, error: String) {
        Log.e(TAG, "obtainPoToken error from JavaScript: $error")
        popDeferred(identifier)?.completeExceptionally(buildExceptionForJsError(error))
    }
    //endregion

    /** Mints a poToken for [identifier]. Safe to call repeatedly while not [isExpired]. */
    suspend fun generatePoToken(identifier: String): String {
        val deferred = CompletableDeferred<String>()
        synchronized(deferredsLock) { poTokenDeferreds[identifier] = deferred }
        runOnMain {
            val u8Identifier = stringToU8(identifier)
            webView.evaluateJavascript(
                """try {
                        identifier = "$identifier"
                        u8Identifier = $u8Identifier
                        poTokenU8 = obtainPoToken(webPoSignalOutput, integrityToken, u8Identifier)
                        poTokenU8String = ""
                        for (i = 0; i < poTokenU8.length; i++) {
                            if (i != 0) poTokenU8String += ","
                            poTokenU8String += poTokenU8[i]
                        }
                        $JS_INTERFACE.onObtainPoTokenResult(identifier, poTokenU8String)
                    } catch (error) {
                        $JS_INTERFACE.onObtainPoTokenError(identifier, error + "\n" + error.stack)
                    }""",
                null,
            )
        }
        return deferred.await()
    }

    fun isExpired(): Boolean = ::expirationInstant.isInitialized && Instant.now().isAfter(expirationInstant)

    fun close() = runOnMain {
        runCatching {
            webView.clearHistory()
            webView.clearCache(true)
            webView.loadUrl("about:blank")
            webView.onPause()
            webView.removeAllViews()
            webView.destroy()
        }
    }

    //region internals
    /** POSTs [data] to a BotGuard endpoint on a background thread, then runs [onBody] on the main thread. */
    private fun botguardRequest(url: String, data: String, onBody: (String) -> Unit) {
        Thread {
            try {
                val response = NewPipe.getDownloader().post(url, BOTGUARD_HEADERS, data.toByteArray())
                val code = response.responseCode()
                if (code != 200) {
                    failInit(PoTokenException("Invalid response code: $code"))
                    return@Thread
                }
                runOnMain { onBody(response.responseBody()) }
            } catch (t: Throwable) {
                failInit(t)
            }
        }.apply { isDaemon = true }.start()
    }

    private fun failInit(error: Throwable) {
        if (!initDeferred.isCompleted) initDeferred.completeExceptionally(error)
        popAllDeferreds().forEach { it.completeExceptionally(error) }
        close()
    }

    private fun popDeferred(identifier: String): CompletableDeferred<String>? =
        synchronized(deferredsLock) { poTokenDeferreds.remove(identifier) }

    private fun popAllDeferreds(): List<CompletableDeferred<String>> =
        synchronized(deferredsLock) { poTokenDeferreds.values.toList().also { poTokenDeferreds.clear() } }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }
    //endregion

    companion object {
        private const val TAG = "PoTokenWebView"
        private const val JS_INTERFACE = "PoTokenWebView"
        private const val CREATE_URL = "https://www.youtube.com/api/jnn/v1/Create"
        private const val GENERATE_IT_URL = "https://www.youtube.com/api/jnn/v1/GenerateIT"

        // public BotGuard request key + Google API key, observed from real BotGuard traffic
        private const val REQUEST_KEY = "O43z0dpjhgX20SCx4KAo"
        private const val GOOGLE_API_KEY = "AIzaSyDyT5W0Jh49F30Pqqtyfdf7pDLFKLJoAnw"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.3"

        private val BOTGUARD_HEADERS = mapOf(
            "User-Agent" to listOf(USER_AGENT),
            "Accept" to listOf("application/json"),
            "Content-Type" to listOf("application/json+protobuf"),
            "x-goog-api-key" to listOf(GOOGLE_API_KEY),
            "x-user-agent" to listOf("grpc-web-javascript/0.1"),
        )

        /** Builds a generator on the main thread, runs BotGuard, and suspends until an integrityToken is ready. */
        suspend fun newGenerator(context: Context): PoTokenWebView {
            val deferred = CompletableDeferred<PoTokenWebView>()
            val posted = Handler(Looper.getMainLooper()).post {
                try {
                    PoTokenWebView(context.applicationContext, deferred).loadHtmlAndObtainBotguard()
                } catch (t: Throwable) {
                    deferred.completeExceptionally(t)
                }
            }
            if (!posted) deferred.completeExceptionally(PoTokenException("Could not run on main thread"))
            return deferred.await()
        }
    }
}

/**
 * The [PoTokenProvider] handed to NewPipeExtractor. Supplies only the **web** client token (the one
 * gating stream URLs); the android/ios/embed tokens are left null, matching NewPipe's own impl.
 *
 * A single [PoTokenWebView] generator is cached and reused: per the BotGuard protocol exactly one
 * "streaming" token (keyed by visitorData) must be minted before any per-video "player" tokens. The
 * synchronized block guarantees that ordering; player tokens for different videos can then be minted
 * freely. On any failure we recreate once, else give up and return null.
 */
internal object AndroidWebViewPoTokenProvider : PoTokenProvider {
    private const val TAG = "PoTokenProvider"
    private val lock = Any()
    private var generator: PoTokenWebView? = null
    private var visitorData: String? = null
    private var streamingPot: String? = null
    @Volatile private var webViewBad = false

    override fun getWebClientPoToken(videoId: String): PoTokenResult? {
        val context = MediaContextHolder.app ?: return null
        if (webViewBad) return null
        return try {
            obtain(videoId, forceRecreate = false)
        } catch (e: BadWebViewException) {
            Log.e(TAG, "WebView cannot run BotGuard — disabling poToken", e)
            webViewBad = true
            null
        } catch (t: Throwable) {
            Log.e(TAG, "Could not obtain poToken for $videoId", t)
            null
        }
    }

    override fun getWebEmbedClientPoToken(videoId: String): PoTokenResult? = null
    override fun getAndroidClientPoToken(videoId: String): PoTokenResult? = null
    override fun getIosClientPoToken(videoId: String): PoTokenResult? = null

    private fun obtain(videoId: String, forceRecreate: Boolean): PoTokenResult {
        val context = MediaContextHolder.app!!
        val gen: PoTokenWebView
        val vd: String
        val spot: String
        val recreated: Boolean

        synchronized(lock) {
            recreated = generator == null || forceRecreate || generator!!.isExpired()
            if (recreated) {
                val info = InnertubeClientRequestInfo.ofWebClient()
                info.clientInfo.clientVersion = YoutubeParsingHelper.getClientVersion()
                visitorData = YoutubeParsingHelper.getVisitorDataFromInnertube(
                    info,
                    NewPipe.getPreferredLocalization(),
                    NewPipe.getPreferredContentCountry(),
                    YoutubeParsingHelper.getYouTubeHeaders(),
                    YoutubeParsingHelper.YOUTUBEI_V1_URL,
                    null,
                    false,
                )
                generator?.close()
                generator = runBlocking { PoTokenWebView.newGenerator(context) }
                // exactly one streaming token, before any player tokens
                streamingPot = runBlocking { generator!!.generatePoToken(visitorData!!) }
            }
            gen = generator!!
            vd = visitorData!!
            spot = streamingPot!!
        }

        val playerPot = try {
            runBlocking { gen.generatePoToken(videoId) }
        } catch (t: Throwable) {
            if (recreated) throw t // already fresh; nothing more to try
            Log.e(TAG, "Failed to obtain poToken, recreating generator", t)
            return obtain(videoId, forceRecreate = true)
        }

        return PoTokenResult(vd, playerPot, spot)
    }
}

/** Android [actual]: register the WebView BotGuard provider with NewPipeExtractor. */
internal actual fun installYoutubePoTokenProvider() {
    YoutubeStreamExtractor.setPoTokenProvider(AndroidWebViewPoTokenProvider)
}

/**
 * Minimal HTML hosting YouTube's BotGuard bridge functions (`runBotGuard`, `obtainPoToken`, etc.),
 * loaded into the headless WebView. Ported verbatim from NewPipe's `po_token.html` asset.
 */
private val PO_TOKEN_HTML = """
<!DOCTYPE html>
<html lang="en"><head><title></title><script>
function loadBotGuard(challengeData) {
  this.vm = this[challengeData.globalName];
  this.program = challengeData.program;
  this.vmFunctions = {};
  this.syncSnapshotFunction = null;

  if (!this.vm)
    throw new Error('[BotGuardClient]: VM not found in the global object');
  if (!this.vm.a)
    throw new Error('[BotGuardClient]: Could not load program');

  const vmFunctionsCallback = function (asyncSnapshotFunction, shutdownFunction, passEventFunction, checkCameraFunction) {
    this.vmFunctions = {
      asyncSnapshotFunction: asyncSnapshotFunction,
      shutdownFunction: shutdownFunction,
      passEventFunction: passEventFunction,
      checkCameraFunction: checkCameraFunction
    };
  };

  this.syncSnapshotFunction = this.vm.a(this.program, vmFunctionsCallback, true, this.userInteractionElement, function () {}, [ [], [] ])[0]

  return new Promise(function (resolve, reject) {
    i = 0
    refreshIntervalId = setInterval(function () {
      if (!!this.vmFunctions.asyncSnapshotFunction) {
        resolve(this)
        clearInterval(refreshIntervalId);
      }
      if (i >= 10000) {
        reject("asyncSnapshotFunction is null even after 10 seconds")
        clearInterval(refreshIntervalId);
      }
      i += 1;
    }, 1);
  })
}

function snapshot(args) {
  return new Promise(function (resolve, reject) {
    if (!this.vmFunctions.asyncSnapshotFunction)
      return reject(new Error('[BotGuardClient]: Async snapshot function not found'));
    this.vmFunctions.asyncSnapshotFunction(function (response) { resolve(response) }, [
      args.contentBinding,
      args.signedTimestamp,
      args.webPoSignalOutput,
      args.skipPrivacyBuffer
    ]);
  });
}

function runBotGuard(challengeData) {
  const interpreterJavascript = challengeData.interpreterJavascript.privateDoNotAccessOrElseSafeScriptWrappedValue;
  if (interpreterJavascript) {
    new Function(interpreterJavascript)();
  } else throw new Error('Could not load VM');

  const webPoSignalOutput = [];
  return loadBotGuard({
    globalName: challengeData.globalName,
    globalObj: this,
    program: challengeData.program
  }).then(function (botguard) {
    return botguard.snapshot({ webPoSignalOutput: webPoSignalOutput })
  }).then(function (botguardResponse) {
    return { webPoSignalOutput: webPoSignalOutput, botguardResponse: botguardResponse }
  })
}

function obtainPoToken(webPoSignalOutput, integrityToken, identifier) {
  const getMinter = webPoSignalOutput[0];
  if (!getMinter)
    throw new Error('PMD:Undefined');
  const mintCallback = getMinter(integrityToken);
  if (!(mintCallback instanceof Function))
    throw new Error('APF:Failed');
  const result = mintCallback(identifier);
  if (!result)
    throw new Error('YNJ:Undefined');
  if (!(result instanceof Uint8Array))
    throw new Error('ODM:Invalid');
  return result;
}
</script></head><body></body></html>
""".trimIndent()
