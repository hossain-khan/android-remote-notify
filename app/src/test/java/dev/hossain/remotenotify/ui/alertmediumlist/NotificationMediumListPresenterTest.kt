package dev.hossain.remotenotify.ui.alertmediumlist

import android.content.Context
import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.TestNavGraphs
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.NotificationSender
import dev.hossain.remotenotify.model.NotifierType
import dev.hossain.remotenotify.service.PeriodicWorkRequestManager
import dev.hossain.remotenotify.ui.NavGraphs
import dev.hossain.remotenotify.ui.alertmediumconfig.ConfigureNotificationMediumScreen
import dev.hossain.remotenotify.ui.alertmediumconfig.ConfigurationResult
import dev.hossain.remotenotify.ui.alertmediumconfig.rememberAnsweringNavigator
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests for [NotificationMediumListPresenter].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationMediumListPresenterTest {

    private lateinit var navigator: DestinationsNavigator
    private lateinit var appPreferencesDataStore: AppPreferencesDataStore
    private lateinit var mockNotifiers: Set<@JvmSuppressWildcards NotificationSender>
    private lateinit var analytics: Analytics
    private lateinit var mockContext: Context

    // Individual mock senders for easier verification
    private lateinit var emailNotifier: NotificationSender
    private lateinit var webhookNotifier: NotificationSender
    private lateinit var telegramNotifier: NotificationSender

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        navigator = mockk(relaxed = true)
        appPreferencesDataStore = mockk(relaxed = true) // relaxed for saveWorkerInterval
        analytics = mockk(relaxed = true)
        mockContext = mockk(relaxed = true)

        // Mock individual notifiers
        emailNotifier = mockk(name = "EmailNotifier", relaxed = true) {
            every { type } returns NotifierType.EMAIL
            every { displayName } returns R.string.notifier_email_name // Assuming these are actual string res IDs
            every { icon } returns R.drawable.ic_email // Assuming these are actual drawable res IDs
        }
        webhookNotifier = mockk(name = "WebhookNotifier", relaxed = true) {
            every { type } returns NotifierType.WEBHOOK_REST_API
            every { displayName } returns R.string.notifier_webhook_name
            every { icon } returns R.drawable.ic_webhook
        }
        telegramNotifier = mockk(name = "TelegramNotifier", relaxed = true) {
            every { type } returns NotifierType.TELEGRAM
            every { displayName } returns R.string.notifier_telegram_name
            every { icon } returns R.drawable.ic_telegram
        }
        mockNotifiers = setOf(emailNotifier, webhookNotifier, telegramNotifier)

        // Default mock for worker interval
        coEvery { appPreferencesDataStore.workerIntervalFlow() } returns flowOf(60L)

        // Mock static NavGraphs
        mockkStatic(NavGraphs::class)
        every { NavGraphs.root } returns TestNavGraphs.Root

        // Mock static rememberAnsweringNavigator - this is tricky.
        // We'll mock the extension function itself if possible, or test its effects via the navigator it uses.
        // For now, we assume `rememberAnsweringNavigator` correctly calls `navigator.navigate`
        // and we will verify the `navigator.navigate` call.
        // The callback part will be tested by simulating the pop result.

        // Mock static PeriodicWorkRequestManager.sendPeriodicWorkRequest
        mockkStatic(PeriodicWorkRequestManager::class)
        every { PeriodicWorkRequestManager.sendPeriodicWorkRequest(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createPresenter(
        mockedNavigator: DestinationsNavigator = navigator,
        // This allows us to inject a navigator that we can control for callback testing
        answeringNavigatorProvider: @Composable (callback: (ConfigurationResult) -> Unit) -> DestinationsNavigator = { callback ->
            rememberAnsweringNavigator(mockedNavigator, callback)
        }
    ): NotificationMediumListPresenter {
        return NotificationMediumListPresenter(
            navigator = mockedNavigator,
            appPreferences = appPreferencesDataStore,
            notifiers = mockNotifiers,
            analytics = analytics,
            appContext = mockContext,
            // Provide the answeringNavigatorProvider to the presenter
            configureMediumNavigatorProvider = answeringNavigatorProvider
        )
    }

    @Test
    fun `initial state - loads worker interval and notifier configs correctly`() = runTest(testDispatcher) {
        val initialInterval = 30L
        coEvery { appPreferencesDataStore.workerIntervalFlow() } returns flowOf(initialInterval)

        // Configure Email Notifier
        val emailConfig = mockk<AlertMediumConfig.EmailConfig>()
        every { emailConfig.configPreviewText(mockContext) } returns "test@example.com"
        every { emailNotifier.hasValidConfig() } returns true
        every { emailNotifier.getConfig() } returns emailConfig

        // Webhook Notifier - Not configured
        every { webhookNotifier.hasValidConfig() } returns false
        every { webhookNotifier.getConfig() } returns null

        // Telegram Notifier - Configured
        val telegramConfig = mockk<AlertMediumConfig.TelegramConfig>()
        every { telegramConfig.configPreviewText(mockContext) } returns "@channel_id"
        every { telegramNotifier.hasValidConfig() } returns true
        every { telegramNotifier.getConfig() } returns telegramConfig


        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()

            assertThat(state.workerIntervalMinutes).isEqualTo(initialInterval)
            assertThat(state.notifiers).hasSize(3)

            // Order is by displayName, assume Email, Telegram, Webhook for this example
            // This part is tricky as it depends on actual string resource values and sorting.
            // For robustness, find by type or verify properties individually.
            val emailState = state.notifiers.find { it.type == NotifierType.EMAIL }!!
            assertThat(emailState.isConfigured).isTrue()
            assertThat(emailState.configPreviewText).isEqualTo("test@example.com")

            val webhookState = state.notifiers.find { it.type == NotifierType.WEBHOOK_REST_API }!!
            assertThat(webhookState.isConfigured).isFalse()
            assertThat(webhookState.configPreviewText).isNull()

            val telegramState = state.notifiers.find { it.type == NotifierType.TELEGRAM }!!
            assertThat(telegramState.isConfigured).isTrue()
            assertThat(telegramState.configPreviewText).isEqualTo("@channel_id")

            coVerify { analytics.logScreenView(NotificationMediumListScreen::class) } // Impression
        }
    }

    @Test
    fun `event EditMediumConfig - navigates to ConfigureNotificationMediumScreen`() = runTest(testDispatcher) {
        // We need to capture the navigator that rememberAnsweringNavigator uses internally
        // or ensure our mock `navigator` is the one being called.
        val capturedNavigator = mockk<DestinationsNavigator>(relaxed = true)
        val presenter = createPresenter(mockedNavigator = capturedNavigator)


        moleculeFlow(RecompositionClock.Immediate) {
            presenter.present()
        }.test {
            val state = awaitItem()
            val targetNotifierType = NotifierType.EMAIL
            state.eventSink(NotificationMediumListScreenEvent.EditMediumConfig(targetNotifierType))

            val slot = slot<Any>() // Use Any for the destination type
            verify { capturedNavigator.navigate(capture(slot), any(), any(), any()) }

            // Check if the captured destination matches ConfigureNotificationMediumScreen(targetNotifierType)
            // This requires ConfigureNotificationMediumScreen to be a data class or have a meaningful equals/toString
            val capturedDestination = slot.captured
            assertThat(capturedDestination).isInstanceOf(ConfigureNotificationMediumScreen::class.java)
            assertThat((capturedDestination as ConfigureNotificationMediumScreen).notifierType).isEqualTo(targetNotifierType)
        }
    }

    @Test
    fun `event ResetMediumConfig - clears config, updates state`() = runTest(testDispatcher) {
        // Email notifier initially configured
        val emailConfig = mockk<AlertMediumConfig.EmailConfig>()
        every { emailConfig.configPreviewText(mockContext) } returns "initial@config.com"
        every { emailNotifier.hasValidConfig() } returns true
        every { emailNotifier.getConfig() } returns emailConfig
        coEvery { emailNotifier.clearConfig() } returns Unit // Mock clearConfig

        // Other notifiers (e.g., webhook) not configured for simplicity in this test
        every { webhookNotifier.hasValidConfig() } returns false
        every { telegramNotifier.hasValidConfig() } returns false


        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            var state = awaitItem()
            val emailStateBeforeReset = state.notifiers.find { it.type == NotifierType.EMAIL }!!
            assertThat(emailStateBeforeReset.isConfigured).isTrue()

            state.eventSink(NotificationMediumListScreenEvent.ResetMediumConfig(NotifierType.EMAIL))
            state = awaitItem() // Recomposition

            coVerify { emailNotifier.clearConfig() }
            val emailStateAfterReset = state.notifiers.find { it.type == NotifierType.EMAIL }!!
            assertThat(emailStateAfterReset.isConfigured).isFalse()
            assertThat(emailStateAfterReset.configPreviewText).isNull()
        }
    }

    @Test
    fun `event OnWorkerIntervalUpdated - debounces and calls save and sendPeriodicWorkRequest`() = runTest(testDispatcher) {
        val initialInterval = 60L
        val newInterval = 30L
        coEvery { appPreferencesDataStore.workerIntervalFlow() } returns flowOf(initialInterval)

        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()

            // Update interval but don't advance time yet
            state.eventSink(NotificationMediumListScreenEvent.OnWorkerIntervalUpdated(newInterval))
            coVerify(exactly = 0) { appPreferencesDataStore.saveWorkerInterval(any()) }
            verify(exactly = 0) { PeriodicWorkRequestManager.sendPeriodicWorkRequest(any(), any()) }

            // Advance time by more than debounce duration (1000ms)
            advanceTimeBy(1001)

            coVerify { appPreferencesDataStore.saveWorkerInterval(newInterval) }
            verify { PeriodicWorkRequestManager.sendPeriodicWorkRequest(mockContext, newInterval) }
        }
    }

    @Test
    fun `event ShareFeedback - logs analytics`() = runTest(testDispatcher) {
        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(NotificationMediumListScreenEvent.ShareFeedback)
            coVerify { analytics.logSendFeedback() }
        }
    }

    @Test
    fun `event NavigateBack - calls navigator pop`() = runTest(testDispatcher) {
        moleculeFlow(RecompositionClock.Immediate) {
            createPresenter().present()
        }.test {
            val state = awaitItem()
            state.eventSink(NotificationMediumListScreenEvent.NavigateBack)
            verify { navigator.pop() }
        }
    }

    @Test
    fun `rememberAnsweringNavigator callback - Configured result - calls sendPeriodicWorkRequest`() = runTest(testDispatcher) {
        val currentInterval = 45L
        coEvery { appPreferencesDataStore.workerIntervalFlow() } returns flowOf(currentInterval)

        // This flow will simulate the callback from ConfigureNotificationMediumScreen
        val mockConfigurationResultFlow = MutableSharedFlow<ConfigurationResult>(replay = 1)

        // Custom provider for rememberAnsweringNavigator
        val testAnsweringNavigatorProvider: @Composable (callback: (ConfigurationResult) -> Unit) -> DestinationsNavigator = { callback ->
            // This mock navigator will be used by EditMediumConfig
            val mockDestinationsNavigator = mockk<DestinationsNavigator>(relaxed = true)
            // Simulate the callback being invoked when ConfigureNotificationMediumScreen would pop
            // This requires a way to trigger `callback(result)` when `mockDestinationsNavigator.popBackStack(result)` would be called.
            // A simpler way is to directly invoke the callback for testing purposes.
            // We'll use the flow to emit the result as if it came from the popped screen.
            mockConfigurationResultFlow.tryEmit(ConfigurationResult.Configured(NotifierType.EMAIL)) // Emit after presenter starts collecting
            mockDestinationsNavigator // return a navigator, though its navigate call might be what we verify for EditMediumConfig
        }


        val presenter = NotificationMediumListPresenter(
            navigator = navigator, // Main navigator
            appPreferences = appPreferencesDataStore,
            notifiers = mockNotifiers,
            analytics = analytics,
            appContext = mockContext,
            configureMediumNavigatorProvider = { callback ->
                // This is the navigator that will be used by EditMediumConfig
                // When EditMediumConfig calls goTo on this, we want to eventually trigger the callback
                // For this test, we will manually trigger the callback after the presenter is set up.
                val internalNavigator = mockk<DestinationsNavigator>(relaxed = true)
                // Simulate the callback being invoked
                // This is the core of testing rememberAnsweringNavigator's callback behavior
                // We need to ensure this callback is the one passed to the presenter.
                callback(ConfigurationResult.Configured(NotifierType.EMAIL)) // Directly invoke for test
                internalNavigator
            }
        )

        moleculeFlow(RecompositionClock.Immediate) {
            presenter.present()
        }.test {
            awaitItem() // Consume initial state

            // The callback should have been invoked during the composition of the presenter due to the direct call.
            // Verify sendPeriodicWorkRequest was called with the current interval
            verify(timeout(100)) { // Add timeout for coroutine execution
                PeriodicWorkRequestManager.sendPeriodicWorkRequest(mockContext, currentInterval)
            }
        }
    }


    @Test
    fun `rememberAnsweringNavigator callback - NotConfigured result - no additional actions`() = runTest(testDispatcher) {
        val currentInterval = 45L
        coEvery { appPreferencesDataStore.workerIntervalFlow() } returns flowOf(currentInterval)

        val presenter = NotificationMediumListPresenter(
            navigator = navigator,
            appPreferences = appPreferencesDataStore,
            notifiers = mockNotifiers,
            analytics = analytics,
            appContext = mockContext,
            configureMediumNavigatorProvider = { callback ->
                val internalNavigator = mockk<DestinationsNavigator>(relaxed = true)
                callback(ConfigurationResult.NotConfigured) // Simulate NotConfigured result
                internalNavigator
            }
        )

        moleculeFlow(RecompositionClock.Immediate) {
            presenter.present()
        }.test {
            awaitItem() // Consume initial state

            // Verify sendPeriodicWorkRequest was NOT called again (it's called once on init if worker is enabled, but not due to this callback)
            // To be precise, ensure no *additional* calls due to this specific callback.
            // Since the setup might involve an initial call, we can't do `exactly = 0` without more complex setup.
            // Instead, we rely on the fact that the Configured case *does* verify a call.
            // A more robust way would be to count calls before and after, or use a spy on WorkManager with more detailed verification.
            // For now, this implicitly tests that no *new* work request is scheduled due to NotConfigured.
            // Let's ensure it's not called with a specific marker if possible, or simply that it's not called *again* if it was already.
            // The initial setup does not call sendPeriodicWorkRequest, only the callback for Configured does.
            verify(exactly = 0) { PeriodicWorkRequestManager.sendPeriodicWorkRequest(mockContext, currentInterval) }
        }
    }
}
