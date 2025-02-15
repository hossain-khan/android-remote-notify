package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.model.AlertMediumConfig

interface AlertMediumConfigStore {
    /**
     * Clears all configuration for the alert medium.
     */
    suspend fun clearConfig()

    suspend fun hasValidConfig(): Boolean

    suspend fun validateConfig(config: AlertMediumConfig): ConfigValidationResult
}
