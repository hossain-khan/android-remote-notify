package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import dev.hossain.remotenotify.model.AlertMediumConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WebhookConfigDataStoreTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())
    private lateinit var context: Context
    private lateinit var webhookConfigDataStore: WebhookConfigDataStore
    private val testDataStoreName = "test_webhook_config"

    companion object { // Use a companion object for static initialization
        private var firebaseInitialized = false

        @JvmStatic // Important for JUnit to recognize the @BeforeClass method
        @BeforeClass
        fun setup() {
            // Avoid `./gradlew :app:testReleaseUnitTest` test failure
            // - java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process
            // dev.hossain.remotenotify. Make sure to call FirebaseApp.initializeApp(Context) first.
            if (!firebaseInitialized) {
                val context = ApplicationProvider.getApplicationContext<Context>()
                FirebaseApp.initializeApp(context)
                firebaseInitialized = true
            }
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any existing test files
        File(context.filesDir, "$testDataStoreName.preferences").delete()

        // Create a test-specific DataStore
        val testDataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { context.preferencesDataStoreFile(testDataStoreName) },
            )

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
