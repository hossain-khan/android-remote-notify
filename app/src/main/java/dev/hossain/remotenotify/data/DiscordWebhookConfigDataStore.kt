package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.discordWebhookConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "discord_webhook_config")

@Inject
@SingleIn(AppScope::class)
class DiscordWebhookConfigDataStore
    constructor(
        private val context: Context,
    ) : AlertMediumConfigStore {
        companion object {
            private val DISCORD_WEBHOOK_URL_KEY = stringPreferencesKey("discord_webhook_url")

            object ValidationKeys {
                const val URL = "url"
            }
        }

        val discordWebhookUrl: Flow<String?> =
            context.discordWebhookConfigDataStore.data
                .map { preferences ->
                    preferences[DISCORD_WEBHOOK_URL_KEY]
                }

        suspend fun saveDiscordWebhookUrl(url: String) {
            Timber.d("Saving Discord webhook URL: $url")
            context.discordWebhookConfigDataStore.edit { preferences ->
                preferences[DISCORD_WEBHOOK_URL_KEY] = url
            }
        }

        override suspend fun clearConfig() {
            Timber.d("Clearing Discord Webhook configuration")
            context.discordWebhookConfigDataStore.edit { preferences ->
                preferences.remove(DISCORD_WEBHOOK_URL_KEY)
            }
        }

        override suspend fun hasValidConfig(): Boolean {
            val url = discordWebhookUrl.first()
            if (url.isNullOrBlank()) {
                Timber.e("Discord Webhook URL is not configured")
                return false
            }

            return validateConfig(AlertMediumConfig.DiscordConfig(url)).isValid
        }

        override suspend fun validateConfig(config: AlertMediumConfig): ConfigValidationResult {
            val errors = mutableMapOf<String, String>()

            val url =
                when (config) {
                    is AlertMediumConfig.DiscordConfig -> config.webhookUrl
                    else -> return ConfigValidationResult(isValid = false, errors = emptyMap())
                }

            // Validation for Discord webhook URLs
            // Discord webhook URL format: https://discord.com/api/webhooks/{id}/{token}
            // or https://discordapp.com/api/webhooks/{id}/{token} (legacy)
            val isValidUrl =
                try {
                    url.matches(
                        Regex(
                            """^https://(discord\.com|discordapp\.com)/api/webhooks/\d+/[a-zA-Z0-9_-]+$""",
                        ),
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Invalid Discord webhook URL format")
                    false
                }

            if (!isValidUrl) {
                errors[ValidationKeys.URL] =
                    "Invalid Discord webhook URL format. It should start with https://discord.com/api/webhooks/"
                Timber.e("Invalid Discord webhook URL format: $url")
            }

            if (errors.isEmpty()) {
                Timber.i("Discord webhook config is valid")
            }
            return ConfigValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
            )
        }

        suspend fun getConfig(): AlertMediumConfig.DiscordConfig {
            val url = discordWebhookUrl.first() ?: ""
            return AlertMediumConfig.DiscordConfig(url)
        }
    }
