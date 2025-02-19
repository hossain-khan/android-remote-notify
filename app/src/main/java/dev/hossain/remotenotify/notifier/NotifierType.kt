package dev.hossain.remotenotify.notifier

/**
 * List of supported medium to send alert notification.
 */
enum class NotifierType(
    val displayName: String,
) {
    EMAIL("Email"),
    TELEGRAM("Telegram"),
    TWILIO("Twilio"),
    WEBHOOK_REST_API("Webhook"),
}
