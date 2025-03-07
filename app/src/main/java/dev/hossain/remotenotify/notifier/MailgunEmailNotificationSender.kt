package dev.hossain.remotenotify.notifier

import android.util.Base64
import com.squareup.anvil.annotations.ContributesMultibinding
import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.EmailConfigDataStore
import dev.hossain.remotenotify.data.EmailQuotaManager
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.DeviceAlert
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.toTypeDisplayName
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import android.util.Base64.encodeToString as encodeBase64

/**
 * Sends email notification using Mailgun API.
 * - https://documentation.mailgun.com/docs/mailgun/user-manual/get-started/
 * - https://documentation.mailgun.com/en/latest/api-sending.html#sending
 */
@ContributesMultibinding(AppScope::class)
@Named("email")
class MailgunEmailNotificationSender
    @Inject
    constructor(
        private val emailConfigDataStore: EmailConfigDataStore,
        private val emailQuotaManager: EmailQuotaManager,
        private val okHttpClient: OkHttpClient,
        private val alertFormatter: AlertFormatter,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.EMAIL

        override suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean {
            if (!emailQuotaManager.canSendEmail()) {
                Timber.w("Daily email quota exceeded")
                return false
            }

            val config = emailConfigDataStore.getConfig()
            val htmlMessage = alertFormatter.format(remoteAlert, DeviceAlert.FormatType.HTML)

            /*
             * Example cURL command:
             * curl -s --user 'api:API_KEY' \
             * https://api.mailgun.net/v3/notify.liquidlabs.ca/messages \
             * -F from='Mailgun Sandbox <postmaster@notify.liquidlabs.ca>' \
             * -F to='Recipient Name <bob@emai.com>' \
             * -F subject='Battery Alert' \
             * -F text='Your battery is running low.'
             */
            val formBody =
                FormBody
                    .Builder()
                    .add("from", config.fromEmail)
                    .add("to", config.toEmail)
                    .add("subject", "Remote Notify Alert: ${remoteAlert.toTypeDisplayName()}")
                    .add("html", htmlMessage)
                    .build()

            val request =
                Request
                    .Builder()
                    .url("https://api.mailgun.net/v3/${config.domain}/messages")
                    .addHeader("Authorization", "Basic " + encodeBase64("api:${config.apiKey}".toByteArray(), Base64.NO_WRAP))
                    .post(formBody)
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Failed to send email: ${response.code} - ${response.message}")
                    return false
                }

                emailQuotaManager.recordEmailSent()
                Timber.d("Email sent successfully")
                return true
            }
        }

        override suspend fun hasValidConfig(): Boolean = emailConfigDataStore.hasValidConfig()

        override suspend fun saveConfig(alertMediumConfig: AlertMediumConfig) {
            when (alertMediumConfig) {
                is AlertMediumConfig.EmailConfig -> emailConfigDataStore.saveConfig(alertMediumConfig)
                else -> throw IllegalArgumentException("Invalid config type: $alertMediumConfig")
            }
        }

        override suspend fun getConfig(): AlertMediumConfig = emailConfigDataStore.getConfig()

        override suspend fun clearConfig() = emailConfigDataStore.clearConfig()

        override suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            emailConfigDataStore.validateConfig(alertMediumConfig)
    }
