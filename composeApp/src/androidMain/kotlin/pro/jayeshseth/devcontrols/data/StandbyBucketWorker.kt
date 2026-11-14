package pro.jayeshseth.devcontrols.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Background WorkManager worker that queries [android.app.usage.UsageStatsManager]
 * and persists standby bucket transition logs into DataStore even when the app process is terminated.
 */
class StandbyBucketWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            StandbyBucketTracker.checkAndUpdate(applicationContext, "WorkManager Background Check")
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
