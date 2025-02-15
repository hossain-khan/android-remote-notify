package dev.hossain.remotenotify.notifier

import com.squareup.anvil.annotations.ContributesMultibinding
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.data.ConfigValidationResult
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
            // Text of the message to be sent, 1-4096 characters after entities parsing
            val message =
                when (remoteNotification) {
                    is RemoteNotification.BatteryNotification -> "Battery Alert: ${remoteNotification.batteryPercentage}%"
                    is RemoteNotification.StorageNotification -> "Storage Alert: ${remoteNotification.storageMinSpaceGb}GB available"
                }

            // Each bot is given a unique authentication token when it is created.
            // The token looks something like 123456:ABCDEF1234ghIklzyx57W2v1u123ew11 or 110201543:AAHdqTcvCH1vGWJxfSeofSAs0K5PALDsaw
            val botToken =
                requireNotNull(telegramConfigDataStore.botToken.first()) {
                    "Bot token is required. Check `hasValidConfiguration` before using the notifier."
                }
            // Unique identifier for the target chat or username of the target channel (in the format @channelusername)
            val chatId =
                requireNotNull(telegramConfigDataStore.chatId.first()) {
                    "Telegram chat id is required. Check `hasValidConfiguration` before using the notifier."
                }

            // https://core.telegram.org/bots/api#making-requests
            // https://core.telegram.org/bots/api#sendmessage
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

        override suspend fun hasValidConfig(): Boolean = telegramConfigDataStore.hasValidConfig()

        override suspend fun saveConfig(alertMediumConfig: AlertMediumConfig) {
            when (alertMediumConfig) {
                is AlertMediumConfig.TelegramConfig -> {
                    telegramConfigDataStore.saveBotToken(alertMediumConfig.botToken)
                    telegramConfigDataStore.saveChatId(alertMediumConfig.chatId)
                }
                else -> throw IllegalArgumentException("Invalid configuration type: $alertMediumConfig")
            }
        }

        override suspend fun getConfig(): AlertMediumConfig = telegramConfigDataStore.getConfig()

        override suspend fun clearConfig() {
            telegramConfigDataStore.clearConfig()
        }

        override suspend fun isValidConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            telegramConfigDataStore.validateConfig(alertMediumConfig)
    }
