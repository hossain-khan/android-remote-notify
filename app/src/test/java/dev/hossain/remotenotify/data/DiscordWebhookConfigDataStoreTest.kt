package dev.hossain.remotenotify.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.model.AlertMediumConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DiscordWebhookConfigDataStoreTest {
    private lateinit var context: Context
    private lateinit var discordWebhookConfigDataStore: DiscordWebhookConfigDataStore
    private val testDataStoreName = "test_discord_webhook_config"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any existing Timber tree to avoid calling into crashlytics
        Timber.uprootAll()

        // Clean up any existing test files
        File(context.filesDir, "$testDataStoreName.preferences").delete()

        // Create test instance
        discordWebhookConfigDataStore = DiscordWebhookConfigDataStore(context)
    }

    @After
    fun tearDown() {
        // Clean up the test DataStore file
        File(context.filesDir, "$testDataStoreName.preferences").delete()
    }

    @Test
    fun `saveDiscordWebhookUrl saves URL and can be retrieved`() =
        runTest {
            // Given
            val testUrl = "https://discord.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz123456"

            // When
            discordWebhookConfigDataStore.saveDiscordWebhookUrl(testUrl)

            // Then
            val result = discordWebhookConfigDataStore.discordWebhookUrl.first()
            assertThat(result).isEqualTo(testUrl)
        }

    @Test
    fun `getConfig returns config with saved URL`() =
        runTest {
            // Given
            val testUrl = "https://discord.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz123456"
            discordWebhookConfigDataStore.saveDiscordWebhookUrl(testUrl)

            // When
            val config = discordWebhookConfigDataStore.getConfig()

            // Then
            assertThat(config.webhookUrl).isEqualTo(testUrl)
        }

    @Test
    fun `getConfig returns empty string when no URL saved`() =
        runTest {
            // Given
            discordWebhookConfigDataStore.clearConfig()

            // When
            val config = discordWebhookConfigDataStore.getConfig()

            // Then
            assertThat(config.webhookUrl).isEmpty()
        }

    @Test
    fun `clearConfig removes saved URL`() =
        runTest {
            // Given
            val testUrl = "https://discord.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz123456"
            discordWebhookConfigDataStore.saveDiscordWebhookUrl(testUrl)
            assertThat(discordWebhookConfigDataStore.discordWebhookUrl.first()).isEqualTo(testUrl)

            // When
            discordWebhookConfigDataStore.clearConfig()

            // Then
            val result = discordWebhookConfigDataStore.discordWebhookUrl.first()
            assertThat(result).isNull()
        }

    @Test
    fun `hasValidConfig returns true when URL is valid`() =
        runTest {
            // Given
            val validUrl = "https://discord.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz123456"
            discordWebhookConfigDataStore.saveDiscordWebhookUrl(validUrl)

            // When
            val result = discordWebhookConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `hasValidConfig returns false when URL is missing`() =
        runTest {
            // Given
            discordWebhookConfigDataStore.clearConfig()

            // When
            val result = discordWebhookConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `validateConfig returns valid result for valid discord URL`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.DiscordConfig(
                    webhookUrl = "https://discord.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz123456",
                )

            // When
            val result = discordWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns valid result for legacy discordapp URL`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.DiscordConfig(
                    webhookUrl = "https://discordapp.com/api/webhooks/1234567890123456789/abcdefghijklmnopqrstuvwxyz123456",
                )

            // When
            val result = discordWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns errors for invalid URL format`() =
        runTest {
            // Given
            val config = AlertMediumConfig.DiscordConfig(webhookUrl = "https://discord.com/api/channels/123")

            // When
            val result = discordWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(DiscordWebhookConfigDataStore.Companion.ValidationKeys.URL)
        }

    @Test
    fun `validateConfig returns errors for empty URL`() =
        runTest {
            // Given
            val config = AlertMediumConfig.DiscordConfig(webhookUrl = "")

            // When
            val result = discordWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(DiscordWebhookConfigDataStore.Companion.ValidationKeys.URL)
        }

    @Test
    fun `validateConfig fails for wrong config type`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = "123",
                    domain = "example.com",
                    fromEmail = "from@example.com",
                    toEmail = "to@example.com",
                )

            // When
            val result = discordWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
        }

    @Test
    fun `validateConfig returns errors for non-discord URL`() =
        runTest {
            // Given
            val config = AlertMediumConfig.DiscordConfig(webhookUrl = "https://example.com/webhook")

            // When
            val result = discordWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(DiscordWebhookConfigDataStore.Companion.ValidationKeys.URL)
        }
}
