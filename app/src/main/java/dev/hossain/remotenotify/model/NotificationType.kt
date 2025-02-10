package dev.hossain.remotenotify.model

/**
 * Enum class representing the types of device metrics that can trigger a notification.
 */
enum class NotificationType {
    /**
     * Notification type for battery-related alerts.
     */
    BATTERY,

    /**
     * Notification type for storage-related alerts.
     */
    STORAGE,
}
