package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

internal const val DEFAULT_PERIODIC_INTERVAL_MINUTES = 60L

fun sendOneTimeWorkRequest(context: Context) {
    val workRequest: WorkRequest =
        OneTimeWorkRequestBuilder<ObserveDeviceHealthWorker>()
            .setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .setRequiresStorageNotLow(true)
                    .build(),
            ).addTag("onetime-test-request")
            .build()

    WorkManager.getInstance(context).enqueue(workRequest)
}

fun sendPeriodicWorkRequest(
    context: Context,
    repeatIntervalMinutes: Long = DEFAULT_PERIODIC_INTERVAL_MINUTES,
) {
    // Ensure minimum interval is respected (15 minutes as per WorkManager constraints)
    val intervalMinutes = repeatIntervalMinutes.coerceAtLeast(15)

    val workRequest =
        PeriodicWorkRequestBuilder<ObserveDeviceHealthWorker>(
            repeatInterval = intervalMinutes,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
        ).setConstraints(
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresDeviceIdle(true)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build(),
        ).addTag("periodic-health-check")
            .build()

    WorkManager
        .getInstance(context)
        .enqueueUniquePeriodicWork(
            uniqueWorkName = "periodic-health-check",
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
            request = workRequest,
        )
}
