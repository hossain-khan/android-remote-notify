package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.model.RemoteNotification

/**
 * Interface to send notification to remote destination.
 */
interface NotificationSender {
    val notifierType: NotifierType

    /**
     * Sends remote notification and provides result as boolean representing success or failure
     */
    suspend fun sendNotification(remoteNotification: RemoteNotification): Boolean

    /**
     * Checks if all the required configuration is set for the notifier.
     */
    suspend fun hasValidConfiguration(): Boolean

    /**
     * Clears all configuration for the notifier.
     */
    suspend fun clearConfig()
}
