package pro.jayeshseth.devcontrols.data

import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pro.jayeshseth.devcontrols.media.StandbyBucketRecord
import java.util.concurrent.TimeUnit

/**
 * Process-wide Standby Bucket Tracker.
 * Operates independently of Compose UI screens.
 * Initialized in Application process entry point ([pro.jayeshseth.devcontrols.DevControlsApp.onCreate]).
 */
object StandbyBucketTracker {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isInitialized = false

    private val _appStandbyBucket = MutableStateFlow<String?>("Retrieving...")
    val appStandbyBucket: StateFlow<String?> = _appStandbyBucket.asStateFlow()

    private val _standbyHistoryLog = MutableStateFlow<List<StandbyBucketRecord>>(emptyList())
    val standbyHistoryLog: StateFlow<List<StandbyBucketRecord>> = _standbyHistoryLog.asStateFlow()

    private var lastBucketName: String? = null
    private var lastBucketTimestampMs: Long = System.currentTimeMillis()

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val appContext = context.applicationContext
        val prefs = StandbyBucketPrefs.get(appContext)

        scope.launch {
            prefs.historyRecords.collect { records ->
                _standbyHistoryLog.value = records
            }
        }

        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            appContext.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    val reason = when (intent?.action) {
                        Intent.ACTION_POWER_CONNECTED -> "Power Connected (Charger Plugged)"
                        Intent.ACTION_POWER_DISCONNECTED -> "Power Disconnected"
                        Intent.ACTION_USER_PRESENT -> "User Unlocked Device"
                        else -> "System Event"
                    }
                    checkAndUpdate(appContext, reason)
                }
            }, filter)
        } catch (e: Exception) {
            // Receiver registration handling
        }

        scope.launch {
            while (isActive) {
                checkAndUpdate(appContext, "Process Background Check")
                delay(30000)
            }
        }

        schedulePeriodicWorker(appContext)
    }

    fun refresh(context: Context) {
        checkAndUpdate(context.applicationContext, "Manual Refresh Check")
    }

    fun clearHistory(context: Context) {
        scope.launch {
            StandbyBucketPrefs.get(context.applicationContext).clearHistory()
        }
    }

    fun checkAndUpdate(context: Context, reason: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                val bucket = usageStats?.appStandbyBucket
                val name = when (bucket) {
                    UsageStatsManager.STANDBY_BUCKET_ACTIVE -> "Active"
                    UsageStatsManager.STANDBY_BUCKET_WORKING_SET -> "Working Set"
                    UsageStatsManager.STANDBY_BUCKET_FREQUENT -> "Frequent"
                    UsageStatsManager.STANDBY_BUCKET_RARE -> "Rare"
                    UsageStatsManager.STANDBY_BUCKET_RESTRICTED -> "Restricted"
                    null -> "Unknown"
                    else -> "Unknown ($bucket)"
                }
                _appStandbyBucket.value = name

                val now = System.currentTimeMillis()
                val isChanged = (lastBucketName != null && lastBucketName != name)
                val isExplicitTrigger = reason != "Process Background Check"

                if (isChanged || isExplicitTrigger || _standbyHistoryLog.value.isEmpty()) {
                    val durationMs = if (lastBucketTimestampMs > 0) now - lastBucketTimestampMs else 0L
                    lastBucketName = name
                    lastBucketTimestampMs = now

                    val formattedTime = java.text.SimpleDateFormat("HH:mm:ss (dd MMM)", java.util.Locale.US).format(java.util.Date(now))
                    val record = StandbyBucketRecord(
                        id = "${now}_${(1000..9999).random()}",
                        timestampMs = now,
                        formattedTime = formattedTime,
                        bucketName = name,
                        bucketId = bucket ?: -1,
                        triggerReason = reason,
                        durationInPreviousBucketMs = durationMs
                    )

                    scope.launch {
                        StandbyBucketPrefs.get(context.applicationContext).addRecord(record)
                    }
                }
            } catch (e: Exception) {
                _appStandbyBucket.value = "Error: ${e.message}"
            }
        } else {
            _appStandbyBucket.value = "Not Supported (< API 28)"
        }
    }

    private fun schedulePeriodicWorker(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<StandbyBucketWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "StandbyBucketBackgroundWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            // WorkManager enqueue fallback
        }
    }
}
