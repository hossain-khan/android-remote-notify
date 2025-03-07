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

    data class EmailConfig(
        val apiKey: String,
        val domain: String,
        val fromEmail: String,
        val toEmail: String,
    ) : AlertMediumConfig
}

/**
 * Provides a preview text for the configuration.
 */
internal fun AlertMediumConfig.configPreviewText(): String =
    when (this) {
        is AlertMediumConfig.TelegramConfig -> {
            val preview =
                if (chatId.length > 15) {
                    "${chatId.take(5)}...${chatId.takeLast(7)}"
                } else {
                    chatId
                }
            preview
        }
        is AlertMediumConfig.WebhookConfig -> {
            val restUrl = url.removePrefix("https://").removePrefix("http://")
            val preview =
                if (restUrl.length > 20) {
                    "${restUrl.take(12)}...${restUrl.takeLast(8)}"
                } else {
                    restUrl
                }
            preview
        }
        is AlertMediumConfig.TwilioConfig -> {
            val preview =
                if (toPhone.length > 12) {
                    "${toPhone.take(3)}...${toPhone.takeLast(4)}"
                } else {
                    toPhone
                }
            preview
        }
        is AlertMediumConfig.EmailConfig -> {
            val preview =
                if (toEmail.length > 20) {
                    val atIndex = toEmail.indexOf('@')
                    if (atIndex > 0 && atIndex < toEmail.length - 1) {
                        // Keep part of username, @ symbol, and part of domain
                        val username = toEmail.substring(0, atIndex)
                        val domain = toEmail.substring(atIndex + 1)

                        val truncatedUsername = if (username.length > 10) "${username.take(5)}..." else username
                        val truncatedDomain = if (domain.length > 10) "...${domain.takeLast(7)}" else domain

                        "$truncatedUsername@$truncatedDomain"
                    } else {
                        // Fallback to simple truncation if @ not found in expected position
                        "${toEmail.take(10)}...${toEmail.takeLast(7)}"
                    }
                } else {
                    toEmail
                }
            preview
        }
    }
