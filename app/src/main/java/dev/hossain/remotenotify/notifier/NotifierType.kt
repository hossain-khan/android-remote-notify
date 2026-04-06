package dev.hossain.remotenotify.notifier

import androidx.annotation.Keep

/**
 * List of supported medium to send alert notification.
 *
 * ℹ️ Steps to take to add new notifier:
 * 1. Add new enum value here.
 * 2. Create a new class that implements [NotificationSender] interface.
 * 3. Add the @ContributesIntoSet(AppScope::class) annotation to the class.
 */
@Keep
enum class NotifierType(
    val displayName: String,
) {
    /**
     * Email notification using Mailgun API with limit of 100 per day.
     */
    EMAIL("Email"),

    /**
     * Telegram notification using Telegram Bot API.
     */
    TELEGRAM("Telegram"),

    /**
     * SMS notification using Twilio API.
     */
    TWILIO("Twilio"),

    /**
     * Simple REST webhook that can be used to send POST request with JSON payload.
     */
    WEBHOOK_REST_API("Webhook"),

    /**
     * Slack 2.0 workflow webhook to send POST request with JSON payload.
     */
    WEBHOOK_SLACK_WORKFLOW("Slack"),

    /**
     * Discord webhook to send POST request with rich embed formatting.
     */
    WEBHOOK_DISCORD("Discord"),
}
