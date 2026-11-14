package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pro.jayeshseth.devcontrols.media.StandbyBucketRecord

private val Context.standbyBucketDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "standby_bucket_history_prefs")

/**
 * DataStore helper for persisting Standby Bucket transition records.
 * Maintains up to [MAX_RECORDS] historical records across app restarts.
 */
class StandbyBucketPrefs private constructor(private val context: Context) {

    private val store get() = context.standbyBucketDataStore

    /** Flow emitting all saved standby bucket transition records ordered newest first. */
    val historyRecords: Flow<List<StandbyBucketRecord>> = store.data.map { prefs ->
        val rawJson = prefs[KEY_HISTORY_JSON] ?: ""
        deserializeRecords(rawJson)
    }

    /** Appends a new transition record and retains up to [MAX_RECORDS] entries. */
    suspend fun addRecord(record: StandbyBucketRecord) {
        store.edit { prefs ->
            val currentRaw = prefs[KEY_HISTORY_JSON] ?: ""
            val currentList = deserializeRecords(currentRaw)
            val updatedList = (listOf(record) + currentList).take(MAX_RECORDS)
            prefs[KEY_HISTORY_JSON] = serializeRecords(updatedList)
        }
    }

    /** Clears all historical transition records. */
    suspend fun clearHistory() {
        store.edit { prefs ->
            prefs.remove(KEY_HISTORY_JSON)
        }
    }

    private fun serializeRecords(records: List<StandbyBucketRecord>): String {
        return records.joinToString(separator = "|||") { r ->
            "${r.id}:::${r.timestampMs}:::${escape(r.formattedTime)}:::${escape(r.bucketName)}:::${r.bucketId}:::${escape(r.triggerReason)}:::${r.durationInPreviousBucketMs}"
        }
    }

    private fun deserializeRecords(raw: String): List<StandbyBucketRecord> {
        if (raw.isBlank()) return emptyList()
        return raw.split("|||").mapNotNull { entryStr ->
            val parts = entryStr.split(":::")
            if (parts.size >= 7) {
                try {
                    StandbyBucketRecord(
                        id = parts[0],
                        timestampMs = parts[1].toLongOrNull() ?: 0L,
                        formattedTime = unescape(parts[2]),
                        bucketName = unescape(parts[3]),
                        bucketId = parts[4].toIntOrNull() ?: 0,
                        triggerReason = unescape(parts[5]),
                        durationInPreviousBucketMs = parts[6].toLongOrNull() ?: 0L
                    )
                } catch (e: Exception) {
                    null
                }
            } else null
        }
    }

    private fun escape(s: String): String = s.replace(":", "%3A").replace("|", "%7C")
    private fun unescape(s: String): String = s.replace("%3A", ":").replace("%7C", "|")

    companion object {
        private const val MAX_RECORDS = 50
        private val KEY_HISTORY_JSON = stringPreferencesKey("standby_history_records_v1")

        @Volatile private var instance: StandbyBucketPrefs? = null

        fun get(context: Context): StandbyBucketPrefs =
            instance ?: synchronized(this) {
                instance ?: StandbyBucketPrefs(context.applicationContext).also { instance = it }
            }
    }
}
