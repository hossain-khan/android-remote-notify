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
        coEvery { mockNotificationSender.hasValidConfig() } returns false
        coEvery { mockNotificationSender.getConfig() } returns
            AlertMediumConfig.EmailConfig(apiKey = "", domain = "", fromEmail = "", toEmail = "")
        coEvery { mockNotificationSender.validateConfig(any()) } returns ConfigValidationResult(false, emptyMap())
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
            coEvery { mockNotificationSender.hasValidConfig() } returns true
            coEvery { mockNotificationSender.getConfig() } returns
                AlertMediumConfig.EmailConfig(
                    apiKey = "test",
                    domain = "example.com",
                    fromEmail = "from@example.com",
                    toEmail = "test@example.com",
                )

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
                assertThat(state.alertMediumConfig).isInstanceOf(AlertMediumConfig.EmailConfig::class.java)
            }
        }
}
