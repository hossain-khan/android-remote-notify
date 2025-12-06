package dev.hossain.remotenotify.ui.alertmediumconfig

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.EmailQuotaManager
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.notifier.of
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfigureNotificationMediumPresenterTest {
    // System under test
    private lateinit var presenter: ConfigureNotificationMediumPresenter

    // Mock dependencies
    private lateinit var mockNavigator: FakeNavigator
    private val mockNotificationSender = mockk<NotificationSender>()
    private val mockNotifiers = setOf(mockNotificationSender)
    private val mockEmailQuotaManager = mockk<EmailQuotaManager>()
    private val mockAlertFormatter = mockk<AlertFormatter>(relaxed = true)
    private val mockAnalytics = mockk<Analytics>(relaxed = true)

    @Before
    fun setup() {
        // Mock the extension function 'of'
        mockkStatic("dev.hossain.remotenotify.notifier.NotifierUtilsKt")
        every { mockNotifiers.of(any()) } returns mockNotificationSender

        // Setup default mock behaviors
        every { mockNotificationSender.notifierType } returns NotifierType.EMAIL
        every { mockNotificationSender.hasValidConfig() } returns false
        every { mockNotificationSender.getConfig() } returns AlertMediumConfig.Email(email = "")
        every { mockNotificationSender.validateConfig(any()) } returns ConfigValidationResult(false, emptyMap())
    }

    @After
    fun tearDown() {
        unmockkStatic("dev.hossain.remotenotify.notifier.NotifierUtilsKt")
    }

    @Test
    fun `when presenter is initialized then state reflects notifier configuration`() =
        runTest {
            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val state = awaitItem()

                assertThat(state.notifierType).isEqualTo(NotifierType.EMAIL)
                assertThat(state.isConfigured).isFalse()
                assertThat(state.configValidationResult.isValid).isFalse()
            }
        }

    @Test
    fun `when notifier is configured then isConfigured is true`() =
        runTest {
            every { mockNotificationSender.hasValidConfig() } returns true
            every { mockNotificationSender.getConfig() } returns AlertMediumConfig.Email(email = "test@example.com")

            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val state = awaitItem()

                assertThat(state.isConfigured).isTrue()
                assertThat(state.alertMediumConfig).isInstanceOf(AlertMediumConfig.Email::class.java)
            }
        }

    @Test
    fun `when UpdateConfigValue event is triggered then state reflects new config`() =
        runTest {
            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            val newConfig = AlertMediumConfig.Email(email = "new@example.com")
            every { mockNotificationSender.validateConfig(newConfig) } returns ConfigValidationResult(true, emptyMap())

            presenter.test {
                val initialState = awaitItem()
                initialState.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(newConfig))

                val updatedState = awaitItem()
                assertThat(updatedState.alertMediumConfig).isEqualTo(newConfig)
                assertThat(updatedState.showValidationError).isFalse()
            }
        }

    @Test
    fun `when SaveConfig event is triggered with valid config then saves and navigates back`() =
        runTest {
            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            val validConfig = AlertMediumConfig.Email(email = "valid@example.com")
            every { mockNotificationSender.validateConfig(validConfig) } returns ConfigValidationResult(true, emptyMap())
            coEvery { mockNotificationSender.saveConfig(validConfig) } returns Unit

            presenter.test {
                val initialState = awaitItem()
                initialState.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(validConfig))
                val updatedState = awaitItem()

                updatedState.eventSink(ConfigureNotificationMediumScreen.Event.SaveConfig)

                coVerify { mockNotificationSender.saveConfig(validConfig) }
                val result = mockNavigator.awaitResetRoot() as ConfigureNotificationMediumScreen.ConfigurationResult
                assertThat(result).isInstanceOf(ConfigureNotificationMediumScreen.ConfigurationResult.Configured::class.java)
            }
        }

    @Test
    fun `when SaveConfig event is triggered with invalid config then does not save`() =
        runTest {
            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            val invalidConfig = AlertMediumConfig.Email(email = "invalid")
            every { mockNotificationSender.validateConfig(invalidConfig) } returns
                ConfigValidationResult(false, mapOf("email" to "Invalid email"))

            presenter.test {
                val initialState = awaitItem()
                initialState.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(invalidConfig))
                val updatedState = awaitItem()

                updatedState.eventSink(ConfigureNotificationMediumScreen.Event.SaveConfig)

                coVerify(exactly = 0) { mockNotificationSender.saveConfig(any()) }
                // Should show validation error
                val errorState = awaitItem()
                assertThat(errorState.showValidationError).isTrue()
            }
        }

    @Test
    fun `when TestConfig event is triggered then sends test notification`() =
        runTest {
            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            val config = AlertMediumConfig.Email(email = "test@example.com")
            coEvery { mockNotificationSender.saveConfig(config) } returns Unit
            coEvery { mockNotificationSender.sendNotification(any()) } returns true
            coEvery { mockEmailQuotaManager.validateQuota() } returns ConfigValidationResult(true, emptyMap())

            presenter.test {
                val initialState = awaitItem()
                initialState.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(config))
                val updatedState = awaitItem()

                updatedState.eventSink(ConfigureNotificationMediumScreen.Event.TestConfig)

                coVerify { mockNotificationSender.sendNotification(any()) }
                val stateWithMessage = awaitItem()
                assertThat(stateWithMessage.snackbarMessage).contains("successfully")
            }
        }

    @Test
    fun `when TestConfig event fails then shows error message`() =
        runTest {
            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            val config = AlertMediumConfig.Email(email = "test@example.com")
            coEvery { mockNotificationSender.saveConfig(config) } returns Unit
            coEvery { mockNotificationSender.sendNotification(any()) } returns false
            coEvery { mockEmailQuotaManager.validateQuota() } returns ConfigValidationResult(true, emptyMap())

            presenter.test {
                val initialState = awaitItem()
                initialState.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(config))
                val updatedState = awaitItem()

                updatedState.eventSink(ConfigureNotificationMediumScreen.Event.TestConfig)

                val stateWithMessage = awaitItem()
                assertThat(stateWithMessage.snackbarMessage).contains("Failed")
            }
        }

    @Test
    fun `when DismissSnackbar event is triggered then snackbar message is cleared`() =
        runTest {
            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            val config = AlertMediumConfig.Email(email = "test@example.com")
            coEvery { mockNotificationSender.saveConfig(config) } returns Unit
            coEvery { mockNotificationSender.sendNotification(any()) } returns true
            coEvery { mockEmailQuotaManager.validateQuota() } returns ConfigValidationResult(true, emptyMap())

            presenter.test {
                val initialState = awaitItem()
                initialState.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(config))
                val updatedState = awaitItem()

                // Trigger test to show snackbar
                updatedState.eventSink(ConfigureNotificationMediumScreen.Event.TestConfig)
                val stateWithMessage = awaitItem()
                assertThat(stateWithMessage.snackbarMessage).isNotNull()

                // Dismiss snackbar
                stateWithMessage.eventSink(ConfigureNotificationMediumScreen.Event.DismissSnackbar)
                val clearedState = awaitItem()
                assertThat(clearedState.snackbarMessage).isNull()
            }
        }

    @Test
    fun `when NavigateBack event is triggered with configured notifier then pops with Configured result`() =
        runTest {
            every { mockNotificationSender.hasValidConfig() } returns true

            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val state = awaitItem()
                state.eventSink(ConfigureNotificationMediumScreen.Event.NavigateBack)

                val result = mockNavigator.awaitResetRoot() as ConfigureNotificationMediumScreen.ConfigurationResult
                assertThat(result).isInstanceOf(ConfigureNotificationMediumScreen.ConfigurationResult.Configured::class.java)
            }
        }

    @Test
    fun `when NavigateBack event is triggered with unconfigured notifier then pops with NotConfigured result`() =
        runTest {
            every { mockNotificationSender.hasValidConfig() } returns false

            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val state = awaitItem()
                state.eventSink(ConfigureNotificationMediumScreen.Event.NavigateBack)

                val result = mockNavigator.awaitResetRoot() as ConfigureNotificationMediumScreen.ConfigurationResult
                assertThat(result).isEqualTo(ConfigureNotificationMediumScreen.ConfigurationResult.NotConfigured)
            }
        }

    @Test
    fun `when email quota is exceeded then test shows quota error`() =
        runTest {
            val screen = ConfigureNotificationMediumScreen(NotifierType.EMAIL)
            mockNavigator = FakeNavigator(screen)
            presenter =
                ConfigureNotificationMediumPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    notifiers = mockNotifiers,
                    emailQuotaManager = mockEmailQuotaManager,
                    alertFormatter = mockAlertFormatter,
                    analytics = mockAnalytics,
                )

            val config = AlertMediumConfig.Email(email = "test@example.com")
            coEvery { mockNotificationSender.saveConfig(config) } returns Unit
            coEvery { mockEmailQuotaManager.validateQuota() } returns
                ConfigValidationResult(
                    false,
                    mapOf(EmailQuotaManager.ValidationKeys.EMAIL_DAILY_QUOTA to "Daily quota exceeded"),
                )

            presenter.test {
                val initialState = awaitItem()
                initialState.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(config))
                val updatedState = awaitItem()

                updatedState.eventSink(ConfigureNotificationMediumScreen.Event.TestConfig)

                val stateWithMessage = awaitItem()
                assertThat(stateWithMessage.snackbarMessage).contains("quota")
                coVerify(exactly = 0) { mockNotificationSender.sendNotification(any()) }
            }
        }
}
