package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.model.RemoteNotification

/**
 * Interface to send notification to remote destination.
 */
interface NotificationSender {
    val notifierType: NotifierType

    suspend fun sendNotification(remoteNotification: RemoteNotification)

    /**
     * Checks if all the required configuration is set for the notifier.
     */
    suspend fun hasValidConfiguration(): Boolean
}
