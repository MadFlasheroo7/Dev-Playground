package pro.jayeshseth.media

/**
 * Platform hook invoked once, right after [org.schabi.newpipe.extractor.NewPipe.init], to register a
 * **PoToken provider** with NewPipeExtractor.
 *
 * Since ~2024 YouTube gates the actual stream URLs behind a *Proof-of-Origin Token* (PoToken): a value
 * minted by running YouTube's obfuscated **BotGuard** attestation VM in a real JS/DOM environment.
 * NewPipeExtractor cannot produce one on its own — without a provider, `StreamInfo.getInfo` comes back
 * with empty stream lists (or throws `Could not get any stream`). The host app must supply one via
 * `YoutubeStreamExtractor.setPoTokenProvider(...)`.
 *
 * - **Android** ([actual] in androidMain): runs BotGuard inside a headless `WebView` — the same proven
 *   approach the official NewPipe app uses. See `YoutubePoToken.kt`.
 * - **Desktop** ([actual] in desktopMain): no-op. The JVM has no WebView, so there is no built-in way to
 *   run BotGuard; extraction will fail until a desktop generator (embedded Chromium / JS engine) is
 *   added. The failure is surfaced as an inline error, never a crash.
 */
internal expect fun installYoutubePoTokenProvider()
