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
internal const val DEVICE_VITALS_CHECKER_WORKER_ID = "periodic-health-check"

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
    val intervalMinutes = repeatIntervalMinutes.coerceAtLeast(15)

    val workRequest =
        PeriodicWorkRequestBuilder<ObserveDeviceHealthWorker>(
            repeatInterval = intervalMinutes,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
            // Add some flex interval to help with battery optimization
            flexTimeInterval = (intervalMinutes / 2).coerceAtLeast(5),
            flexTimeIntervalUnit = TimeUnit.MINUTES,
        ).setConstraints(
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                // Don't add the device idle requirement
                .setRequiresBatteryNotLow(false)
                .setRequiresStorageNotLow(false)
                .build(),
        ).build()

    WorkManager
        .getInstance(context)
        .enqueueUniquePeriodicWork(
            uniqueWorkName = DEVICE_VITALS_CHECKER_WORKER_ID,
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.UPDATE,
            request = workRequest,
        )
}
