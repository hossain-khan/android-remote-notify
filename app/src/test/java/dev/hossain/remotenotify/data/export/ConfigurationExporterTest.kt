package dev.hossain.remotenotify.data.export

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.DiscordWebhookConfigDataStore
import dev.hossain.remotenotify.data.EmailConfigDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.data.SlackWebhookConfigDataStore
import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.data.TwilioConfigDataStore
import dev.hossain.remotenotify.data.WebhookConfigDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.AlertMode
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigurationExporterTest {
    private val remoteAlertRepository = mockk<RemoteAlertRepository>()
    private val appPreferencesDataStore = mockk<AppPreferencesDataStore>()
    private val telegramConfigDataStore = mockk<TelegramConfigDataStore>()
    private val emailConfigDataStore = mockk<EmailConfigDataStore>()
    private val twilioConfigDataStore = mockk<TwilioConfigDataStore>()
    private val webhookConfigDataStore = mockk<WebhookConfigDataStore>()
    private val slackWebhookConfigDataStore = mockk<SlackWebhookConfigDataStore>()
    private val discordWebhookConfigDataStore = mockk<DiscordWebhookConfigDataStore>()
    private val moshi = Moshi.Builder().build()

    private lateinit var exporter: ConfigurationExporter

    @Before
    fun setUp() {
        exporter =
            ConfigurationExporter(
                remoteAlertRepository = remoteAlertRepository,
                appPreferencesDataStore = appPreferencesDataStore,
                telegramConfigDataStore = telegramConfigDataStore,
                emailConfigDataStore = emailConfigDataStore,
                twilioConfigDataStore = twilioConfigDataStore,
                webhookConfigDataStore = webhookConfigDataStore,
                slackWebhookConfigDataStore = slackWebhookConfigDataStore,
                discordWebhookConfigDataStore = discordWebhookConfigDataStore,
                moshi = moshi,
            )
    }

    @Test
    fun `exportConfiguration includes alerts preferences and configured notifiers only`() =
        runTest {
            coEvery { remoteAlertRepository.getAllRemoteAlert() } returns
                listOf(
                    RemoteAlert.BatteryAlert(
                        alertId = 11L,
                        batteryPercentage = 18,
                        alertMode = AlertMode.PERIODIC,
                    ),
                    RemoteAlert.StorageAlert(
                        alertId = 12L,
                        storageMinSpaceGb = 4,
                        alertMode = AlertMode.THRESHOLD,
                    ),
                )
            everyFlowDefaults()
            coEvery { telegramConfigDataStore.getConfig() } returns
                AlertMediumConfig.TelegramConfig(botToken = "bot-token", chatId = "chat-id")
            coEvery { emailConfigDataStore.getConfig() } returns
                AlertMediumConfig.EmailConfig(
                    apiKey = "api-key",
                    domain = "mg.example.com",
                    fromEmail = "alerts@example.com",
                    toEmail = "user@example.com",
                )
            coEvery { twilioConfigDataStore.getConfig() } returns
                AlertMediumConfig.TwilioConfig(
                    accountSid = "",
                    authToken = "",
                    fromPhone = "",
                    toPhone = "",
                )
            coEvery { webhookConfigDataStore.getConfig() } returns
                AlertMediumConfig.WebhookConfig("https://hooks.example.com/notify")
            coEvery { slackWebhookConfigDataStore.getConfig() } returns
                AlertMediumConfig.WebhookConfig("")
            coEvery { discordWebhookConfigDataStore.getConfig() } returns
                AlertMediumConfig.DiscordConfig("https://discord.example.com/webhook")

            val result = exporter.exportConfiguration(password = "secret")

            assertThat(result.isSuccess).isTrue()
            val configuration = decodeConfiguration(requireNotNull(result.getOrNull()))
            assertThat(configuration.version).isEqualTo(AppConfiguration.CURRENT_VERSION)
            assertThat(configuration.deviceName).isNotEmpty()
            assertThat(configuration.preferences.workerIntervalMinutes).isEqualTo(45L)
            assertThat(configuration.alerts).hasSize(2)
            assertThat(configuration.alerts[0]).isEqualTo(
                AlertConfig(
                    type = AlertType.BATTERY,
                    batteryPercentage = 18,
                    alertMode = AlertMode.PERIODIC,
                ),
            )
            assertThat(configuration.alerts[1]).isEqualTo(
                AlertConfig(
                    type = AlertType.STORAGE,
                    storageMinSpaceGb = 4,
                    alertMode = AlertMode.THRESHOLD,
                ),
            )

            assertThat(configuration.notifiers.telegram).isNotNull()
            assertThat(configuration.notifiers.email).isNotNull()
            assertThat(configuration.notifiers.webhook).isNotNull()
            assertThat(configuration.notifiers.discord).isNotNull()
            assertThat(configuration.notifiers.twilio).isNull()
            assertThat(configuration.notifiers.slack).isNull()

            val telegramConfig =
                moshi
                    .adapter(TelegramConfigData::class.java)
                    .fromJson(
                        ConfigEncryption.decrypt(
                            configuration.notifiers.telegram!!.data,
                            "secret",
                        ),
                    )
            val emailConfig =
                moshi
                    .adapter(EmailConfigData::class.java)
                    .fromJson(
                        ConfigEncryption.decrypt(
                            configuration.notifiers.email!!.data,
                            "secret",
                        ),
                    )

            assertThat(telegramConfig).isEqualTo(TelegramConfigData(botToken = "bot-token", chatId = "chat-id"))
            assertThat(emailConfig).isEqualTo(EmailConfigData(toEmail = "user@example.com"))
        }

    @Test
    fun `exportConfiguration returns failure when source data load throws`() =
        runTest {
            coEvery { remoteAlertRepository.getAllRemoteAlert() } throws IllegalStateException("db unavailable")

            val result = exporter.exportConfiguration(password = "secret")

            assertThat(result.isFailure).isTrue()
            assertThat(requireNotNull(result.exceptionOrNull()).message).contains("db unavailable")
        }

    private fun everyFlowDefaults() {
        io.mockk.every { appPreferencesDataStore.workerIntervalFlow } returns flowOf(45L)
    }

    private fun decodeConfiguration(json: String): AppConfiguration =
        requireNotNull(moshi.adapter(AppConfiguration::class.java).fromJson(json))
}
