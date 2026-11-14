package pro.jayeshseth.devcontrols.sdui.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import pro.jayeshseth.devcontrols.sdui.model.SduiDocument

/**
 * Result state emitted by [SduiSwrCache].
 */
sealed interface SduiResource<out T> {
    object Loading : SduiResource<Nothing>
    data class Success<T>(val data: T, val isStale: Boolean) : SduiResource<T>
    data class Error<T>(val message: String, val fallback: T? = null) : SduiResource<T>
}

/**
 * Cached entry container with expiration and ETag support.
 */
data class SduiCacheEntry(
    val document: SduiDocument,
    val cachedAtMillis: Long,
    val ttlMillis: Long,
    val etag: String? = null
) {
    fun isExpired(nowMillis: Long = Clock.System.now().toEpochMilliseconds()): Boolean {
        return (nowMillis - cachedAtMillis) > ttlMillis
    }
}

/**
 * Stale-While-Revalidate (SWR) cache for Server-Driven UI schemas.
 * Guarantees zero-latency local render from cache while concurrently fetching
 * fresh updates in the background.
 */
class SduiSwrCache {
    private val memoryCache = mutableMapOf<String, SduiCacheEntry>()

    fun put(
        key: String,
        document: SduiDocument,
        ttlMillis: Long = 300_000L, // 5 minutes default
        etag: String? = null
    ) {
        val entry = SduiCacheEntry(
            document = document,
            cachedAtMillis = Clock.System.now().toEpochMilliseconds(),
            ttlMillis = ttlMillis,
            etag = etag
        )
        memoryCache[key] = entry
    }

    fun get(key: String): SduiCacheEntry? = memoryCache[key]

    fun remove(key: String) {
        memoryCache.remove(key)
    }

    fun clear() {
        memoryCache.clear()
    }

    /**
     * SWR Flow:
     * 1. Emits cached version immediately if present (marked as stale or fresh).
     * 2. If no cache or if stale, invokes [fetcher] in background.
     * 3. Emits fresh result and updates cache.
     * 4. If network fails and cached data was available, retains cache gracefully.
     */
    fun loadWithSwr(
        key: String,
        ttlMillis: Long = 300_000L,
        fetcher: suspend () -> SduiDocument
    ): Flow<SduiResource<SduiDocument>> = flow {
        val cached = get(key)
        val now = Clock.System.now().toEpochMilliseconds()

        if (cached != null) {
            val isStale = cached.isExpired(now)
            emit(SduiResource.Success(cached.document, isStale = isStale))
            if (!isStale) {
                return@flow
            }
        } else {
            emit(SduiResource.Loading)
        }

        try {
            val freshDoc = fetcher()
            put(key, freshDoc, ttlMillis)
            emit(SduiResource.Success(freshDoc, isStale = false))
        } catch (e: Exception) {
            emit(
                SduiResource.Error(
                    message = e.message ?: "Failed to fetch remote SDUI schema",
                    fallback = cached?.document
                )
            )
        }
    }

    companion object {
        val default = SduiSwrCache()
    }
}
