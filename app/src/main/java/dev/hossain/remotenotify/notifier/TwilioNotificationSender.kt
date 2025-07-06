package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.TwilioConfigDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.RemoteAlert
import me.tatarka.inject.annotations.Inject
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

class TwilioNotificationSender
    @Inject
    constructor(
        private val twilioConfigDataStore: TwilioConfigDataStore,
        private val okHttpClient: OkHttpClient,
        private val alertFormatter: AlertFormatter,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.TWILIO

        override suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean {
            val config = twilioConfigDataStore.getConfig()
            val message = alertFormatter.format(remoteAlert)

            val url = "https://api.twilio.com/2010-04-01/Accounts/${config.accountSid}/Messages.json"
            val formBody =
                FormBody
                    .Builder()
                    .add("Body", message)
                    .add("From", config.fromPhone)
                    .add("To", config.toPhone)
                    .build()

            val request =
                Request
                    .Builder()
                    .url(url)
                    .post(formBody)
                    .header("Authorization", Credentials.basic(config.accountSid, config.authToken))
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Failed to send Twilio SMS: ${response.code} - ${response.message}")
                    return false
                }
                Timber.d("SMS sent successfully via Twilio.")
                return true
            }
        }

        override suspend fun hasValidConfig(): Boolean = twilioConfigDataStore.hasValidConfig()

        override suspend fun saveConfig(alertMediumConfig: AlertMediumConfig) {
            if (alertMediumConfig is AlertMediumConfig.TwilioConfig) {
                twilioConfigDataStore.saveConfig(alertMediumConfig)
            } else {
                throw IllegalArgumentException("Invalid configuration type: $alertMediumConfig")
            }
        }

        override suspend fun getConfig(): AlertMediumConfig = twilioConfigDataStore.getConfig()

        override suspend fun clearConfig() = twilioConfigDataStore.clearConfig()

        override suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            twilioConfigDataStore.validateConfig(alertMediumConfig)
    }
