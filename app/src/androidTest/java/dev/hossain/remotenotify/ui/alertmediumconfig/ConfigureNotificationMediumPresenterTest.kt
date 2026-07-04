package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.slack.circuit.runtime.Navigator
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.EmailQuotaManager
import dev.hossain.remotenotify.data.EmailQuotaManager.Companion.ValidationKeys.EMAIL_DAILY_QUOTA
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ConfigureNotificationMediumPresenterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @MockK
    lateinit var navigator: Navigator

    @MockK
    lateinit var notificationSender: NotificationSender

    @MockK
    lateinit var emailQuotaManager: EmailQuotaManager

    @MockK
    lateinit var analytics: Analytics

    private val alertFormatter = AlertFormatter()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { navigator.pop(any()) } returns null
        every { navigator.goTo(any()) } returns true

        every { notificationSender.notifierType } returns NotifierType.WEBHOOK_REST_API
        coEvery { notificationSender.hasValidConfig() } returns false
        coEvery { notificationSender.getConfig() } returns AlertMediumConfig.WebhookConfig("https://example.com")
        coEvery { notificationSender.validateConfig(any()) } returns ConfigValidationResult(true)
        coEvery { notificationSender.saveConfig(any()) } just runs
        coEvery { notificationSender.sendNotification(any()) } returns true

        coEvery { emailQuotaManager.validateQuota() } returns ConfigValidationResult(true)

        coEvery { analytics.logScreenView(any()) } just runs
        coEvery { analytics.logNotifierConfigured(any()) } just runs
    }

    @Test
    fun saveConfigDoesNotPersistOrNavigateWhenValidationFails() {
        every { notificationSender.notifierType } returns NotifierType.TELEGRAM
        coEvery { notificationSender.getConfig() } returns AlertMediumConfig.TelegramConfig("bot-token", "chat-id")
        coEvery { notificationSender.validateConfig(any()) } returns
            ConfigValidationResult(
                isValid = false,
                errors = mapOf("chatId" to "Invalid chat id"),
            )

        val state = renderPresenter(NotifierType.TELEGRAM)

        composeRule.runOnIdle {
            state.eventSink(
                ConfigureNotificationMediumScreen.Event.UpdateConfigValue(
                    AlertMediumConfig.TelegramConfig("bot-token", "chat-id"),
                ),
            )
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.SaveConfig)
        }
        composeRule.waitForIdle()

        assertTrue(currentState().showValidationError)
        assertFalse(currentState().configValidationResult.isValid)
        coVerify(exactly = 0) { notificationSender.saveConfig(any()) }
        coVerify(exactly = 0) { analytics.logNotifierConfigured(any()) }
        io.mockk.verify(exactly = 0) { navigator.pop(any()) }
    }

    @Test
    fun saveConfigPersistsAndPopsConfiguredResultOnSuccess() {
        val config = AlertMediumConfig.WebhookConfig("https://example.com")
        val state = renderPresenter(NotifierType.WEBHOOK_REST_API)

        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(config))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.SaveConfig)
        }
        composeRule.waitForIdle()

        coVerify { notificationSender.saveConfig(config) }
        coVerify { analytics.logNotifierConfigured(NotifierType.WEBHOOK_REST_API) }
        io.mockk.verify {
            navigator.pop(
                ConfigureNotificationMediumScreen.ConfigurationResult.Configured(
                    NotifierType.WEBHOOK_REST_API,
                ),
            )
        }
    }

    @Test
    fun saveConfigFailureShowsSnackbarMessage() {
        val config = AlertMediumConfig.WebhookConfig("https://example.com")
        coEvery { notificationSender.saveConfig(config) } throws IllegalStateException("broken save")

        val state = renderPresenter(NotifierType.WEBHOOK_REST_API)

        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(config))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.SaveConfig)
        }
        composeRule.waitForIdle()

        assertTrue(requireNotNull(currentState().snackbarMessage).contains("broken save"))
        io.mockk.verify(exactly = 0) { navigator.pop(any()) }
    }

    @Test
    fun testConfigForEmailSurfacesQuotaErrorAndDoesNotSendNotification() {
        every { notificationSender.notifierType } returns NotifierType.EMAIL
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "api-key",
                domain = "mg.example.com",
                fromEmail = "alerts@example.com",
                toEmail = "user@example.com",
            )
        coEvery { notificationSender.getConfig() } returns config
        coEvery { emailQuotaManager.validateQuota() } returns
            ConfigValidationResult(
                isValid = false,
                errors = mapOf(EMAIL_DAILY_QUOTA to "Daily quota exceeded"),
            )

        val state = renderPresenter(NotifierType.EMAIL)

        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(config))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.TestConfig)
        }
        composeRule.waitForIdle()

        assertEquals("Daily quota exceeded", currentState().snackbarMessage)
        coVerify { notificationSender.saveConfig(config) }
        coVerify(exactly = 0) { notificationSender.sendNotification(any()) }
    }

    @Test
    fun testConfigAlternatesBatteryThenStorageAlerts() {
        val capturedAlerts = mutableListOf<RemoteAlert>()
        coEvery { notificationSender.sendNotification(any()) } answers {
            capturedAlerts += firstArg<RemoteAlert>()
            true
        }

        val state = renderPresenter(NotifierType.WEBHOOK_REST_API)

        composeRule.runOnIdle {
            state.eventSink(
                ConfigureNotificationMediumScreen.Event.UpdateConfigValue(
                    AlertMediumConfig.WebhookConfig("https://example.com"),
                ),
            )
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.TestConfig)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            state.eventSink(ConfigureNotificationMediumScreen.Event.TestConfig)
        }
        composeRule.waitForIdle()

        assertEquals(2, capturedAlerts.size)
        assertTrue(capturedAlerts[0] is RemoteAlert.BatteryAlert)
        assertTrue(capturedAlerts[1] is RemoteAlert.StorageAlert)
    }

    private var latestState by mutableStateOf<ConfigureNotificationMediumScreen.State?>(null)

    private fun renderPresenter(notifierType: NotifierType): ConfigureNotificationMediumScreen.State {
        val presenter =
            ConfigureNotificationMediumPresenter(
                screen = ConfigureNotificationMediumScreen(notifierType),
                navigator = navigator,
                notifiers = setOf(notificationSender),
                emailQuotaManager = emailQuotaManager,
                alertFormatter = alertFormatter,
                analytics = analytics,
            )

        composeRule.setContent {
            latestState = presenter.present()
        }
        composeRule.waitForIdle()
        return currentState()
    }

    private fun currentState(): ConfigureNotificationMediumScreen.State = requireNotNull(latestState)
}
