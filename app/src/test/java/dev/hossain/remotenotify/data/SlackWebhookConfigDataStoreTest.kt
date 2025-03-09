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
class SlackWebhookConfigDataStoreTest {
    private lateinit var context: Context
    private lateinit var slackWebhookConfigDataStore: SlackWebhookConfigDataStore
    private val testDataStoreName = "test_slack_webhook_config"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any existing Timber tree to avoid calling into crashlytics
        Timber.uprootAll()

        // Clean up any existing test files
        File(context.filesDir, "$testDataStoreName.preferences").delete()

        // Create test instance
        slackWebhookConfigDataStore = SlackWebhookConfigDataStore(context)
    }

    @After
    fun tearDown() {
        // Clean up the test DataStore file
        File(context.filesDir, "$testDataStoreName.preferences").delete()
    }

    @Test
    fun `saveSlackWorkflowWebhookUrl saves URL and can be retrieved`() =
        runTest {
            // Given
            val testUrl = "https://hooks.slack.com/triggers/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"

            // When
            slackWebhookConfigDataStore.saveSlackWorkflowWebhookUrl(testUrl)

            // Then
            val result = slackWebhookConfigDataStore.slackWorkflowWebhookUrl.first()
            assertThat(result).isEqualTo(testUrl)
        }

    @Test
    fun `getConfig returns config with saved URL`() =
        runTest {
            // Given
            val testUrl = "https://hooks.slack.com/triggers/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"
            slackWebhookConfigDataStore.saveSlackWorkflowWebhookUrl(testUrl)

            // When
            val config = slackWebhookConfigDataStore.getConfig()

            // Then
            assertThat(config.url).isEqualTo(testUrl)
        }

    @Test
    fun `getConfig returns empty string when no URL saved`() =
        runTest {
            // Given
            slackWebhookConfigDataStore.clearConfig()

            // When
            val config = slackWebhookConfigDataStore.getConfig()

            // Then
            assertThat(config.url).isEmpty()
        }

    @Test
    fun `clearConfig removes saved URL`() =
        runTest {
            // Given
            val testUrl = "https://hooks.slack.com/triggers/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"
            slackWebhookConfigDataStore.saveSlackWorkflowWebhookUrl(testUrl)
            assertThat(slackWebhookConfigDataStore.slackWorkflowWebhookUrl.first()).isEqualTo(testUrl)

            // When
            slackWebhookConfigDataStore.clearConfig()

            // Then
            val result = slackWebhookConfigDataStore.slackWorkflowWebhookUrl.first()
            assertThat(result).isNull()
        }

    @Test
    fun `hasValidConfig returns true when URL is valid`() =
        runTest {
            // Given
            val validUrl = "https://hooks.slack.com/triggers/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"
            slackWebhookConfigDataStore.saveSlackWorkflowWebhookUrl(validUrl)

            // When
            val result = slackWebhookConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `hasValidConfig returns false when URL is missing`() =
        runTest {
            // Given
            slackWebhookConfigDataStore.clearConfig()

            // When
            val result = slackWebhookConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `validateConfig returns valid result for valid triggers URL`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.WebhookConfig(
                    url = "https://hooks.slack.com/triggers/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",
                )

            // When
            val result = slackWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns valid result for valid services URL`() =
        runTest {
            // Given
            val config = AlertMediumConfig.WebhookConfig(url = "https://hooks.slack.com/services/T12345ZX6/B12345678/abcdefgh123456789")

            // When
            val result = slackWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns errors for invalid URL format`() =
        runTest {
            // Given
            val config = AlertMediumConfig.WebhookConfig(url = "https://slack.com/api/webhook")

            // When
            val result = slackWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(SlackWebhookConfigDataStore.Companion.ValidationKeys.URL)
        }

    @Test
    fun `validateConfig returns errors for empty URL`() =
        runTest {
            // Given
            val config = AlertMediumConfig.WebhookConfig(url = "")

            // When
            val result = slackWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(SlackWebhookConfigDataStore.Companion.ValidationKeys.URL)
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
            val result = slackWebhookConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
        }
}
