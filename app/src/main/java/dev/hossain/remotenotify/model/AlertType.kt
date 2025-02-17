package dev.hossain.remotenotify.model

import androidx.annotation.DrawableRes
import dev.hossain.remotenotify.R

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

@DrawableRes
fun AlertType.toIconResId(): Int =
    when (this) {
        AlertType.BATTERY -> R.drawable.battery_3_bar_24dp
        AlertType.STORAGE -> R.drawable.hard_disk_24dp
    }
