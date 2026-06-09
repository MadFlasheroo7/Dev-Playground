package pro.jayeshseth.media

/**
 * Desktop has no WebView and thus no built-in way to run YouTube's BotGuard VM, so there is no PoToken
 * provider to register. YouTube extraction will therefore fail on Desktop until an embedded-Chromium
 * (e.g. KCEF/JCEF) or JS-engine BotGuard generator is added — tracked as a follow-up. The resulting
 * extraction failure is caught upstream and shown as an inline error rather than crashing.
 */
internal actual fun installYoutubePoTokenProvider() {
    System.err.println(
        "[media] Desktop has no PoToken provider (no WebView) — YouTube stream resolution will fail. " +
            "Use the Android build, or add an embedded-Chromium BotGuard generator."
    )
}
