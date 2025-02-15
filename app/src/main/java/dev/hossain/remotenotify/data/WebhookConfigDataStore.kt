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

private val Context.webhookConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "webhook_config")

@SingleIn(AppScope::class)
class WebhookConfigDataStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AlertMediumConfigStore {
        companion object {
            private val WEBHOOK_URL_KEY = stringPreferencesKey("webhook_url")

            object ValidationKeys {
                const val URL = "url"
            }
        }

        val webhookUrl: Flow<String?> =
            context.webhookConfigDataStore.data
                .map { preferences ->
                    preferences[WEBHOOK_URL_KEY]
                }

        suspend fun saveWebhookUrl(url: String) {
            Timber.d("Saving webhook URL: $url")
            context.webhookConfigDataStore.edit { preferences ->
                preferences[WEBHOOK_URL_KEY] = url
            }
        }

        override suspend fun clearConfig() {
            Timber.d("Clearing Webhook configuration")
            context.webhookConfigDataStore.edit { preferences ->
                preferences.remove(WEBHOOK_URL_KEY)
            }
        }

        override suspend fun hasValidConfig(): Boolean {
            val url = webhookUrl.first()
            if (url.isNullOrBlank()) {
                Timber.e("Webhook URL is not configured")
                return false
            }

            return validateConfig(AlertMediumConfig.WebhookConfig(url)).isValid
        }

        override suspend fun validateConfig(config: AlertMediumConfig): ConfigValidationResult {
            val errors = mutableMapOf<String, String>()

            val url =
                when (config) {
                    is AlertMediumConfig.WebhookConfig -> config.url
                    else -> return ConfigValidationResult(false, emptyMap())
                }

            // Basic URL validation for HTTP/HTTPS
            val isValidUrl =
                try {
                    url.matches(Regex("""^https?://[^\s/$.?#].[^\s]*$"""))
                } catch (e: Exception) {
                    Timber.e(e, "Invalid URL format")
                    false
                }

            if (!isValidUrl) {
                errors[ValidationKeys.URL] = "Invalid URL format"
                Timber.e("Invalid webhook URL format: $url")
            }

            Timber.i("Webhook config is valid")
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
