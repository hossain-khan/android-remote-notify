package dev.hossain.remotenotify.notifier

import com.squareup.anvil.annotations.ContributesMultibinding
import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.RemoteNotification
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@ContributesMultibinding(AppScope::class)
@Named("telegram") // Could not use `NotifierType.TELEGRAM.name` as it's not a constant.
class TelegramNotificationSender
    @Inject
    constructor(
        private val telegramConfigDataStore: TelegramConfigDataStore,
        private val okHttpClient: OkHttpClient,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.TELEGRAM

        override suspend fun sendNotification(remoteNotification: RemoteNotification): Boolean {
            val message =
                when (remoteNotification) {
                    is RemoteNotification.BatteryNotification -> "Battery Alert: ${remoteNotification.batteryPercentage}%"
                    is RemoteNotification.StorageNotification -> "Storage Alert: ${remoteNotification.storageMinSpaceGb}GB available"
                }

            val botToken =
                requireNotNull(telegramConfigDataStore.botToken.first()) {
                    "Bot token is required. Check `hasValidConfiguration` before using the notifier."
                }
            val chatId =
                requireNotNull(telegramConfigDataStore.chatId.first()) {
                    "Telegram chat id is required. Check `hasValidConfiguration` before using the notifier."
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

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Failed to send notification: ${response.code} - ${response.message}")
                    return false
                } else {
                    Timber.d("Notification sent successfully: $message")
                    return true
                }
            }
        }

        override suspend fun hasValidConfiguration(): Boolean {
            val botToken = telegramConfigDataStore.botToken.first()
            val chatId = telegramConfigDataStore.chatId.first()

            if (botToken.isNullOrBlank() || chatId.isNullOrBlank()) {
                Timber.e("Bot token or chat ID is not configured.")
                return false
            } else {
                Timber.i("Telegram config is set correctly.")
                return true
            }
        }
    }
