package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Worker to observe device health for supported [AlertType] and send notification if thresholds are met.
 */
class ObserveDeviceHealthWorker(
    context: Context,
    workerParams: WorkerParameters,
    private val batteryMonitor: BatteryMonitor,
    private val storageMonitor: StorageMonitor,
    private val repository: RemoteAlertRepository,
    private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
    private val analytics: Analytics,
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val WORKER_LOG_TAG = "RA-Worker"

        /**
         * Key to store last run timestamp in work data that is shown in the work info.
         * @see dev.hossain.remotenotify.model.WorkerStatus
         */
        internal const val WORK_DATA_KEY_LAST_RUN_TIMESTAMP_MS = "last_run_timestamp_ms"
    }

    override suspend fun doWork(): Result {
        Timber.tag(WORKER_LOG_TAG).i("Schedule worker has started the job.")
        try {
            // Load all alerts from the repository
            val userConfiguredAlerts = repository.getAllRemoteAlert()
            Timber.tag(WORKER_LOG_TAG).d("Loaded alerts: $userConfiguredAlerts")

            // Log worker job started with alerts count
            analytics.logWorkerJob(
                interval = 0, // You might want to pass the actual interval if available
                alertsCount = userConfiguredAlerts.size.toLong(),
            )

            // Check battery and storage levels
            val deviceCurrentBatteryLevel = batteryMonitor.getBatteryLevel()
            Timber.tag(WORKER_LOG_TAG).d("Battery level: $deviceCurrentBatteryLevel")

            val deviceCurrentAvailableStorage = storageMonitor.getAvailableStorageInGB()
            Timber.tag(WORKER_LOG_TAG).d("Available storage: $deviceCurrentAvailableStorage")

            // Send notifications if thresholds are met
            userConfiguredAlerts.forEach { userConfiguredAlert: RemoteAlert ->
                val lastAlertLog: AlertCheckLog? = repository.getLatestCheckForAlert(userConfiguredAlert.alertId).first()

                // Skip if last alert was triggered within 24 hours
                if (lastAlertLog?.isAlertSent == true) {
                    val hoursSinceLastAlert = (System.currentTimeMillis() - lastAlertLog.checkedOn) / (1000 * 60 * 60)
                    if (hoursSinceLastAlert < 24) {
                        // NOTE: This is to avoid spamming the notification
                        // If the 24 hour limit changes, make sure to update `Add Alert` screen to reflect that.
                        Timber.tag(WORKER_LOG_TAG).d("Skipping alert: Last notification was sent $hoursSinceLastAlert hours ago")
                        return@forEach
                    }
                }

                when (userConfiguredAlert) {
                    is RemoteAlert.BatteryAlert -> {
                        val triggered = deviceCurrentBatteryLevel <= userConfiguredAlert.batteryPercentage
                        checkAndProcessAlert(
                            userConfiguredAlert = userConfiguredAlert,
                            triggered = triggered,
                            alertType = AlertType.BATTERY,
                            stateValue = deviceCurrentBatteryLevel,
                        )
                    }
                    is RemoteAlert.StorageAlert -> {
                        val triggered = deviceCurrentAvailableStorage <= userConfiguredAlert.storageMinSpaceGb
                        checkAndProcessAlert(
                            userConfiguredAlert = userConfiguredAlert,
                            triggered = triggered,
                            alertType = AlertType.STORAGE,
                            stateValue = deviceCurrentAvailableStorage.toInt(),
                        )
                    }
                }
            }

            // Add tag to track this work
            setProgress(workDataOf(WORK_DATA_KEY_LAST_RUN_TIMESTAMP_MS to System.currentTimeMillis()))

            // Log successful completion
            analytics.logWorkSuccess()

            return Result.success()
        } catch (e: Exception) {
            Timber.tag(WORKER_LOG_TAG).e(e, "Failed to observe device health")

            // Log failure
            analytics.logWorkFailed(notifierType = null, exception = e)

            return Result.failure()
        }
    }

    private suspend fun checkAndProcessAlert(
        userConfiguredAlert: RemoteAlert,
        triggered: Boolean,
        alertType: AlertType,
        stateValue: Int,
    ) {
        if (triggered) {
            Timber.tag(WORKER_LOG_TAG).d("Notification threshold met. Sending: $userConfiguredAlert for $alertType")
            sendNotification(userConfiguredAlert, alertType, stateValue)
        } else {
            Timber.tag(WORKER_LOG_TAG).d("Notification threshold not met. Not sending: $userConfiguredAlert for $alertType")
            saveAlertCheckLog(userConfiguredAlert.alertId, alertType, stateValue, false, null)
        }
    }

    private suspend fun sendNotification(
        remoteAlert: RemoteAlert,
        alertType: AlertType,
        stateValue: Int,
    ) {
        Timber.i("Notification triggered - sending: $remoteAlert")
        if (notifiers.isEmpty()) {
            // This should ideally not happen unless dagger setup has failed
            Timber.tag(WORKER_LOG_TAG).e("No remote notifier has been registered")
        }

        notifiers
            .filter { it.hasValidConfig() }
            .forEach { notifier ->
                try {
                    Timber.tag(WORKER_LOG_TAG).i("Sending notification with ${notifier.notifierType}")

                    // Update the alert with the current state value for the user notification
                    val triggeredRemoteAlert =
                        when (remoteAlert) {
                            is RemoteAlert.BatteryAlert ->
                                RemoteAlert.BatteryAlert(
                                    alertId = remoteAlert.alertId,
                                    batteryPercentage = stateValue,
                                )
                            is RemoteAlert.StorageAlert ->
                                RemoteAlert.StorageAlert(
                                    alertId = remoteAlert.alertId,
                                    storageMinSpaceGb = remoteAlert.storageMinSpaceGb,
                                    currentStorageGb = stateValue.toDouble(),
                                )
                        }

                    kotlin
                        .runCatching { notifier.sendNotification(triggeredRemoteAlert) }
                        .onFailure { error: Throwable ->
                            Timber.tag(WORKER_LOG_TAG).e(error)

                            // Log when alert is failed to send
                            analytics.logWorkFailed(notifier.notifierType, error)
                        }.onSuccess {
                            saveAlertCheckLog(remoteAlert.alertId, alertType, stateValue, true, notifier.notifierType)
                            Timber.tag(WORKER_LOG_TAG).d("Notifier status: $it")

                            // Log when alert is successfully sent
                            analytics.logAlertSent(alertType, notifier.notifierType)
                        }

                    // Add some delay to avoid spamming the notification
                    delay(10_000)
                } catch (e: Exception) {
                    Timber.tag(WORKER_LOG_TAG).e(e, "Failed to send notification with ${notifier.notifierType}")
                    analytics.logWorkFailed(notifier.notifierType, e)
                }
            }
    }

    private suspend fun saveAlertCheckLog(
        alertId: Long,
        alertType: AlertType,
        stateValue: Int,
        alertSent: Boolean,
        notifierType: NotifierType?,
    ) {
        Timber.tag(WORKER_LOG_TAG).i("Saving alert check log for $alertId")
        repository.insertAlertCheckLog(
            alertId = alertId,
            alertType = alertType,
            alertStateValue = stateValue,
            alertTriggered = alertSent,
            notifierType = notifierType,
        )
    }
}
