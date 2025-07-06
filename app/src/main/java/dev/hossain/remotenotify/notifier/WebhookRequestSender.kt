package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.WebhookConfigDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.DeviceAlert.FormatType
import dev.hossain.remotenotify.model.RemoteAlert
import kotlinx.coroutines.flow.first
import dev.zacsweers.metro.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

class WebhookRequestSender
    @Inject
    constructor(
        private val webhookConfigDataStore: WebhookConfigDataStore,
        private val okHttpClient: OkHttpClient,
        private val alertFormatter: AlertFormatter,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.WEBHOOK_REST_API

        override suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean {
            val webhookUrl =
                requireNotNull(webhookConfigDataStore.webhookUrl.first()) {
                    "Webhook URL is required. Check `hasValidConfiguration` before using the notifier."
                }

            val json = alertFormatter.format(remoteAlert, FormatType.JSON)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request =
                Request
                    .Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Failed to send webhook: ${response.code} - ${response.message}")
                    return false
                }
                Timber.d("Webhook sent successfully")
                return true
            }
        }

        override suspend fun hasValidConfig(): Boolean = webhookConfigDataStore.hasValidConfig()

        override suspend fun saveConfig(alertMediumConfig: AlertMediumConfig) {
            when (alertMediumConfig) {
                is AlertMediumConfig.WebhookConfig -> webhookConfigDataStore.saveWebhookUrl(alertMediumConfig.url)
                else -> throw IllegalArgumentException("Invalid configuration type: $alertMediumConfig")
            }
        }

        override suspend fun getConfig(): AlertMediumConfig = webhookConfigDataStore.getConfig()

        override suspend fun clearConfig() {
            webhookConfigDataStore.clearConfig()
        }

        override suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            webhookConfigDataStore.validateConfig(alertMediumConfig)
    }
