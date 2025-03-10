package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

/**
 * Default interval in minutes for periodic health check.
 * User can always override the value in the app settings.
 */
internal const val DEFAULT_PERIODIC_INTERVAL_MINUTES = 120L // 2 hours
internal const val DEVICE_VITALS_CHECKER_DEBUG_WORKER_ID = "onetime-debug-request"
internal const val DEVICE_VITALS_CHECKER_WORKER_ID = "periodic-health-check"

/**
 * One time request for testing purpose only.
 * @see sendPeriodicWorkRequest
 */
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

    WorkManager.getInstance(context).enqueueUniqueWork(
        uniqueWorkName = DEVICE_VITALS_CHECKER_DEBUG_WORKER_ID,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE,
        request = workRequest as OneTimeWorkRequest,
    )
}

/**
 * Schedules a periodic work request to observe device health.
 *
 * @param context The application context.
 * @param repeatIntervalMinutes The interval in minutes at which the work should repeat.
 *                              Defaults to [DEFAULT_PERIODIC_INTERVAL_MINUTES].
 */
fun sendPeriodicWorkRequest(
    context: Context,
    repeatIntervalMinutes: Long = DEFAULT_PERIODIC_INTERVAL_MINUTES,
) {
    // Ensure minimum interval of 15 minutes based on Android's WorkManager documentation.
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
