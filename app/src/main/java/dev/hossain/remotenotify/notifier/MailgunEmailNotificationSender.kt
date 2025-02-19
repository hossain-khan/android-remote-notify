package dev.hossain.remotenotify.notifier

import com.squareup.anvil.annotations.ContributesMultibinding
import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.MailgunConfigDataStore
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.RemoteAlert
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

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
        private val mailgunConfigDataStore: MailgunConfigDataStore,
        private val okHttpClient: OkHttpClient,
        private val alertFormatter: AlertFormatter,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.EMAIL

        override suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean {
            val config = mailgunConfigDataStore.getConfig()
            val message = alertFormatter.format(remoteAlert)

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
                    .add("subject", "Remote Alert: ${remoteAlert.javaClass.simpleName}")
                    .add("text", message)
                    .build()

            val request =
                Request
                    .Builder()
                    .url("https://api.mailgun.net/v3/${config.domain}/messages")
                    // TODO fix me
                    // .addHeader("Authorization", "Basic " + config.apiKey.toByteArray().encodeBase64())
                    .post(formBody)
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Failed to send email: ${response.code} - ${response.message}")
                    return false
                }
                Timber.d("Email sent successfully")
                return true
            }
        }

        override suspend fun hasValidConfig(): Boolean = mailgunConfigDataStore.hasValidConfig()

        override suspend fun saveConfig(alertMediumConfig: AlertMediumConfig) {
            when (alertMediumConfig) {
                is AlertMediumConfig.EmailConfig -> mailgunConfigDataStore.saveConfig(alertMediumConfig)
                else -> throw IllegalArgumentException("Invalid config type: $alertMediumConfig")
            }
        }

        override suspend fun getConfig(): AlertMediumConfig = mailgunConfigDataStore.getConfig()

        override suspend fun clearConfig() = mailgunConfigDataStore.clearConfig()

        override suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            mailgunConfigDataStore.validateConfig(alertMediumConfig)
    }
