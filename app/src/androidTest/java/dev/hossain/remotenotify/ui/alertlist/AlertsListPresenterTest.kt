package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.slack.circuit.runtime.Navigator
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.ui.about.AboutAppScreen
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
import dev.hossain.remotenotify.ui.alertchecklog.AlertCheckLogViewerScreen
import dev.hossain.remotenotify.ui.alertmediumlist.NotificationMediumListScreen
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AlertsListPresenterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @MockK
    lateinit var navigator: Navigator

    @MockK
    lateinit var remoteAlertRepository: RemoteAlertRepository

    @MockK
    lateinit var batteryMonitor: BatteryMonitor

    @MockK
    lateinit var storageMonitor: StorageMonitor

    @MockK
    lateinit var notifier: NotificationSender

    @MockK
    lateinit var appPreferencesDataStore: AppPreferencesDataStore

    @MockK
    lateinit var analytics: Analytics

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { navigator.goTo(any()) } returns true
        every { navigator.pop(any()) } returns null

        every { batteryMonitor.getBatteryLevel() } returns 81
        every { storageMonitor.getAvailableStorageInGB() } returns 24L
        every { storageMonitor.getTotalStorageInGB() } returns 128L
        every { notifier.notifierType } returns NotifierType.TELEGRAM
        coEvery { notifier.hasValidConfig() } returns true

        every { remoteAlertRepository.getAllRemoteAlertFlow() } returns flowOf(emptyList())
        every { remoteAlertRepository.getLatestCheckLog() } returns flowOf(null)
        coEvery { remoteAlertRepository.deleteRemoteAlert(any()) } just runs

        coEvery { analytics.logScreenView(any()) } just runs
        coEvery { analytics.logViewTutorial(any()) } just runs
        coEvery { appPreferencesDataStore.markEducationDialogShown() } just runs
    }

    @Test
    fun deleteNotificationCallsRepositoryDelete() {
        val alert = RemoteAlert.BatteryAlert(alertId = 2L, batteryPercentage = 20)
        val state = renderPresenter()

        composeRule.runOnIdle {
            state.eventSink(AlertsListScreen.Event.DeleteNotification(alert))
        }
        composeRule.waitForIdle()

        coVerify { remoteAlertRepository.deleteRemoteAlert(alert) }
    }

    @Test
    fun navigationEventsRouteToExpectedScreens() {
        val alert = RemoteAlert.StorageAlert(alertId = 9L, storageMinSpaceGb = 5)
        val state = renderPresenter()

        composeRule.runOnIdle {
            state.eventSink(AlertsListScreen.Event.EditRemoteAlert(alert))
            state.eventSink(AlertsListScreen.Event.AddNotification)
            state.eventSink(AlertsListScreen.Event.AddNotificationDestination)
            state.eventSink(AlertsListScreen.Event.NavigateToAbout)
            state.eventSink(AlertsListScreen.Event.ViewAllLogs)
        }
        composeRule.waitForIdle()

        io.mockk.verify { navigator.goTo(AddNewRemoteAlertScreen(alertId = 9L)) }
        io.mockk.verify { navigator.goTo(AddNewRemoteAlertScreen()) }
        io.mockk.verify { navigator.goTo(NotificationMediumListScreen) }
        io.mockk.verify { navigator.goTo(AboutAppScreen) }
        io.mockk.verify { navigator.goTo(AlertCheckLogViewerScreen) }
    }

    @Test
    fun educationSheetShowAndDismissUpdateStateAnalyticsAndPreferences() {
        val state = renderPresenter()

        composeRule.runOnIdle {
            state.eventSink(AlertsListScreen.Event.ShowEducationSheet)
        }
        composeRule.waitForIdle()
        assertTrue(currentState().showEducationSheet)

        composeRule.runOnIdle {
            currentState().eventSink(AlertsListScreen.Event.DismissEducationSheet)
        }
        composeRule.waitForIdle()

        assertFalse(currentState().showEducationSheet)
        coVerify { analytics.logViewTutorial(false) }
        coVerify { analytics.logViewTutorial(true) }
        coVerify(exactly = 2) { appPreferencesDataStore.markEducationDialogShown() }
    }

    @Test
    fun isAnyNotifierConfiguredReflectsSenderState() {
        renderPresenter()

        assertTrue(currentState().isAnyNotifierConfigured)
        assertEquals(81, currentState().batteryPercentage)
        assertEquals(24L, currentState().availableStorage)
        assertEquals(128L, currentState().totalStorage)
    }

    private var latestState by mutableStateOf<AlertsListScreen.State?>(null)

    private fun renderPresenter(): AlertsListScreen.State {
        val presenter =
            AlertsListPresenter(
                navigator = navigator,
                remoteAlertRepository = remoteAlertRepository,
                batteryMonitor = batteryMonitor,
                storageMonitor = storageMonitor,
                notifiers = setOf(notifier),
                appPreferencesDataStore = appPreferencesDataStore,
                analytics = analytics,
            )

        composeRule.setContent {
            latestState = presenter.present()
        }
        composeRule.waitForIdle()
        return currentState()
    }

    private fun currentState(): AlertsListScreen.State = requireNotNull(latestState)
}
