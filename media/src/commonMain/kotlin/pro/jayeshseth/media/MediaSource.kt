package pro.jayeshseth.media

/**
 * A YouTube media source: search, and resolve an item to playable stream URLs.
 *
 * Two backends implement this behind [createMediaSource]:
 *  - **JVM (Android + Desktop):** NewPipeExtractor — extracts directly from YouTube. It executes
 *    YouTube's player JS (signature/nsig cipher) via Mozilla Rhino and parses HTML/JSON with Jsoup,
 *    so it only runs on a JVM (not Kotlin/Native or Wasm).
 *  - **iOS + Web (later):** a Piped REST instance — Piped *is* NewPipeExtractor running server-side,
 *    so the client just makes HTTPS calls (works on any platform).
 *
 * Both [search] and [resolve] are suspending network calls and may throw (YouTube changes, throttling,
 * region blocks). Callers should run them off the main thread and surface failures to the user — the
 * extraction layer is inherently a moving target.
 */
interface MediaSource {
    /** Search YouTube for [query]; returns the top results as [MediaItem]s. */
    suspend fun search(query: String): List<MediaItem>

    /** Resolve [MediaItem.id] to its playable audio/video [Track]s. */
    suspend fun resolve(id: String): StreamInfo
}

/** Returns the platform's [MediaSource] (NewPipe on JVM; Piped on iOS/Web). */
expect fun createMediaSource(): MediaSource
