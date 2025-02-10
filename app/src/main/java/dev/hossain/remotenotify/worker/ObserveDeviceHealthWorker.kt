package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.notifier.NotificationSender

/**
 * Worker to observe device health for supported [Type] and send notification if thresholds are met.
 */
class ObserveDeviceHealthWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val repository: RemoteAlertRepository,
    private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        // Check battery and storage levels
        // Send notifications if thresholds are met
        return Result.success()
    }
}
