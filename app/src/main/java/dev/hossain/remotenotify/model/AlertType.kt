package dev.hossain.remotenotify.model

/**
 * Enum class representing the types of device metrics that can trigger an alert.
 */
enum class AlertType {
    /**
     * Notification type for battery-related alerts.
     */
    BATTERY,

    /**
     * Notification type for storage-related alerts.
     */
    STORAGE,
}
