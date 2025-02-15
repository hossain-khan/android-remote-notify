package dev.hossain.remotenotify.notifier

import com.squareup.anvil.annotations.ContributesMultibinding
import dev.hossain.remotenotify.data.AlertMediumConfig
import dev.hossain.remotenotify.data.WebhookConfigDataStore
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.RemoteNotification
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Named

@ContributesMultibinding(AppScope::class)
@Named("webhook")
class WebhookRequestSender
    @Inject
    constructor(
        private val webhookConfigDataStore: WebhookConfigDataStore,
        private val okHttpClient: OkHttpClient,
        private val clock: Clock,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.WEBHOOK_REST_API

        override suspend fun sendNotification(remoteNotification: RemoteNotification): Boolean {
            val webhookUrl =
                requireNotNull(webhookConfigDataStore.webhookUrl.first()) {
                    "Webhook URL is required. Check `hasValidConfiguration` before using the notifier."
                }

            val json = buildJsonPayload(remoteNotification)
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

        private fun buildJsonPayload(notification: RemoteNotification): String {
            val timestamp = Instant.now(clock).toString()
            return when (notification) {
                is RemoteNotification.BatteryNotification ->
                    """
                    {
                        "alert_type": "BATTERY",
                        "battery_level_percent": ${notification.batteryPercentage},
                        "sent_on": "$timestamp"
                    }
                    """.trimIndent()
                is RemoteNotification.StorageNotification ->
                    """
                    {
                        "alert_type": "STORAGE",
                        "storage_gb": ${notification.storageMinSpaceGb},
                        "sent_on": "$timestamp"
                    }
                    """.trimIndent()
            }
        }

        override suspend fun hasValidConfig(): Boolean = webhookConfigDataStore.hasValidConfig()

        override suspend fun getConfig(): AlertMediumConfig = webhookConfigDataStore.getConfig()

        override suspend fun clearConfig() {
            webhookConfigDataStore.clearConfig()
        }

        override suspend fun isValidConfig(alertMediumConfig: AlertMediumConfig): Boolean =
            webhookConfigDataStore.isValidConfig(alertMediumConfig)
    }
