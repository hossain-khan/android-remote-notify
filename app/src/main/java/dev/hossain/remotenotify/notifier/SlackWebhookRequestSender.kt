package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.SlackWebhookConfigDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.DeviceAlert.FormatType
import dev.hossain.remotenotify.model.RemoteAlert
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

class SlackWebhookRequestSender
    @Inject
    constructor(
        private val slackWebhookConfigDataStore: SlackWebhookConfigDataStore,
        private val okHttpClient: OkHttpClient,
        private val alertFormatter: AlertFormatter,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.WEBHOOK_SLACK_WORKFLOW

        override suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean {
            val webhookUrl =
                requireNotNull(slackWebhookConfigDataStore.slackWorkflowWebhookUrl.first()) {
                    "Slack Webhook URL is required. Check `hasValidConfiguration` before using the notifier."
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
                    Timber.e("Failed to send Slack webhook: ${response.code} - ${response.message}")
                    return false
                }
                Timber.d("Slack webhook sent successfully")
                return true
            }
        }

        override suspend fun hasValidConfig(): Boolean = slackWebhookConfigDataStore.hasValidConfig()

        override suspend fun saveConfig(alertMediumConfig: AlertMediumConfig) {
            when (alertMediumConfig) {
                is AlertMediumConfig.WebhookConfig -> slackWebhookConfigDataStore.saveSlackWorkflowWebhookUrl(alertMediumConfig.url)
                else -> throw IllegalArgumentException("Invalid configuration type: $alertMediumConfig")
            }
        }

        override suspend fun getConfig(): AlertMediumConfig = slackWebhookConfigDataStore.getConfig()

        override suspend fun clearConfig() {
            slackWebhookConfigDataStore.clearConfig()
        }

        override suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            slackWebhookConfigDataStore.validateConfig(alertMediumConfig)
    }
