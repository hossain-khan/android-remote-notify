package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.di.ApplicationContext
import dev.hossain.remotenotify.model.AlertMediumConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

private val Context.slackWebhookConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "slack_webhook_config")

@SingleIn(AppScope::class)
class SlackWebhookConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlertMediumConfigStore {
        companion object {
            private val SLACK_WEBHOOK_URL_KEY = stringPreferencesKey("slack_webhook_url")

            object ValidationKeys {
                const val URL = "url"
            }
        }

        val webhookUrl: Flow<String?> =
            context.slackWebhookConfigDataStore.data
                .map { preferences ->
                    preferences[SLACK_WEBHOOK_URL_KEY]
                }

        suspend fun saveWebhookUrl(url: String) {
            Timber.d("Saving Slack webhook URL: $url")
            context.slackWebhookConfigDataStore.edit { preferences ->
                preferences[SLACK_WEBHOOK_URL_KEY] = url
            }
        }

        override suspend fun clearConfig() {
            Timber.d("Clearing Slack Webhook configuration")
            context.slackWebhookConfigDataStore.edit { preferences ->
                preferences.remove(SLACK_WEBHOOK_URL_KEY)
            }
        }

        override suspend fun hasValidConfig(): Boolean {
            val url = webhookUrl.first()
            if (url.isNullOrBlank()) {
                Timber.e("Slack Webhook URL is not configured")
                return false
            }

            return validateConfig(AlertMediumConfig.WebhookConfig(url)).isValid
        }

        override suspend fun validateConfig(config: AlertMediumConfig): ConfigValidationResult {
            val errors = mutableMapOf<String, String>()

            val url =
                when (config) {
                    is AlertMediumConfig.WebhookConfig -> config.url
                    else -> return ConfigValidationResult(isValid = false, errors = emptyMap())
                }

            // Specific validation for Slack webhook URLs
            val isValidUrl =
                try {
                    url.matches(Regex("""^https://hooks\.slack\.com/services/T[A-Z0-9]+/B[A-Z0-9]+/[a-zA-Z0-9]+$"""))
                } catch (e: Exception) {
                    Timber.e(e, "Invalid Slack webhook URL format")
                    false
                }

            if (!isValidUrl) {
                errors[ValidationKeys.URL] = "Invalid Slack webhook URL format. It should start with https://hooks.slack.com/services/"
                Timber.e("Invalid Slack webhook URL format: $url")
            }

            Timber.i("Slack webhook config is valid")
            return ConfigValidationResult(
                isValid = errors.isEmpty(),
                errors = errors,
            )
        }

        suspend fun getConfig(): AlertMediumConfig.WebhookConfig {
            val url = webhookUrl.first() ?: ""
            return AlertMediumConfig.WebhookConfig(url)
        }
    }
