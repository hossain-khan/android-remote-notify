package dev.hossain.remotenotify.ui.alertmediumconfig

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.TestNavGraphs
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.EmailQuotaManager
import dev.hossain.remotenotify.model.NotificationSender
import dev.hossain.remotenotify.model.NotifierType
import dev.hossain.remotenotify.utils.AlertFormatter
import dev.hossain.remotenotify.utils.NotifierUtils.getNotifier
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Tests for [ConfigureNotificationMediumPresenter].
 * This test class is parameterized to run tests for each [NotifierType].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class ConfigureNotificationMediumPresenterTest(private val currentNotifierType: NotifierType) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "NotifierType = {0}")
        fun data(): Collection<NotifierType> {
            return NotifierType.values().filter { it != NotifierType.UNKNOWN } // Exclude UNKNOWN as it's not configurable
        }
    }

    private lateinit var navigator: Navigator
    private lateinit var mockNotifiers: Set<@JvmSuppressWildcards NotificationSender>
    private lateinit var mockNotifier: NotificationSender // Specific mock for the currentNotifierType
    private lateinit var emailQuotaManager: EmailQuotaManager
    private lateinit var alertFormatter: AlertFormatter
    private lateinit var analytics: Analytics
    private lateinit var screen: ConfigureNotificationMediumScreen

    // Sample configs based on notifier type
    private fun getSampleConfig(type: NotifierType): AlertMediumConfig {
        return when (type) {
            NotifierType.EMAIL -> AlertMediumConfig.EmailConfig(listOf("test@example.com"))
            NotifierType.WEBHOOK_SLACK_WORKFLOW -> AlertMediumConfig.WebhookConfig("https://hooks.slack.com/workflows/...")
            NotifierType.WEBHOOK_REST_API -> AlertMediumConfig.WebhookConfig("https://example.com/api/notify")
            NotifierType.TELEGRAM -> AlertMediumConfig.TelegramConfig("12345:bot_token", "channel_id")
            NotifierType.TWILIO -> AlertMediumConfig.TwilioConfig("AC123", "auth_token", "+15551234567", "+15557654321")
            else -> throw IllegalArgumentException("Unsupported notifier type for sample config: $type")
        }
    }
    private fun getInvalidSampleConfig(type: NotifierType): AlertMediumConfig {
         return when (type) {
            NotifierType.EMAIL -> AlertMediumConfig.EmailConfig(listOf("invalid-email"))
            NotifierType.WEBHOOK_SLACK_WORKFLOW -> AlertMediumConfig.WebhookConfig("invalid-url")
            NotifierType.WEBHOOK_REST_API -> AlertMediumConfig.WebhookConfig("invalid-url")
            NotifierType.TELEGRAM -> AlertMediumConfig.TelegramConfig("", "")
            NotifierType.TWILIO -> AlertMediumConfig.TwilioConfig("", "", "", "")
            else -> throw IllegalArgumentException("Unsupported notifier type for sample config: $type")
        }
    }

    private val sampleJsonPayload = "{ \"message\": \"Test notification\" }"

    @Before
    fun setUp() {
        navigator = mockk(relaxed = true)
        mockNotifier = mockk(relaxed = true) // Individual mock for the parameterized type
        mockNotifiers = setOf(mockNotifier) // Set containing only the current type's mock
        emailQuotaManager = mockk(relaxed = true)
        alertFormatter = mockk()
        analytics = mockk(relaxed = true)
        screen = ConfigureNotificationMediumScreen(currentNotifierType)


        // Mock static NavGraphs
        mockkStatic(TestNavGraphs::class) // Assuming TestNavGraphs.Root exists
        every { TestNavGraphs.root } returns mockk()


        // Mock static getNotifier to return our specific mockNotifier for the current type
        mockkStatic("dev.hossain.remotenotify.utils.NotifierUtilsKt")
        every { mockNotifiers.getNotifier(currentNotifierType) } returns mockNotifier

        // Default mock for alertFormatter for webhook types
        if (currentNotifierType.name.startsWith("WEBHOOK")) {
            every { alertFormatter.format(any(), any(), any(), any()) } returns sampleJsonPayload
        }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createPresenter(): ConfigureNotificationMediumPresenter {
        return ConfigureNotificationMediumPresenter(
            screen = screen,
            navigator = navigator,
            notifiers = mockNotifiers,
            emailQuotaManager = emailQuotaManager,
            alertFormatter = alertFormatter,
            analytics = analytics
        )
    }

    // region Initial State Tests
    @Test
    fun `initial state - when notifier IS configured`() = runTest {
        val sampleConfig = getSampleConfig(currentNotifierType)
        every { mockNotifier.hasValidConfig() } returns true
        every { mockNotifier.getConfig() } returns sampleConfig
        every { mockNotifier.validateConfig(sampleConfig) } returns ConfigValidationResult(true)

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            assertThat(state.notifierType).isEqualTo(currentNotifierType)
            assertThat(state.isConfigured).isTrue()
            assertThat(state.configValidationResult.isValid).isTrue()
            assertThat(state.alertMediumConfig).isEqualTo(sampleConfig)
            assertThat(state.showValidationError).isFalse()
            if (currentNotifierType.name.startsWith("WEBHOOK")) {
                assertThat(state.sampleJsonPayload).isEqualTo(sampleJsonPayload)
                verify { alertFormatter.format(any(), any(), any(), any()) }
            } else {
                assertThat(state.sampleJsonPayload).isNull()
            }
            assertThat(state.snackbarMessage).isNull()

            coVerify { analytics.logScreenView(ConfigureNotificationMediumScreen::class) } // Impression
        }
    }

    @Test
    fun `initial state - when notifier IS NOT configured`() = runTest {
        every { mockNotifier.hasValidConfig() } returns false
        every { mockNotifier.getConfig() } returns null // No config when not configured
        // Validate with a default/empty config for the type if necessary, or expect null
        val defaultConfigForType = when(currentNotifierType) {
            NotifierType.EMAIL -> AlertMediumConfig.EmailConfig()
            NotifierType.WEBHOOK_SLACK_WORKFLOW, NotifierType.WEBHOOK_REST_API -> AlertMediumConfig.WebhookConfig()
            NotifierType.TELEGRAM -> AlertMediumConfig.TelegramConfig()
            NotifierType.TWILIO -> AlertMediumConfig.TwilioConfig()
            else -> null
        }
        every { mockNotifier.validateConfig(defaultConfigForType) } returns ConfigValidationResult(false, R.string.error_invalid_config_generic)


        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            assertThat(state.notifierType).isEqualTo(currentNotifierType)
            assertThat(state.isConfigured).isFalse()
            assertThat(state.configValidationResult.isValid).isFalse() // Default validation for empty config
            assertThat(state.alertMediumConfig).isEqualTo(defaultConfigForType) // Should be default/empty
            assertThat(state.showValidationError).isFalse()
            if (currentNotifierType.name.startsWith("WEBHOOK")) {
                assertThat(state.sampleJsonPayload).isEqualTo(sampleJsonPayload)
            }
            assertThat(state.snackbarMessage).isNull()
            coVerify { analytics.logScreenView(ConfigureNotificationMediumScreen::class) } // Impression
        }
    }
    // endregion Initial State Tests

    // region Event Handling Tests
    @Test
    fun `event UpdateConfigValue - updates config, resets validation error, re-validates`() = runTest {
        val initialConfig = getInvalidSampleConfig(currentNotifierType) // Start with some config
        val newConfig = getSampleConfig(currentNotifierType)
        val validationResultForNewConfig = ConfigValidationResult(true)

        every { mockNotifier.getConfig() } returns initialConfig // Initial config
        every { mockNotifier.validateConfig(initialConfig) } returns ConfigValidationResult(false, R.string.error_invalid_config_generic)
        every { mockNotifier.validateConfig(newConfig) } returns validationResultForNewConfig

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            var state = awaitItem() // Initial state
            // Simulate showing validation error from a previous failed save attempt
            state.eventSink(ConfigureNotificationMediumScreenEvent.SaveConfig(initialConfig)) // This will set showValidationError = true
            state = awaitItem()
            assertThat(state.showValidationError).isTrue()


            state.eventSink(ConfigureNotificationMediumScreenEvent.UpdateConfigValue(newConfig))
            state = awaitItem()

            assertThat(state.alertMediumConfig).isEqualTo(newConfig)
            assertThat(state.showValidationError).isFalse() // Should reset
            assertThat(state.configValidationResult).isEqualTo(validationResultForNewConfig)
            verify { mockNotifier.validateConfig(newConfig) }
        }
    }

    @Test
    fun `event SaveConfig - validation fails - shows error, does not save or pop`() = runTest {
        val invalidConfig = getInvalidSampleConfig(currentNotifierType)
        every { mockNotifier.validateConfig(invalidConfig) } returns ConfigValidationResult(false, R.string.error_invalid_config_url)

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.SaveConfig(invalidConfig))
            val updatedState = awaitItem()

            assertThat(updatedState.showValidationError).isTrue()
            assertThat(updatedState.configValidationResult.errorMessageRes).isEqualTo(R.string.error_invalid_config_url)
            coVerify(exactly = 0) { mockNotifier.saveConfig(any()) }
            verify(exactly = 0) { navigator.pop(any()) }
        }
    }

    @Test
    fun `event SaveConfig - validation succeeds, save fails - shows snackbar error, does not pop`() = runTest {
        val validConfig = getSampleConfig(currentNotifierType)
        every { mockNotifier.validateConfig(validConfig) } returns ConfigValidationResult(true)
        coEvery { mockNotifier.saveConfig(validConfig) } throws RuntimeException("Save failed")

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.SaveConfig(validConfig))
            val updatedState = awaitItem()

            assertThat(updatedState.snackbarMessage).isEqualTo(R.string.error_failed_to_save_config)
            assertThat(updatedState.showValidationError).isFalse() // Should not be a validation error
            coVerify(exactly = 1) { mockNotifier.saveConfig(validConfig) }
            verify(exactly = 0) { navigator.pop(any()) }
        }
    }

    @Test
    fun `event SaveConfig - validation and save succeed - logs analytics, pops with Configured result`() = runTest {
        val validConfig = getSampleConfig(currentNotifierType)
        every { mockNotifier.validateConfig(validConfig) } returns ConfigValidationResult(true)
        coEvery { mockNotifier.saveConfig(validConfig) } returns Unit // Success

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.SaveConfig(validConfig))
            // No new state expected after pop

            coVerify { analytics.logNotifierConfigured(currentNotifierType) }
            coVerify { mockNotifier.saveConfig(validConfig) }
            val popSlot = slot<ConfigurationResult>()
            verify { navigator.pop(capture(popSlot)) }
            assertThat(popSlot.captured).isInstanceOf(ConfigurationResult.Configured::class.java)
            assertThat((popSlot.captured as ConfigurationResult.Configured).notifierType).isEqualTo(currentNotifierType)
        }
    }

    @Test
    fun `event TestConfig - EMAIL - quota invalid - shows snackbar`() = runTest {
        if (currentNotifierType != NotifierType.EMAIL) return@runTest // Test only for EMAIL

        val emailConfig = getSampleConfig(NotifierType.EMAIL) as AlertMediumConfig.EmailConfig
        every { mockNotifier.getConfig() } returns emailConfig
        every { mockNotifier.validateConfig(emailConfig) } returns ConfigValidationResult(true)
        coEvery { emailQuotaManager.validateQuota(emailConfig.emailAddresses.size) } returns EmailQuotaManager.QuotaValidationResult(false, 0, 5, R.string.error_email_quota_exceeded)

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.TestConfig)
            val updatedState = awaitItem()

            assertThat(updatedState.snackbarMessage).isEqualTo(R.string.error_email_quota_exceeded)
            coVerify(exactly = 0) { mockNotifier.sendNotification(any(), any()) }
        }
    }

    @Test
    fun `event TestConfig - EMAIL - quota valid, send success - shows success snackbar`() = runTest {
        if (currentNotifierType != NotifierType.EMAIL) return@runTest

        val emailConfig = getSampleConfig(NotifierType.EMAIL) as AlertMediumConfig.EmailConfig
        every { mockNotifier.getConfig() } returns emailConfig
        every { mockNotifier.validateConfig(emailConfig) } returns ConfigValidationResult(true)
        coEvery { emailQuotaManager.validateQuota(emailConfig.emailAddresses.size) } returns EmailQuotaManager.QuotaValidationResult(true)
        coEvery { mockNotifier.sendNotification(any(), any()) } returns true // Send success

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.TestConfig)
            val updatedState = awaitItem()

            assertThat(updatedState.snackbarMessage).isEqualTo(R.string.msg_test_notification_sent)
            coVerify { mockNotifier.sendNotification(emailConfig, any()) }
            coVerify { mockNotifier.saveConfig(emailConfig) } // Test saves config first
        }
    }

    @Test
    fun `event TestConfig - EMAIL - quota valid, send fails - shows failure snackbar`() = runTest {
        if (currentNotifierType != NotifierType.EMAIL) return@runTest

        val emailConfig = getSampleConfig(NotifierType.EMAIL) as AlertMediumConfig.EmailConfig
        every { mockNotifier.getConfig() } returns emailConfig
        every { mockNotifier.validateConfig(emailConfig) } returns ConfigValidationResult(true)
        coEvery { emailQuotaManager.validateQuota(emailConfig.emailAddresses.size) } returns EmailQuotaManager.QuotaValidationResult(true)
        coEvery { mockNotifier.sendNotification(any(), any()) } returns false // Send fails

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.TestConfig)
            val updatedState = awaitItem()

            assertThat(updatedState.snackbarMessage).isEqualTo(R.string.error_failed_to_send_test_notification)
        }
    }

    @Test
    fun `event TestConfig - Non-EMAIL - send success - shows success snackbar`() = runTest {
        if (currentNotifierType == NotifierType.EMAIL) return@runTest // Test for non-EMAIL types

        val config = getSampleConfig(currentNotifierType)
        every { mockNotifier.getConfig() } returns config
        every { mockNotifier.validateConfig(config) } returns ConfigValidationResult(true)
        coEvery { mockNotifier.sendNotification(any(), any()) } returns true // Send success

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.TestConfig)
            val updatedState = awaitItem()

            assertThat(updatedState.snackbarMessage).isEqualTo(R.string.msg_test_notification_sent)
            coVerify { mockNotifier.sendNotification(config, any()) }
            coVerify { mockNotifier.saveConfig(config) } // Test saves config first
        }
    }

    @Test
    fun `event TestConfig - Non-EMAIL - send fails - shows failure snackbar`() = runTest {
        if (currentNotifierType == NotifierType.EMAIL) return@runTest

        val config = getSampleConfig(currentNotifierType)
        every { mockNotifier.getConfig() } returns config
        every { mockNotifier.validateConfig(config) } returns ConfigValidationResult(true)
        coEvery { mockNotifier.sendNotification(any(), any()) } returns false // Send fails

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.TestConfig)
            val updatedState = awaitItem()

            assertThat(updatedState.snackbarMessage).isEqualTo(R.string.error_failed_to_send_test_notification)
        }
    }
    
    @Test
    fun `event TestConfig - any type - send throws exception - shows error snackbar`() = runTest {
        val config = getSampleConfig(currentNotifierType)
        every { mockNotifier.getConfig() } returns config
        every { mockNotifier.validateConfig(config) } returns ConfigValidationResult(true)
        if (currentNotifierType == NotifierType.EMAIL) {
             coEvery { emailQuotaManager.validateQuota(any()) } returns EmailQuotaManager.QuotaValidationResult(true)
        }
        coEvery { mockNotifier.sendNotification(any(), any()) } throws RuntimeException("Network error")

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(ConfigureNotificationMoldelScreenEvent.TestConfig)
            val updatedState = awaitItem()

            assertThat(updatedState.snackbarMessage).isEqualTo(R.string.error_failed_to_send_test_notification)
        }
    }


    @Test
    fun `event DismissSnackbar - clears snackbarMessage`() = runTest {
        // Initial setup to show a snackbar message
        val config = getSampleConfig(currentNotifierType)
        every { mockNotifier.getConfig() } returns config
        every { mockNotifier.validateConfig(config) } returns ConfigValidationResult(true)
        if (currentNotifierType == NotifierType.EMAIL) {
             coEvery { emailQuotaManager.validateQuota(any()) } returns EmailQuotaManager.QuotaValidationResult(true)
        }
        coEvery { mockNotifier.sendNotification(any(), any()) } returns false // Make it fail to show snackbar

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            var state = awaitItem()
            state.eventSink(ConfigureNotificationMediumScreenEvent.TestConfig) // Trigger snackbar
            state = awaitItem()
            assertThat(state.snackbarMessage).isNotNull()

            state.eventSink(ConfigureNotificationMediumScreenEvent.DismissSnackbar)
            state = awaitItem()
            assertThat(state.snackbarMessage).isNull()
        }
    }

    @Test
    fun `event NavigateBack - when configured - pops with Configured result`() = runTest {
        every { mockNotifier.hasValidConfig() } returns true // Initial state is configured

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            assertThat(state.isConfigured).isTrue() // Verify precondition

            state.eventSink(ConfigureNotificationMediumScreenEvent.NavigateBack)
            // No new state expected

            val popSlot = slot<ConfigurationResult>()
            verify { navigator.pop(capture(popSlot)) }
            assertThat(popSlot.captured).isInstanceOf(ConfigurationResult.Configured::class.java)
            assertThat((popSlot.captured as ConfigurationResult.Configured).notifierType).isEqualTo(currentNotifierType)
        }
    }

    @Test
    fun `event NavigateBack - when NOT configured - pops with NotConfigured result`() = runTest {
        every { mockNotifier.hasValidConfig() } returns false // Initial state is not configured

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            assertThat(state.isConfigured).isFalse() // Verify precondition

            state.eventSink(ConfigureNotificationMediumScreenEvent.NavigateBack)
            // No new state expected

            val popSlot = slot<ConfigurationResult>()
            verify { navigator.pop(capture(popSlot)) }
            assertThat(popSlot.captured).isEqualTo(ConfigurationResult.NotConfigured)
        }
    }
    // endregion Event Handling Tests
}
