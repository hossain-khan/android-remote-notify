package dev.hossain.remotenotify.ui.addalert

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.slack.circuit.runtime.Navigator
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.EmailConfigDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.AlertMode
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.StorageMonitor
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddNewRemoteAlertPresenterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @MockK
    lateinit var navigator: Navigator

    @MockK
    lateinit var remoteAlertRepository: RemoteAlertRepository

    @MockK
    lateinit var storageMonitor: StorageMonitor

    @MockK
    lateinit var appPreferencesDataStore: AppPreferencesDataStore

    @MockK
    lateinit var emailConfigDataStore: EmailConfigDataStore

    @MockK
    lateinit var analytics: Analytics

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { navigator.pop(any()) } returns null
        every { navigator.goTo(any()) } returns true

        every { storageMonitor.getAvailableStorageInGB() } returns 12L
        coEvery { emailConfigDataStore.hasValidConfig() } returns false
        every { appPreferencesDataStore.hideBatteryOptReminder } returns flowOf(false)

        coEvery { analytics.logScreenView(any()) } just runs
        coEvery { analytics.logAlertAdded(any()) } just runs
        coEvery { analytics.logAlertEdited(any()) } just runs
        coEvery { analytics.logOptimizeBatteryIgnore() } just runs

        coEvery { remoteAlertRepository.saveRemoteAlert(any()) } just runs
        coEvery { remoteAlertRepository.updateRemoteAlert(any()) } returns 1
    }

    @Test
    fun editModeLoadsExistingAlertIntoPresenterState() {
        coEvery { remoteAlertRepository.getRemoteAlertById(7L) } returns
            RemoteAlert.BatteryAlert(
                alertId = 7L,
                batteryPercentage = 23,
                alertMode = AlertMode.PERIODIC,
            )

        renderPresenter(AddNewRemoteAlertScreen(alertId = 7L))

        assertTrue(currentState().isEditMode)
        assertEquals(AlertType.BATTERY, currentState().selectedAlertType)
        assertEquals(AlertMode.PERIODIC, currentState().selectedAlertMode)
        assertEquals(23, currentState().threshold)
    }

    @Test
    fun missingAlertInEditModePopsNavigator() {
        coEvery { remoteAlertRepository.getRemoteAlertById(7L) } returns null

        renderPresenter(AddNewRemoteAlertScreen(alertId = 7L))

        io.mockk.verify { navigator.pop(null) }
    }

    @Test
    fun storageThresholdIsClampedToStorageSliderMax() {
        coEvery { remoteAlertRepository.getRemoteAlertById(7L) } returns
            RemoteAlert.StorageAlert(
                alertId = 7L,
                storageMinSpaceGb = 25,
                alertMode = AlertMode.THRESHOLD,
            )

        renderPresenter(AddNewRemoteAlertScreen(alertId = 7L))

        assertEquals(AlertType.STORAGE, currentState().selectedAlertType)
        assertEquals(20, currentState().storageSliderMax)
        assertEquals(20, currentState().threshold)
    }

    @Test
    fun saveNotificationInCreateModeSavesNewAlertAndPops() {
        val state = renderPresenter(AddNewRemoteAlertScreen())
        val alert = RemoteAlert.BatteryAlert(batteryPercentage = 15)

        composeRule.runOnIdle {
            state.eventSink(AddNewRemoteAlertScreen.Event.SaveNotification(alert))
        }
        composeRule.waitForIdle()

        coVerify { remoteAlertRepository.saveRemoteAlert(alert) }
        coVerify { analytics.logAlertAdded(AlertType.BATTERY) }
        io.mockk.verify { navigator.pop(null) }
    }

    @Test
    fun saveNotificationInEditModeUpdatesExistingAlertIdAndPops() {
        coEvery { remoteAlertRepository.getRemoteAlertById(7L) } returns
            RemoteAlert.BatteryAlert(
                alertId = 7L,
                batteryPercentage = 20,
            )
        val state = renderPresenter(AddNewRemoteAlertScreen(alertId = 7L))

        composeRule.runOnIdle {
            state.eventSink(
                AddNewRemoteAlertScreen.Event.SaveNotification(
                    RemoteAlert.BatteryAlert(batteryPercentage = 15),
                ),
            )
        }
        composeRule.waitForIdle()

        coVerify {
            remoteAlertRepository.updateRemoteAlert(
                RemoteAlert.BatteryAlert(
                    alertId = 7L,
                    batteryPercentage = 15,
                ),
            )
        }
        coVerify { analytics.logAlertEdited(AlertType.BATTERY) }
        io.mockk.verify { navigator.pop(null) }
    }

    @Test
    fun hideBatteryOptimizationReminderPersistsPreferenceAndAnalytics() {
        val state = renderPresenter(AddNewRemoteAlertScreen())

        composeRule.runOnIdle {
            state.eventSink(AddNewRemoteAlertScreen.Event.HideBatteryOptimizationReminder)
        }
        composeRule.waitForIdle()

        coVerify { appPreferencesDataStore.setHideBatteryOptReminder(true) }
        coVerify { analytics.logOptimizeBatteryIgnore() }
    }

    private var latestState by mutableStateOf<AddNewRemoteAlertScreen.State?>(null)

    private fun renderPresenter(screen: AddNewRemoteAlertScreen): AddNewRemoteAlertScreen.State {
        val presenter =
            AddNewRemoteAlertPresenter(
                screen = screen,
                navigator = navigator,
                remoteAlertRepository = remoteAlertRepository,
                storageMonitor = storageMonitor,
                appPreferencesDataStore = appPreferencesDataStore,
                emailConfigDataStore = emailConfigDataStore,
                analytics = analytics,
            )

        composeRule.setContent {
            latestState = presenter.present()
        }
        composeRule.waitForIdle()
        return currentState()
    }

    private fun currentState(): AddNewRemoteAlertScreen.State = requireNotNull(latestState)
}
