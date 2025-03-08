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
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TwilioConfigDataStoreTest {
    private lateinit var context: Context
    private lateinit var twilioConfigDataStore: TwilioConfigDataStore
    private val testDataStoreName = "test_twilio_config"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any existing test files
        File(context.filesDir, "$testDataStoreName.preferences").delete()

        // Create test instance with our DataStore
        twilioConfigDataStore = TwilioConfigDataStore(context)
    }

    @After
    fun tearDown() {
        // Clean up the test DataStore file
        File(context.filesDir, "$testDataStoreName.preferences").delete()
    }

    @Test
    fun `saveConfig saves all values and can be retrieved`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC12345678901234567890123456789012",
                    authToken = "abcdef1234567890abcdef1234567890",
                    fromPhone = "+12345678900",
                    toPhone = "+12345678901",
                )

            // When
            twilioConfigDataStore.saveConfig(config)

            // Then
            assertThat(twilioConfigDataStore.accountSid.first()).isEqualTo(config.accountSid)
            assertThat(twilioConfigDataStore.authToken.first()).isEqualTo(config.authToken)
            assertThat(twilioConfigDataStore.fromPhone.first()).isEqualTo(config.fromPhone)
            assertThat(twilioConfigDataStore.toPhone.first()).isEqualTo(config.toPhone)
        }

    @Test
    fun `getConfig returns config with saved values`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC12345678901234567890123456789012",
                    authToken = "abcdef1234567890abcdef1234567890",
                    fromPhone = "+12345678900",
                    toPhone = "+12345678901",
                )
            twilioConfigDataStore.saveConfig(config)

            // When
            val retrievedConfig = twilioConfigDataStore.getConfig()

            // Then
            assertThat(retrievedConfig.accountSid).isEqualTo(config.accountSid)
            assertThat(retrievedConfig.authToken).isEqualTo(config.authToken)
            assertThat(retrievedConfig.fromPhone).isEqualTo(config.fromPhone)
            assertThat(retrievedConfig.toPhone).isEqualTo(config.toPhone)
        }

    @Test
    fun `getConfig returns empty strings when nothing saved`() =
        runTest {
            // Given
            twilioConfigDataStore.clearConfig()

            // When
            val config = twilioConfigDataStore.getConfig()

            // Then
            assertThat(config.accountSid).isEmpty()
            assertThat(config.authToken).isEmpty()
            assertThat(config.fromPhone).isEmpty()
            assertThat(config.toPhone).isEmpty()
        }

    @Test
    fun `clearConfig removes all saved values`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC12345678901234567890123456789012",
                    authToken = "abcdef1234567890abcdef1234567890",
                    fromPhone = "+12345678900",
                    toPhone = "+12345678901",
                )
            twilioConfigDataStore.saveConfig(config)

            // When
            twilioConfigDataStore.clearConfig()

            // Then
            assertThat(twilioConfigDataStore.accountSid.first()).isNull()
            assertThat(twilioConfigDataStore.authToken.first()).isNull()
            assertThat(twilioConfigDataStore.fromPhone.first()).isNull()
            assertThat(twilioConfigDataStore.toPhone.first()).isNull()
        }

    @Test
    fun `hasValidConfig returns true when config is valid`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC12345678901234567890123456789012",
                    authToken = "abcdef1234567890abcdef1234567890",
                    fromPhone = "+12345678900",
                    toPhone = "+12345678901",
                )
            twilioConfigDataStore.saveConfig(config)

            // When
            val result = twilioConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `hasValidConfig returns false when config is invalid`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "invalid",
                    authToken = "invalid",
                    fromPhone = "invalid",
                    toPhone = "invalid",
                )
            twilioConfigDataStore.saveConfig(config)

            // When
            val result = twilioConfigDataStore.hasValidConfig()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `validateConfig returns valid result for valid config`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC12345678901234567890123456789012",
                    authToken = "abcdef1234567890abcdef1234567890",
                    fromPhone = "+12345678900",
                    toPhone = "+12345678901",
                )

            // When
            val result = twilioConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateConfig returns errors for invalid account SID`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "invalid",
                    authToken = "abcdef1234567890abcdef1234567890",
                    fromPhone = "+12345678900",
                    toPhone = "+12345678901",
                )

            // When
            val result = twilioConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(TwilioConfigDataStore.Companion.ValidationKeys.ACCOUNT_SID)
        }

    @Test
    fun `validateConfig returns errors for invalid auth token`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC12345678901234567890123456789012",
                    authToken = "invalid",
                    fromPhone = "+12345678900",
                    toPhone = "+12345678901",
                )

            // When
            val result = twilioConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(TwilioConfigDataStore.Companion.ValidationKeys.AUTH_TOKEN)
        }

    @Test
    fun `validateConfig returns errors for invalid from phone`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC12345678901234567890123456789012",
                    authToken = "abcdef1234567890abcdef1234567890",
                    fromPhone = "invalid",
                    toPhone = "+12345678901",
                )

            // When
            val result = twilioConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(TwilioConfigDataStore.Companion.ValidationKeys.FROM_PHONE)
        }

    @Test
    fun `validateConfig returns errors for invalid to phone`() =
        runTest {
            // Given
            val config =
                AlertMediumConfig.TwilioConfig(
                    accountSid = "AC12345678901234567890123456789012",
                    authToken = "abcdef1234567890abcdef1234567890",
                    fromPhone = "+12345678900",
                    toPhone = "invalid",
                )

            // When
            val result = twilioConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(TwilioConfigDataStore.Companion.ValidationKeys.TO_PHONE)
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
            val result = twilioConfigDataStore.validateConfig(config)

            // Then
            assertThat(result.isValid).isFalse()
        }
}
