package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.model.AlertMediumConfig

/**
 * Interface for storing and validating notification-channel configuration.
 *
 * Each notification channel (e.g., Email, Telegram, Twilio) provides its own implementation
 * that persists configuration to DataStore and exposes validation helpers.
 *
 * @see dev.hossain.remotenotify.notifier.NotificationSender
 */
interface AlertMediumConfigStore {
    /**
     * Clears all configuration for the alert medium.
     */
    suspend fun clearConfig()

    /**
     * Returns `true` if the current saved configuration is complete and ready to use.
     */
    suspend fun hasValidConfig(): Boolean

    /**
     * Validates the provided [config] and returns a [ConfigValidationResult] describing
     * any errors or confirming the configuration is valid.
     */
    suspend fun validateConfig(config: AlertMediumConfig): ConfigValidationResult
}
