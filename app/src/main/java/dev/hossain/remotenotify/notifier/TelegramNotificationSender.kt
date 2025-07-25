package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.DeviceAlert.FormatType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

@ContributesIntoSet(AppScope::class)
@Named("telegram") // Could not use `NotifierType.TELEGRAM.name` as it's not a constant.
@Inject
class TelegramNotificationSender
    constructor(
        private val telegramConfigDataStore: TelegramConfigDataStore,
        private val okHttpClient: OkHttpClient,
        private val alertFormatter: AlertFormatter,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.TELEGRAM

        override suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean {
            // Text of the message to be sent, 1-4096 characters after entities parsing
            val message = alertFormatter.format(remoteAlert, FormatType.EXTENDED_TEXT)

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

        override suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            telegramConfigDataStore.validateConfig(alertMediumConfig)
    }
