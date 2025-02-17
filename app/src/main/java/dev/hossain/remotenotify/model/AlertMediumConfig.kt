package dev.hossain.remotenotify.model

/**
 * Represents different configuration types for alert mediums supported by the application.
 */
sealed interface AlertMediumConfig {
    /**
     * Configuration for Telegram alert notifications.
     *
     * @property botToken The Telegram bot token in format "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11"
     * @property chatId The target chat ID, can be numeric (e.g., "123456789") or channel username (e.g., "@channel")
     * @see [Telegram Bot API](https://core.telegram.org/bots/api)
     */
    data class TelegramConfig(
        val botToken: String,
        val chatId: String,
    ) : AlertMediumConfig

    /**
     * Configuration for Webhook (REST API) alert notifications.
     *
     * @property url The webhook URL that will receive POST requests with notification data.
     *             Must be a valid HTTP/HTTPS URL.
     */
    data class WebhookConfig(
        val url: String,
    ) : AlertMediumConfig

    data class TwilioConfig(
        /**
         * Twilio Account SID.
         * @see [Twilio Account SID](https://www.twilio.com/docs/iam/api/authtoken)
         */
        val accountSid: String,
        val authToken: String,
        val fromPhone: String,
        val toPhone: String,
    ) : AlertMediumConfig
}
