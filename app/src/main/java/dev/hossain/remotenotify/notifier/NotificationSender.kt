package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.RemoteAlert

/**
 * Interface to send notification to remote destinations like email, sms, telegram and so on.
 *
 * @see NotifierType
 */
interface NotificationSender {
    val notifierType: NotifierType

    /**
     * Sends remote notification and provides result as boolean representing success or failure
     */
    suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean

    /**
     * Checks if all the required configuration is set for the notifier.
     */
    suspend fun hasValidConfig(): Boolean

    /**
     * Saves the configuration for the notifier.
     */
    suspend fun saveConfig(alertMediumConfig: AlertMediumConfig)

    /**
     * Retrieves the current configuration for the notifier.
     */
    suspend fun getConfig(): AlertMediumConfig

    /**
     * Clears all configuration for the notifier.
     */
    suspend fun clearConfig()

    /**
     * Validates the configuration for the notifier.
     */
    suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult
}

/**
 * Extension function to find a [NotificationSender] from a set by its [NotifierType].
 *
 * @param senderNotifierType The type of the notifier to find.
 * @return The [NotificationSender] matching the given [NotifierType].
 * @throws IllegalArgumentException if no sender is found for the given [NotifierType].
 */
fun Set<@JvmSuppressWildcards NotificationSender>.of(senderNotifierType: NotifierType): NotificationSender =
    requireNotNull(find { it.notifierType == senderNotifierType }) {
        "Sender for notifier type not found: $senderNotifierType"
    }
