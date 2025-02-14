package dev.hossain.remotenotify.data

interface AlertMediumConfigStore {
    /**
     * Clears all configuration for the alert medium.
     */
    suspend fun clearConfig()
}
