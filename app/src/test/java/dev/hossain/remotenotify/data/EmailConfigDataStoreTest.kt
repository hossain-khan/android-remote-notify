package dev.hossain.remotenotify.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.notifier.mailgun.MailgunConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EmailConfigDataStoreTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())
    private lateinit var context: Context
    private lateinit var emailConfigDataStore: EmailConfigDataStore
    private val testDataStoreName = "test_email_config"

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
        emailConfigDataStore = EmailConfigDataStore(context)
    }

    @After
    fun tearDown() {
        // Clean up the test DataStore file
        File(context.filesDir, "$testDataStoreName.preferences").delete()
    }

    @Test
    fun `saveConfig saves email and can be retrieved`() =
        runTest {
            // Given
            val testEmail = "test@example.com"
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = "test-api-key",
                    domain = "example.com",
                    fromEmail = "from@example.com",
                    toEmail = testEmail,
                )

            // When
            emailConfigDataStore.saveConfig(config)

            // Then
            val result = emailConfigDataStore.toEmail.first()
            assertThat(result).isEqualTo(testEmail)
        }

    @Test
    fun `getConfig returns config with saved email and default values`() =
        runTest {
            // Given
            val testEmail = "test@example.com"
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = "any-key", // Will be overridden with MailgunConfig constants
                    domain = "any-domain", // Will be overridden with MailgunConfig constants
                    fromEmail = "any@example.com", // Will be overridden with MailgunConfig constants
                    toEmail = testEmail,
                )
            emailConfigDataStore.saveConfig(config)

            // When
            val retrievedConfig = emailConfigDataStore.getConfig()

            // Then
            assertThat(retrievedConfig.toEmail).isEqualTo(testEmail)
            assertThat(retrievedConfig.apiKey).isEqualTo(MailgunConfig.API_KEY)
            assertThat(retrievedConfig.domain).isEqualTo(MailgunConfig.DOMAIN)
            assertThat(retrievedConfig.fromEmail).isEqualTo(MailgunConfig.FROM_EMAIL)
        }

    @Test
    fun `getConfig returns empty string for toEmail when nothing saved`() =
        runTest {
            // Given
            emailConfigDataStore.clearConfig()

            // When
            val config = emailConfigDataStore.getConfig()

            // Then
            assertThat(config.toEmail).isEmpty()
            // These values should still be the defaults from MailgunConfig
            assertThat(config.apiKey).isEqualTo(MailgunConfig.API_KEY)
            assertThat(config.domain).isEqualTo(MailgunConfig.DOMAIN)
            assertThat(config.fromEmail).isEqualTo(MailgunConfig.FROM_EMAIL)
        }

    @Test
    fun `clearConfig removes saved email`() =
        runTest {
            // Given
            val testEmail = "test@example.com"
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = "test-api-key",
                    domain = "example.com",
                    fromEmail = "from@example.com",
                    toEmail = testEmail,
                )
            emailConfigDataStore.saveConfig(config)
            assertThat(emailConfigDataStore.toEmail.first()).isEqualTo(testEmail)

            // When
            emailConfigDataStore.clearConfig()

            // Then
            val result = emailConfigDataStore.toEmail.first()
            assertThat(result).isNull()
        }

    @Test
    fun `hasValidConfig returns true when email is valid`() =
        runTest {
            // Given
            val validEmail = "valid@example.com"
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = MailgunConfig.API_KEY,
                    domain = MailgunConfig.DOMAIN,
                    fromEmail = MailgunConfig.FROM_EMAIL,
                    toEmail = validEmail,
                )
            emailConfigDataStore.saveConfig(config)

            // When
            val result = emailConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `hasValidConfig returns false when email is missing`() =
        runTest {
            // Given
            emailConfigDataStore.clearConfig()

            // When
            val result = emailConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `validateConfig returns valid result for valid email`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = MailgunConfig.API_KEY,
                    domain = MailgunConfig.DOMAIN,
                    fromEmail = MailgunConfig.FROM_EMAIL,
                    toEmail = "valid@example.com",
                )

            // When
            val result = emailConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns errors for invalid email format`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = MailgunConfig.API_KEY,
                    domain = MailgunConfig.DOMAIN,
                    fromEmail = MailgunConfig.FROM_EMAIL,
                    toEmail = "invalid-email",
                )

            // When
            val result = emailConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(EmailConfigDataStore.Companion.ValidationKeys.TO_EMAIL)
        }

    @Test
    fun `validateConfig returns errors for empty email`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = MailgunConfig.API_KEY,
                    domain = MailgunConfig.DOMAIN,
                    fromEmail = MailgunConfig.FROM_EMAIL,
                    toEmail = "",
                )

            // When
            val result = emailConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(EmailConfigDataStore.Companion.ValidationKeys.TO_EMAIL)
        }

    @Test
    fun `validateConfig fails for wrong config type`() =
        runTest {
            // Given
            val config = AlertMediumConfig.WebhookConfig(url = "https://example.com")

            // When
            val result = emailConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
        }
}
