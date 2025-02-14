package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.NotificationType
import dev.hossain.remotenotify.model.RemoteNotification
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Worker to observe device health for supported [NotificationType] and send notification if thresholds are met.
 */
class ObserveDeviceHealthWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val batteryMonitor: BatteryMonitor,
    private val storageMonitor: StorageMonitor,
    private val repository: RemoteAlertRepository,
    private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val WORKER_LOG_TAG = "RA-Worker"
    }

    override suspend fun doWork(): Result =
        try {
            // Load all alerts from the repository
            val alerts = repository.getAllRemoteNotifications()

            // Check battery and storage levels
            val batteryLevel = batteryMonitor.getBatteryLevel()
            val availableStorage = storageMonitor.getAvailableStorageInGB()

            // Send notifications if thresholds are met
            alerts.forEach { alert ->
                when (alert) {
                    is RemoteNotification.BatteryNotification -> {
                        if (batteryLevel <= alert.batteryPercentage) {
                            sendNotification(alert)
                        }
                    }
                    is RemoteNotification.StorageNotification -> {
                        if (availableStorage <= alert.storageMinSpaceGb) {
                            sendNotification(alert)
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Timber.tag(WORKER_LOG_TAG).e(e, "Failed to observe device health")
            Result.failure()
        }

    private suspend fun sendNotification(notification: RemoteNotification) {
        if (notifiers.isEmpty()) {
            // This should ideally not happen unless dagger setup has failed
            Timber.e("No remote notifier has been registered")
        }

        notifiers
            .filter { it.hasValidConfiguration() }
            .forEach { notifier ->
                try {
                    Timber.tag(WORKER_LOG_TAG).i("Sending notification with ${notifier.notifierType}")
                    notifier.sendNotification(notification)

                    // Add some delay to avoid spamming the notification
                    delay(10_000)
                } catch (e: Exception) {
                    Timber.tag(WORKER_LOG_TAG).e(e, "Failed to send notification with ${notifier.notifierType}")
                }
            }
    }
}
