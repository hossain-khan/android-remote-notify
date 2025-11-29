package dev.hossain.remotenotify.data.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.squareup.moshi.Moshi
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.DiscordWebhookConfigDataStore
import dev.hossain.remotenotify.data.EmailConfigDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.data.SlackWebhookConfigDataStore
import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.data.TwilioConfigDataStore
import dev.hossain.remotenotify.data.WebhookConfigDataStore
import dev.hossain.remotenotify.model.AlertType
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
class ConfigurationImporterValidationTest {
    private lateinit var moshi: Moshi
    private lateinit var importer: ConfigurationImporter

    @Before
    fun setUp() {
        // Initialize Firebase if not already done - needed for release tests
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        // Clean up any existing Timber tree to avoid calling into crashlytics
        Timber.uprootAll()
        moshi = Moshi.Builder().build()
        importer =
            ConfigurationImporter(
                remoteAlertRepository = mockk<RemoteAlertRepository>(relaxed = true),
                appPreferencesDataStore = mockk<AppPreferencesDataStore>(relaxed = true),
                telegramConfigDataStore = mockk<TelegramConfigDataStore>(relaxed = true),
                emailConfigDataStore = mockk<EmailConfigDataStore>(relaxed = true),
                twilioConfigDataStore = mockk<TwilioConfigDataStore>(relaxed = true),
                webhookConfigDataStore = mockk<WebhookConfigDataStore>(relaxed = true),
                slackWebhookConfigDataStore = mockk<SlackWebhookConfigDataStore>(relaxed = true),
                discordWebhookConfigDataStore = mockk<DiscordWebhookConfigDataStore>(relaxed = true),
                moshi = moshi,
            )
    }

    @Test
    fun `parseAndValidate returns Invalid for malformed JSON`() {
        val result = importer.parseAndValidate("not valid json")

        assertThat(result).isInstanceOf(ImportValidationResult.Invalid::class.java)
        val invalid = result as ImportValidationResult.Invalid
        assertThat(invalid.errors).isNotEmpty()
    }

    @Test
    fun `parseAndValidate returns Invalid for unsupported version`() {
        val json =
            """
            {
                "version": 999,
                "exportedAt": 1234567890,
                "alerts": [],
                "notifiers": {},
                "preferences": {}
            }
            """.trimIndent()

        val result = importer.parseAndValidate(json)

        assertThat(result).isInstanceOf(ImportValidationResult.Invalid::class.java)
        val invalid = result as ImportValidationResult.Invalid
        assertThat(invalid.errors.any { it.contains("version") }).isTrue()
    }

    @Test
    fun `parseAndValidate returns Invalid for battery alert without percentage`() {
        val json =
            """
            {
                "version": 1,
                "exportedAt": 1234567890,
                "alerts": [
                    {
                        "type": "BATTERY"
                    }
                ],
                "notifiers": {},
                "preferences": {}
            }
            """.trimIndent()

        val result = importer.parseAndValidate(json)

        assertThat(result).isInstanceOf(ImportValidationResult.Invalid::class.java)
        val invalid = result as ImportValidationResult.Invalid
        assertThat(invalid.errors.any { it.contains("batteryPercentage") }).isTrue()
    }

    @Test
    fun `parseAndValidate returns Invalid for invalid battery percentage`() {
        val json =
            """
            {
                "version": 1,
                "exportedAt": 1234567890,
                "alerts": [
                    {
                        "type": "BATTERY",
                        "batteryPercentage": 150
                    }
                ],
                "notifiers": {},
                "preferences": {}
            }
            """.trimIndent()

        val result = importer.parseAndValidate(json)

        assertThat(result).isInstanceOf(ImportValidationResult.Invalid::class.java)
        val invalid = result as ImportValidationResult.Invalid
        assertThat(invalid.errors.any { it.contains("Invalid battery percentage") }).isTrue()
    }

    @Test
    fun `parseAndValidate returns Invalid for storage alert without space`() {
        val json =
            """
            {
                "version": 1,
                "exportedAt": 1234567890,
                "alerts": [
                    {
                        "type": "STORAGE"
                    }
                ],
                "notifiers": {},
                "preferences": {}
            }
            """.trimIndent()

        val result = importer.parseAndValidate(json)

        assertThat(result).isInstanceOf(ImportValidationResult.Invalid::class.java)
        val invalid = result as ImportValidationResult.Invalid
        assertThat(invalid.errors.any { it.contains("storageMinSpaceGb") }).isTrue()
    }

    @Test
    fun `parseAndValidate returns Valid for valid configuration`() {
        val json =
            """
            {
                "version": 1,
                "exportedAt": 1234567890,
                "alerts": [
                    {
                        "type": "BATTERY",
                        "batteryPercentage": 20
                    },
                    {
                        "type": "STORAGE",
                        "storageMinSpaceGb": 5
                    }
                ],
                "notifiers": {},
                "preferences": {
                    "workerIntervalMinutes": 60
                }
            }
            """.trimIndent()

        val result = importer.parseAndValidate(json)

        assertThat(result).isInstanceOf(ImportValidationResult.Valid::class.java)
        val valid = result as ImportValidationResult.Valid
        assertThat(valid.configuration.alerts).hasSize(2)
        assertThat(valid.configuration.alerts[0].type).isEqualTo(AlertType.BATTERY)
        assertThat(valid.configuration.alerts[0].batteryPercentage).isEqualTo(20)
        assertThat(valid.configuration.alerts[1].type).isEqualTo(AlertType.STORAGE)
        assertThat(valid.configuration.alerts[1].storageMinSpaceGb).isEqualTo(5)
    }

    @Test
    fun `parseAndValidate returns Valid for empty configuration`() {
        val json =
            """
            {
                "version": 1,
                "exportedAt": 1234567890,
                "alerts": [],
                "notifiers": {},
                "preferences": {}
            }
            """.trimIndent()

        val result = importer.parseAndValidate(json)

        assertThat(result).isInstanceOf(ImportValidationResult.Valid::class.java)
    }

    @Test
    fun `testPassword returns true when no encrypted configs`() {
        val config =
            AppConfiguration(
                alerts = listOf(AlertConfig(type = AlertType.BATTERY, batteryPercentage = 20)),
                notifiers = NotifierConfigs(),
            )

        val result = importer.testPassword(config, "anyPassword")

        assertThat(result).isTrue()
    }

    @Test
    fun `testPassword returns true with correct password`() {
        val password = "correctPassword"
        val encryptedData = ConfigEncryption.encrypt("""{"botToken":"test","chatId":"test"}""", password)
        val config =
            AppConfiguration(
                notifiers =
                    NotifierConfigs(
                        telegram = EncryptedConfig(encrypted = true, data = encryptedData),
                    ),
            )

        val result = importer.testPassword(config, password)

        assertThat(result).isTrue()
    }

    @Test
    fun `testPassword returns false with wrong password`() {
        val encryptedData = ConfigEncryption.encrypt("""{"botToken":"test","chatId":"test"}""", "correctPassword")
        val config =
            AppConfiguration(
                notifiers =
                    NotifierConfigs(
                        telegram = EncryptedConfig(encrypted = true, data = encryptedData),
                    ),
            )

        val result = importer.testPassword(config, "wrongPassword")

        assertThat(result).isFalse()
    }
}
