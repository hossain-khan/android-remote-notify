package dev.hossain.remotenotify.data.export

import com.squareup.moshi.JsonClass
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType

/**
 * Represents the complete app configuration that can be exported and imported.
 * Contains all user settings except for historical data like alert check logs.
 *
 * @property version Configuration format version for migration support
 * @property exportedAt Unix timestamp when configuration was exported
 * @property deviceName Optional device identifier for reference
 * @property alerts List of alert configurations
 * @property notifiers Notification channel configurations
 * @property preferences App preferences
 */
@JsonClass(generateAdapter = true)
data class AppConfiguration(
    val version: Int = CURRENT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val deviceName: String? = null,
    val alerts: List<AlertConfig> = emptyList(),
    val notifiers: NotifierConfigs = NotifierConfigs(),
    val preferences: AppPreferences = AppPreferences(),
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}

/**
 * Represents a single alert configuration.
 */
@JsonClass(generateAdapter = true)
data class AlertConfig(
    val type: AlertType,
    val batteryPercentage: Int? = null,
    val storageMinSpaceGb: Int? = null,
)

/**
 * Contains all notifier configurations with encrypted sensitive data.
 */
@JsonClass(generateAdapter = true)
data class NotifierConfigs(
    val telegram: EncryptedConfig? = null,
    val email: EncryptedConfig? = null,
    val twilio: EncryptedConfig? = null,
    val webhook: EncryptedConfig? = null,
    val slack: EncryptedConfig? = null,
    val discord: EncryptedConfig? = null,
)

/**
 * Wrapper for encrypted configuration data.
 *
 * @property encrypted Whether the data is encrypted
 * @property data The encrypted or plaintext configuration data as JSON
 */
@JsonClass(generateAdapter = true)
data class EncryptedConfig(
    val encrypted: Boolean = true,
    val data: String,
)

/**
 * Unencrypted Telegram configuration for internal use.
 */
@JsonClass(generateAdapter = true)
data class TelegramConfigData(
    val botToken: String,
    val chatId: String,
)

/**
 * Unencrypted Email configuration for internal use.
 */
@JsonClass(generateAdapter = true)
data class EmailConfigData(
    val toEmail: String,
)

/**
 * Unencrypted Twilio configuration for internal use.
 */
@JsonClass(generateAdapter = true)
data class TwilioConfigData(
    val accountSid: String,
    val authToken: String,
    val fromPhone: String,
    val toPhone: String,
)

/**
 * Unencrypted Webhook configuration for internal use.
 */
@JsonClass(generateAdapter = true)
data class WebhookConfigData(
    val url: String,
)

/**
 * Unencrypted Slack Webhook configuration for internal use.
 */
@JsonClass(generateAdapter = true)
data class SlackConfigData(
    val webhookUrl: String,
)

/**
 * Unencrypted Discord Webhook configuration for internal use.
 */
@JsonClass(generateAdapter = true)
data class DiscordConfigData(
    val webhookUrl: String,
)

/**
 * App preferences.
 */
@JsonClass(generateAdapter = true)
data class AppPreferences(
    val workerIntervalMinutes: Long? = null,
)

/**
 * Result of configuration import validation.
 */
sealed class ImportValidationResult {
    data class Valid(
        val configuration: AppConfiguration,
    ) : ImportValidationResult()

    data class Invalid(
        val errors: List<String>,
    ) : ImportValidationResult()
}

/**
 * Result of configuration export or import operation.
 */
sealed class ConfigOperationResult {
    data object Success : ConfigOperationResult()

    data class Error(
        val message: String,
        val exception: Throwable? = null,
    ) : ConfigOperationResult()
}

/**
 * List of configured notifier types based on NotifierConfigs.
 */
fun NotifierConfigs.getConfiguredNotifierTypes(): List<NotifierType> =
    buildList {
        if (telegram != null) add(NotifierType.TELEGRAM)
        if (email != null) add(NotifierType.EMAIL)
        if (twilio != null) add(NotifierType.TWILIO)
        if (webhook != null) add(NotifierType.WEBHOOK_REST_API)
        if (slack != null) add(NotifierType.WEBHOOK_SLACK_WORKFLOW)
        if (discord != null) add(NotifierType.WEBHOOK_DISCORD)
    }
