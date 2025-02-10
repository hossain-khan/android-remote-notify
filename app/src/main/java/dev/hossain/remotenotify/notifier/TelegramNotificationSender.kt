package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.model.RemoteNotification
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject

class TelegramNotificationSender
    @Inject
    constructor(
        private val telegramConfigDataStore: TelegramConfigDataStore,
        private val client: OkHttpClient,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.TELEGRAM

        override suspend fun sendNotification(remoteNotification: RemoteNotification) {
            val message =
                when (remoteNotification) {
                    is RemoteNotification.BatteryNotification -> "Battery Alert: ${remoteNotification.batteryPercentage}%"
                    is RemoteNotification.StorageNotification -> "Storage Alert: ${remoteNotification.storageMinSpaceGb}GB available"
                }

            val botToken = telegramConfigDataStore.botToken.first() ?: ""
            val chatId = telegramConfigDataStore.chatId.first() ?: ""

            if (botToken.isBlank() || chatId.isBlank()) {
                Timber.e("Bot token or chat ID is not configured.")
                return
            }

            val url = "https://api.telegram.org/bot$botToken/sendMessage"
            val json =
                """
                {
                    "chat_id": "$chatId",
                    "text": "$message"
                }
                """.trimIndent()

            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request =
                Request
                    .Builder()
                    .url(url)
                    .post(body)
                    .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Failed to send notification: ${response.code} - ${response.message}")
                } else {
                    Timber.d("Notification sent successfully: $message")
                }
            }
        }
    }
