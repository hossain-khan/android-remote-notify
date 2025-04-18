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
class WebhookConfigDataStoreTest {
    private lateinit var context: Context
    private lateinit var webhookConfigDataStore: WebhookConfigDataStore
    private val testDataStoreName = "test_webhook_config"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any existing Timber tree to avoid calling into crashlytics
        Timber.uprootAll()

        // Clean up any existing test files
        File(context.filesDir, "$testDataStoreName.preferences").delete()

        // Create test instance with our DataStore
        webhookConfigDataStore = WebhookConfigDataStore(context)
    }

    @After
    fun tearDown() {
        // Clean up the test DataStore file
        File(context.filesDir, "$testDataStoreName.preferences").delete()
    }

    @Test
    fun `saveWebhookUrl saves URL and can be retrieved`() =
        runTest {
            // Given
            val testUrl = "https://example.com/webhook"

            // When
            webhookConfigDataStore.saveWebhookUrl(testUrl)

            // Then
            val result = webhookConfigDataStore.webhookUrl.first()
            assertThat(result).isEqualTo(testUrl)
        }

    @Test
    fun `getConfig returns config with saved URL`() =
        runTest {
            // Given
            val testUrl = "https://example.com/webhook"
            webhookConfigDataStore.saveWebhookUrl(testUrl)

            // When
            val config = webhookConfigDataStore.getConfig()

            // Then
            assertThat(config.url).isEqualTo(testUrl)
        }

    @Test
    fun `getConfig returns empty string when no URL saved`() =
        runTest {
            // Given
            webhookConfigDataStore.clearConfig()

            // When
            val config = webhookConfigDataStore.getConfig()

            // Then
            assertThat(config.url).isEmpty()
        }

    @Test
    fun `clearConfig removes saved URL`() =
        runTest {
            // Given
            val testUrl = "https://example.com/webhook"
            webhookConfigDataStore.saveWebhookUrl(testUrl)
            assertThat(webhookConfigDataStore.webhookUrl.first()).isEqualTo(testUrl)

            // When
            webhookConfigDataStore.clearConfig()

            // Then
            val result = webhookConfigDataStore.webhookUrl.first()
            assertThat(result).isNull()
        }

    @Test
    fun `hasValidConfig returns true when URL is valid`() =
        runTest {
            // Given
            val validUrl = "https://example.com/webhook"
            webhookConfigDataStore.saveWebhookUrl(validUrl)

            // When
            val result = webhookConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `hasValidConfig returns false when URL is missing`() =
        runTest {
            // Given
            webhookConfigDataStore.clearConfig()

            // When
            val result = webhookConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `validateConfig returns valid result for valid URL`() =
        runTest {
            // Given
            val config = AlertMediumConfig.WebhookConfig(url = "https://example.com/webhook")

            // When
            val result = webhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns valid result for HTTP URL`() =
        runTest {
            // Given
            val config = AlertMediumConfig.WebhookConfig(url = "http://example.com/webhook")

            // When
            val result = webhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns errors for invalid URL format`() =
        runTest {
            // Given
            val config = AlertMediumConfig.WebhookConfig(url = "invalid-url")

            // When
            val result = webhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(WebhookConfigDataStore.Companion.ValidationKeys.URL)
        }

    @Test
    fun `validateConfig returns errors for empty URL`() =
        runTest {
            // Given
            val config = AlertMediumConfig.WebhookConfig(url = "")

            // When
            val result = webhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(WebhookConfigDataStore.Companion.ValidationKeys.URL)
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
            val result = webhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
        }
}
