package dev.hossain.remotenotify.model

import androidx.annotation.Keep

/**
 * Defines when a configured alert sends a remote notification.
 */
@Keep
enum class AlertMode {
    /**
     * Send only when the configured threshold condition is met.
     */
    THRESHOLD,

    /**
     * Send the current metric every time the periodic worker runs.
     */
    PERIODIC,
}
