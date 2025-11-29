package dev.hossain.remotenotify.data.export

import com.squareup.moshi.Moshi
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.DiscordWebhookConfigDataStore
import dev.hossain.remotenotify.data.EmailConfigDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.data.SlackWebhookConfigDataStore
import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.data.TwilioConfigDataStore
import dev.hossain.remotenotify.data.WebhookConfigDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import timber.log.Timber

/**
 * Service responsible for importing app configuration from JSON format.
 * Validates configuration and decrypts sensitive data.
 */
@SingleIn(AppScope::class)
@Inject
class ConfigurationImporter
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
         * Parses and validates a JSON configuration string.
         *
         * @param json JSON string to parse
         * @return Validation result with parsed configuration or errors
         */
        fun parseAndValidate(json: String): ImportValidationResult {
            return try {
                val adapter = moshi.adapter(AppConfiguration::class.java)
                val configuration = adapter.fromJson(json)

                if (configuration == null) {
                    return ImportValidationResult.Invalid(listOf("Failed to parse configuration JSON"))
                }

                val errors = validateConfiguration(configuration)
                if (errors.isNotEmpty()) {
                    ImportValidationResult.Invalid(errors)
                } else {
                    ImportValidationResult.Valid(configuration)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse configuration")
                ImportValidationResult.Invalid(listOf("Failed to parse configuration: ${e.message}"))
            }
        }

        /**
         * Tests if the password can decrypt the configuration.
         *
         * Note: This tests against the first available encrypted config. Since all configs
         * in a single export use the same password, testing against one config is sufficient.
         * If a config can be decrypted, the password is correct for all encrypted data.
         *
         * @param configuration The configuration to test
         * @param password Password to test
         * @return True if password is valid
         */
        fun testPassword(
            configuration: AppConfiguration,
            password: String,
        ): Boolean {
            // Try to decrypt any encrypted config to verify password.
            // All notifier configs use the same password during export.
            val encryptedConfig =
                configuration.notifiers.telegram
                    ?: configuration.notifiers.email
                    ?: configuration.notifiers.twilio
                    ?: configuration.notifiers.webhook
                    ?: configuration.notifiers.slack
                    ?: configuration.notifiers.discord

            if (encryptedConfig == null) {
                // No encrypted data, password is valid by default
                return true
            }

            return try {
                ConfigEncryption.decrypt(encryptedConfig.data, password)
                true
            } catch (e: Exception) {
                Timber.w(e, "Password test failed")
                false
            }
        }

        /**
         * Imports configuration into the app.
         *
         * @param configuration Configuration to import
         * @param password Password to decrypt sensitive data
         * @param replaceExisting Whether to replace existing data
         * @return Result of the import operation
         */
        suspend fun importConfiguration(
            configuration: AppConfiguration,
            password: String,
            replaceExisting: Boolean = true,
        ): ConfigOperationResult =
            try {
                if (replaceExisting) {
                    clearExistingData()
                }

                importAlerts(configuration.alerts)
                importNotifiers(configuration.notifiers, password)
                importPreferences(configuration.preferences)

                Timber.i("Configuration imported successfully")
                ConfigOperationResult.Success
            } catch (e: Exception) {
                Timber.e(e, "Failed to import configuration")
                ConfigOperationResult.Error("Failed to import configuration: ${e.message}", e)
            }

        private fun validateConfiguration(configuration: AppConfiguration): List<String> {
            val errors = mutableListOf<String>()

            // Version check
            if (configuration.version < 1 || configuration.version > AppConfiguration.CURRENT_VERSION) {
                errors.add(
                    "Unsupported configuration version: ${configuration.version}. " +
                        "Supported versions: 1-${AppConfiguration.CURRENT_VERSION}",
                )
            }

            // Validate alerts
            configuration.alerts.forEach { alert ->
                when (alert.type) {
                    AlertType.BATTERY -> {
                        if (alert.batteryPercentage == null) {
                            errors.add("Battery alert missing batteryPercentage")
                        } else if (alert.batteryPercentage < 0 || alert.batteryPercentage > 100) {
                            errors.add("Invalid battery percentage: ${alert.batteryPercentage}")
                        }
                    }
                    AlertType.STORAGE -> {
                        if (alert.storageMinSpaceGb == null) {
                            errors.add("Storage alert missing storageMinSpaceGb")
                        } else if (alert.storageMinSpaceGb < 0) {
                            errors.add("Invalid storage space: ${alert.storageMinSpaceGb}")
                        }
                    }
                }
            }

            return errors
        }

        private suspend fun clearExistingData() {
            // Clear existing alerts
            val existingAlerts = remoteAlertRepository.getAllRemoteAlert()
            existingAlerts.forEach { alert ->
                remoteAlertRepository.deleteRemoteAlert(alert)
            }

            // Clear notifier configs
            telegramConfigDataStore.clearConfig()
            emailConfigDataStore.clearConfig()
            twilioConfigDataStore.clearConfig()
            webhookConfigDataStore.clearConfig()
            slackWebhookConfigDataStore.clearConfig()
            discordWebhookConfigDataStore.clearConfig()
        }

        private suspend fun importAlerts(alerts: List<AlertConfig>) {
            alerts.forEach { alertConfig ->
                val remoteAlert =
                    when (alertConfig.type) {
                        AlertType.BATTERY ->
                            RemoteAlert.BatteryAlert(
                                batteryPercentage = alertConfig.batteryPercentage ?: 20,
                            )
                        AlertType.STORAGE ->
                            RemoteAlert.StorageAlert(
                                storageMinSpaceGb = alertConfig.storageMinSpaceGb ?: 1,
                            )
                    }
                remoteAlertRepository.saveRemoteAlert(remoteAlert)
            }
        }

        private suspend fun importNotifiers(
            notifiers: NotifierConfigs,
            password: String,
        ) {
            notifiers.telegram?.let { importTelegramConfig(it, password) }
            notifiers.email?.let { importEmailConfig(it, password) }
            notifiers.twilio?.let { importTwilioConfig(it, password) }
            notifiers.webhook?.let { importWebhookConfig(it, password) }
            notifiers.slack?.let { importSlackConfig(it, password) }
            notifiers.discord?.let { importDiscordConfig(it, password) }
        }

        private suspend fun importTelegramConfig(
            encryptedConfig: EncryptedConfig,
            password: String,
        ) {
            val json =
                if (encryptedConfig.encrypted) {
                    ConfigEncryption.decrypt(encryptedConfig.data, password)
                } else {
                    encryptedConfig.data
                }
            val adapter = moshi.adapter(TelegramConfigData::class.java)
            val config = adapter.fromJson(json) ?: return

            telegramConfigDataStore.saveBotToken(config.botToken)
            telegramConfigDataStore.saveChatId(config.chatId)
        }

        private suspend fun importEmailConfig(
            encryptedConfig: EncryptedConfig,
            password: String,
        ) {
            val json =
                if (encryptedConfig.encrypted) {
                    ConfigEncryption.decrypt(encryptedConfig.data, password)
                } else {
                    encryptedConfig.data
                }
            val adapter = moshi.adapter(EmailConfigData::class.java)
            val config = adapter.fromJson(json) ?: return

            emailConfigDataStore.saveConfig(
                AlertMediumConfig.EmailConfig(
                    apiKey = "",
                    domain = "",
                    fromEmail = "",
                    toEmail = config.toEmail,
                ),
            )
        }

        private suspend fun importTwilioConfig(
            encryptedConfig: EncryptedConfig,
            password: String,
        ) {
            val json =
                if (encryptedConfig.encrypted) {
                    ConfigEncryption.decrypt(encryptedConfig.data, password)
                } else {
                    encryptedConfig.data
                }
            val adapter = moshi.adapter(TwilioConfigData::class.java)
            val config = adapter.fromJson(json) ?: return

            twilioConfigDataStore.saveConfig(
                AlertMediumConfig.TwilioConfig(
                    accountSid = config.accountSid,
                    authToken = config.authToken,
                    fromPhone = config.fromPhone,
                    toPhone = config.toPhone,
                ),
            )
        }

        private suspend fun importWebhookConfig(
            encryptedConfig: EncryptedConfig,
            password: String,
        ) {
            val json =
                if (encryptedConfig.encrypted) {
                    ConfigEncryption.decrypt(encryptedConfig.data, password)
                } else {
                    encryptedConfig.data
                }
            val adapter = moshi.adapter(WebhookConfigData::class.java)
            val config = adapter.fromJson(json) ?: return

            webhookConfigDataStore.saveWebhookUrl(config.url)
        }

        private suspend fun importSlackConfig(
            encryptedConfig: EncryptedConfig,
            password: String,
        ) {
            val json =
                if (encryptedConfig.encrypted) {
                    ConfigEncryption.decrypt(encryptedConfig.data, password)
                } else {
                    encryptedConfig.data
                }
            val adapter = moshi.adapter(SlackConfigData::class.java)
            val config = adapter.fromJson(json) ?: return

            slackWebhookConfigDataStore.saveSlackWorkflowWebhookUrl(config.webhookUrl)
        }

        private suspend fun importDiscordConfig(
            encryptedConfig: EncryptedConfig,
            password: String,
        ) {
            val json =
                if (encryptedConfig.encrypted) {
                    ConfigEncryption.decrypt(encryptedConfig.data, password)
                } else {
                    encryptedConfig.data
                }
            val adapter = moshi.adapter(DiscordConfigData::class.java)
            val config = adapter.fromJson(json) ?: return

            discordWebhookConfigDataStore.saveDiscordWebhookUrl(config.webhookUrl)
        }

        private suspend fun importPreferences(preferences: AppPreferences) {
            preferences.workerIntervalMinutes?.let { interval ->
                appPreferencesDataStore.saveWorkerInterval(interval)
            }
        }
    }
