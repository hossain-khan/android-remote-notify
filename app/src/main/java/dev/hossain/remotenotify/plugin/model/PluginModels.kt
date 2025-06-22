package dev.hossain.remotenotify.plugin.model

import androidx.annotation.Keep

/**
 * Data model representing the configuration status of a notification medium.
 * Used to inform external apps about available notification channels.
 */
@Keep
data class PluginMediumConfig(
    /**
     * The internal name of the notification medium (e.g., "email", "telegram").
     * This corresponds to the NotifierType enum values in lowercase.
     */
    val mediumName: String,
    /**
     * Human-readable display name of the medium (e.g., "Email", "Telegram").
     */
    val mediumDisplayName: String,
    /**
     * Whether this medium is properly configured with all required settings.
     * External apps should only attempt to use configured mediums.
     */
    val isConfigured: Boolean,
    /**
     * Whether this medium is currently available for sending notifications.
     * A medium might be configured but unavailable due to rate limiting,
     * network issues, or quota exhaustion.
     */
    val isAvailable: Boolean,
    /**
     * Additional configuration details in JSON format.
     * This might include rate limit information, last usage timestamp, etc.
     * External apps can use this for informational purposes.
     */
    val configDetails: String? = null,
)

/**
 * Data model representing the overall status of the plugin service.
 * Provides health and operational information to external apps.
 */
@Keep
data class PluginServiceStatus(
    /**
     * Overall service status (active, inactive, error).
     */
    val serviceStatus: String,
    /**
     * Plugin API version number.
     * External apps can use this to ensure compatibility.
     */
    val apiVersion: Int,
    /**
     * Number of notification mediums that are properly configured.
     */
    val configuredMediumsCount: Int,
    /**
     * Timestamp of the last notification sent through the plugin.
     */
    val lastNotificationTimestamp: Long,
    /**
     * Total number of notifications sent today through the plugin.
     */
    val notificationsSentToday: Int,
    /**
     * Service uptime in milliseconds.
     */
    val uptime: Long,
)

/**
 * Data model for plugin notification responses.
 * Returned to external apps after processing a notification request.
 */
@Keep
data class PluginNotificationResponse(
    /**
     * The unique request ID for tracking.
     */
    val requestId: String,
    /**
     * Whether the notification was successfully processed.
     */
    val success: Boolean,
    /**
     * Status message providing additional information.
     */
    val message: String,
    /**
     * List of mediums that were used to send the notification.
     */
    val usedMediums: List<String> = emptyList(),
    /**
     * List of mediums that failed to send the notification.
     */
    val failedMediums: List<String> = emptyList(),
    /**
     * Timestamp when the response was generated.
     */
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        /**
         * Creates a successful response.
         */
        fun success(
            requestId: String,
            message: String = "Notification sent successfully",
            usedMediums: List<String> = emptyList(),
        ): PluginNotificationResponse =
            PluginNotificationResponse(
                requestId = requestId,
                success = true,
                message = message,
                usedMediums = usedMediums,
            )

        /**
         * Creates an error response.
         */
        fun error(
            requestId: String,
            message: String,
            failedMediums: List<String> = emptyList(),
        ): PluginNotificationResponse =
            PluginNotificationResponse(
                requestId = requestId,
                success = false,
                message = message,
                failedMediums = failedMediums,
            )
    }
}
