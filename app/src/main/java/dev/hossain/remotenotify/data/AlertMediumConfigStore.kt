package dev.hossain.remotenotify.data

interface AlertMediumConfigStore {
    /**
     * Clears all configuration for the alert medium.
     */
    suspend fun clearConfig()

    suspend fun hasValidConfig(): Boolean

    suspend fun isValidConfig(config: AlertMediumConfig): Boolean
}
