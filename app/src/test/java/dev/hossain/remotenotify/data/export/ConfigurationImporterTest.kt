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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigurationImporterTest {
    private val remoteAlertRepository = mockk<RemoteAlertRepository>()
    private val appPreferencesDataStore = mockk<AppPreferencesDataStore>()
    private val telegramConfigDataStore = mockk<TelegramConfigDataStore>()
    private val emailConfigDataStore = mockk<EmailConfigDataStore>()
    private val twilioConfigDataStore = mockk<TwilioConfigDataStore>()
    private val webhookConfigDataStore = mockk<WebhookConfigDataStore>()
    private val slackWebhookConfigDataStore = mockk<SlackWebhookConfigDataStore>()
    private val discordWebhookConfigDataStore = mockk<DiscordWebhookConfigDataStore>()
    private val moshi = Moshi.Builder().build()

    private lateinit var importer: ConfigurationImporter

    @Before
    fun setUp() {
        importer =
            ConfigurationImporter(
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

        coEvery { remoteAlertRepository.saveRemoteAlert(any()) } just runs
        coEvery { remoteAlertRepository.deleteRemoteAlert(any()) } just runs
        coEvery { remoteAlertRepository.getAllRemoteAlert() } returns emptyList()
        coEvery { telegramConfigDataStore.clearConfig() } just runs
        coEvery { emailConfigDataStore.clearConfig() } just runs
        coEvery { twilioConfigDataStore.clearConfig() } just runs
        coEvery { webhookConfigDataStore.clearConfig() } just runs
        coEvery { slackWebhookConfigDataStore.clearConfig() } just runs
        coEvery { discordWebhookConfigDataStore.clearConfig() } just runs
        coEvery { telegramConfigDataStore.saveBotToken(any()) } just runs
        coEvery { telegramConfigDataStore.saveChatId(any()) } just runs
        coEvery { emailConfigDataStore.saveConfig(any()) } just runs
        coEvery { twilioConfigDataStore.saveConfig(any()) } just runs
        coEvery { webhookConfigDataStore.saveWebhookUrl(any()) } just runs
        coEvery { slackWebhookConfigDataStore.saveSlackWorkflowWebhookUrl(any()) } just runs
        coEvery { discordWebhookConfigDataStore.saveDiscordWebhookUrl(any()) } just runs
        coEvery { appPreferencesDataStore.saveWorkerInterval(any()) } just runs
    }

    @Test
    fun `parseAndValidate returns invalid for unsupported version and malformed alerts`() {
        val json =
            moshi.adapter(AppConfiguration::class.java).toJson(
                AppConfiguration(
                    version = 99,
                    alerts =
                        listOf(
                            AlertConfig(
                                type = AlertType.BATTERY,
                                batteryPercentage = 120,
                                alertMode = AlertMode.THRESHOLD,
                            ),
                            AlertConfig(
                                type = AlertType.STORAGE,
                                storageMinSpaceGb = -2,
                                alertMode = AlertMode.THRESHOLD,
                            ),
                        ),
                ),
            )

        val result = importer.parseAndValidate(json)

        assertThat(result).isInstanceOf(ImportValidationResult.Invalid::class.java)
        val errors = (result as ImportValidationResult.Invalid).errors
        assertThat(errors).contains("Unsupported configuration version: 99. Supported versions: 1-2")
        assertThat(errors).contains("Invalid battery percentage: 120")
        assertThat(errors).contains("Invalid storage space: -2")
    }

    @Test
    fun `testPassword returns false for wrong password and true when config is unencrypted`() {
        val encryptedConfiguration =
            AppConfiguration(
                notifiers =
                    NotifierConfigs(
                        telegram =
                            EncryptedConfig(
                                encrypted = true,
                                data = ConfigEncryption.encrypt("""{"botToken":"token","chatId":"chat"}""", "correct"),
                            ),
                    ),
            )

        assertThat(importer.testPassword(encryptedConfiguration, "wrong")).isFalse()

        val plaintextConfiguration =
            AppConfiguration(
                notifiers = NotifierConfigs(webhook = EncryptedConfig(encrypted = false, data = """{"url":"https://example.com"}""")),
            )
        assertThat(importer.testPassword(plaintextConfiguration, "anything")).isTrue()
    }

    @Test
    fun `importConfiguration clears existing data and saves decrypted notifier and preference data`() =
        runTest {
            val existingAlert = RemoteAlert.BatteryAlert(alertId = 9L, batteryPercentage = 10)
            coEvery { remoteAlertRepository.getAllRemoteAlert() } returns listOf(existingAlert)
            val configuration =
                AppConfiguration(
                    alerts =
                        listOf(
                            AlertConfig(
                                type = AlertType.BATTERY,
                                batteryPercentage = 25,
                                alertMode = AlertMode.PERIODIC,
                            ),
                        ),
                    notifiers =
                        NotifierConfigs(
                            telegram =
                                EncryptedConfig(
                                    encrypted = true,
                                    data = ConfigEncryption.encrypt("""{"botToken":"token","chatId":"chat"}""", "secret"),
                                ),
                            email =
                                EncryptedConfig(
                                    encrypted = false,
                                    data = """{"toEmail":"user@example.com"}""",
                                ),
                            webhook =
                                EncryptedConfig(
                                    encrypted = false,
                                    data = """{"url":"https://hooks.example.com/post"}""",
                                ),
                        ),
                    preferences = AppPreferences(workerIntervalMinutes = 60),
                )

            val result = importer.importConfiguration(configuration, password = "secret", replaceExisting = true)

            assertThat(result).isEqualTo(ConfigOperationResult.Success)
            coVerify { remoteAlertRepository.deleteRemoteAlert(existingAlert) }
            coVerify {
                remoteAlertRepository.saveRemoteAlert(
                    RemoteAlert.BatteryAlert(
                        batteryPercentage = 25,
                        alertMode = AlertMode.PERIODIC,
                    ),
                )
            }
            coVerify { telegramConfigDataStore.saveBotToken("token") }
            coVerify { telegramConfigDataStore.saveChatId("chat") }
            coVerify {
                emailConfigDataStore.saveConfig(
                    AlertMediumConfig.EmailConfig(
                        apiKey = "",
                        domain = "",
                        fromEmail = "",
                        toEmail = "user@example.com",
                    ),
                )
            }
            coVerify { webhookConfigDataStore.saveWebhookUrl("https://hooks.example.com/post") }
            coVerify { appPreferencesDataStore.saveWorkerInterval(60) }
        }

    @Test
    fun `importConfiguration without replaceExisting skips clearing and uses fallback alert values`() =
        runTest {
            val configuration =
                AppConfiguration(
                    alerts =
                        listOf(
                            AlertConfig(
                                type = AlertType.BATTERY,
                                batteryPercentage = null,
                                alertMode = AlertMode.PERIODIC,
                            ),
                            AlertConfig(
                                type = AlertType.STORAGE,
                                storageMinSpaceGb = null,
                                alertMode = AlertMode.PERIODIC,
                            ),
                        ),
                )

            val result = importer.importConfiguration(configuration, password = "secret", replaceExisting = false)

            assertThat(result).isEqualTo(ConfigOperationResult.Success)
            coVerify(exactly = 0) { remoteAlertRepository.getAllRemoteAlert() }
            coVerify(exactly = 0) { telegramConfigDataStore.clearConfig() }
            coVerify {
                remoteAlertRepository.saveRemoteAlert(
                    RemoteAlert.BatteryAlert(
                        batteryPercentage = 20,
                        alertMode = AlertMode.PERIODIC,
                    ),
                )
            }
            coVerify {
                remoteAlertRepository.saveRemoteAlert(
                    RemoteAlert.StorageAlert(
                        storageMinSpaceGb = 1,
                        alertMode = AlertMode.PERIODIC,
                    ),
                )
            }
        }
}
