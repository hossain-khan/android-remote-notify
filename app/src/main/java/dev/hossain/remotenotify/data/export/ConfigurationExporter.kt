package dev.hossain.remotenotify.data.export

import android.os.Build
import com.squareup.moshi.Moshi
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.DiscordWebhookConfigDataStore
import dev.hossain.remotenotify.data.EmailConfigDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.data.SlackWebhookConfigDataStore
import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.data.TwilioConfigDataStore
import dev.hossain.remotenotify.data.WebhookConfigDataStore
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * Service responsible for exporting app configuration to JSON format.
 * Sensitive data (API keys, tokens) are encrypted using password-based encryption.
 */
@SingleIn(AppScope::class)
@Inject
class ConfigurationExporter
    constructor(
        private val remoteAlertRepository: RemoteAlertRepository,
        private val appPreferencesDataStore: AppPreferencesDataStore,
        private val telegramConfigDataStore: TelegramConfigDataStore,
        private val emailConfigDataStore: EmailConfigDataStore,
        private val twilioConfigDataStore: TwilioConfigDataStore,
        private val webhookConfigDataStore: WebhookConfigDataStore,
        private val slackWebhookConfigDataStore: SlackWebhookConfigDataStore,
        private val discordWebhookConfigDataStore: DiscordWebhookConfigDataStore,
        private val moshi: Moshi,
    ) {
        /**
         * Exports the complete app configuration to JSON string.
         *
         * @param password Password to encrypt sensitive data
         * @return JSON string of the configuration or error result
         */
        suspend fun exportConfiguration(password: String): Result<String> =
            try {
                val configuration = buildConfiguration(password)
                val adapter = moshi.adapter(AppConfiguration::class.java).indent("  ")
                val json = adapter.toJson(configuration)
                Timber.i("Configuration exported successfully")
                Result.success(json)
            } catch (e: Exception) {
                Timber.e(e, "Failed to export configuration")
                Result.failure(e)
            }

        private suspend fun buildConfiguration(password: String): AppConfiguration {
            val alerts = exportAlerts()
            val notifiers = exportNotifiers(password)
            val preferences = exportPreferences()

            return AppConfiguration(
                version = AppConfiguration.CURRENT_VERSION,
                exportedAt = System.currentTimeMillis(),
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                alerts = alerts,
                notifiers = notifiers,
                preferences = preferences,
            )
        }

        private suspend fun exportAlerts(): List<AlertConfig> {
            val alerts = remoteAlertRepository.getAllRemoteAlert()
            return alerts.map { alert ->
                when (alert) {
                    is RemoteAlert.BatteryAlert ->
                        AlertConfig(
                            type = AlertType.BATTERY,
                            batteryPercentage = alert.batteryPercentage,
                        )
                    is RemoteAlert.StorageAlert ->
                        AlertConfig(
                            type = AlertType.STORAGE,
                            storageMinSpaceGb = alert.storageMinSpaceGb,
                        )
                }
            }
        }

        private suspend fun exportNotifiers(password: String): NotifierConfigs =
            NotifierConfigs(
                telegram = exportTelegramConfig(password),
                email = exportEmailConfig(password),
                twilio = exportTwilioConfig(password),
                webhook = exportWebhookConfig(password),
                slack = exportSlackConfig(password),
                discord = exportDiscordConfig(password),
            )

        private suspend fun exportTelegramConfig(password: String): EncryptedConfig? {
            val config = telegramConfigDataStore.getConfig()
            if (config.botToken.isBlank() && config.chatId.isBlank()) return null

            val configData = TelegramConfigData(botToken = config.botToken, chatId = config.chatId)
            val adapter = moshi.adapter(TelegramConfigData::class.java)
            val json = adapter.toJson(configData)
            return EncryptedConfig(
                encrypted = true,
                data = ConfigEncryption.encrypt(json, password),
            )
        }

        private suspend fun exportEmailConfig(password: String): EncryptedConfig? {
            val config = emailConfigDataStore.getConfig()
            if (config.toEmail.isBlank()) return null

            val configData = EmailConfigData(toEmail = config.toEmail)
            val adapter = moshi.adapter(EmailConfigData::class.java)
            val json = adapter.toJson(configData)
            return EncryptedConfig(
                encrypted = true,
                data = ConfigEncryption.encrypt(json, password),
            )
        }

        private suspend fun exportTwilioConfig(password: String): EncryptedConfig? {
            val config = twilioConfigDataStore.getConfig()
            if (config.accountSid.isBlank() &&
                config.authToken.isBlank() &&
                config.fromPhone.isBlank() &&
                config.toPhone.isBlank()
            ) {
                return null
            }

            val configData =
                TwilioConfigData(
                    accountSid = config.accountSid,
                    authToken = config.authToken,
                    fromPhone = config.fromPhone,
                    toPhone = config.toPhone,
                )
            val adapter = moshi.adapter(TwilioConfigData::class.java)
            val json = adapter.toJson(configData)
            return EncryptedConfig(
                encrypted = true,
                data = ConfigEncryption.encrypt(json, password),
            )
        }

        private suspend fun exportWebhookConfig(password: String): EncryptedConfig? {
            val config = webhookConfigDataStore.getConfig()
            if (config.url.isBlank()) return null

            val configData = WebhookConfigData(url = config.url)
            val adapter = moshi.adapter(WebhookConfigData::class.java)
            val json = adapter.toJson(configData)
            return EncryptedConfig(
                encrypted = true,
                data = ConfigEncryption.encrypt(json, password),
            )
        }

        private suspend fun exportSlackConfig(password: String): EncryptedConfig? {
            val config = slackWebhookConfigDataStore.getConfig()
            if (config.url.isBlank()) return null

            val configData = SlackConfigData(webhookUrl = config.url)
            val adapter = moshi.adapter(SlackConfigData::class.java)
            val json = adapter.toJson(configData)
            return EncryptedConfig(
                encrypted = true,
                data = ConfigEncryption.encrypt(json, password),
            )
        }

        private suspend fun exportDiscordConfig(password: String): EncryptedConfig? {
            val config = discordWebhookConfigDataStore.getConfig()
            if (config.webhookUrl.isBlank()) return null

            val configData = DiscordConfigData(webhookUrl = config.webhookUrl)
            val adapter = moshi.adapter(DiscordConfigData::class.java)
            val json = adapter.toJson(configData)
            return EncryptedConfig(
                encrypted = true,
                data = ConfigEncryption.encrypt(json, password),
            )
        }

        private suspend fun exportPreferences(): AppPreferences {
            val workerInterval = appPreferencesDataStore.workerIntervalFlow.first()
            return AppPreferences(
                workerIntervalMinutes = workerInterval,
            )
        }
    }
