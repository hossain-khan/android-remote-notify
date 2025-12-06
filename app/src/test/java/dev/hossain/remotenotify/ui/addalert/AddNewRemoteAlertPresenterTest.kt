package dev.hossain.remotenotify.ui.addalert

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.StorageMonitor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddNewRemoteAlertPresenterTest {
    // System under test
    private lateinit var presenter: AddNewRemoteAlertPresenter

    // Mock dependencies
    private val mockNavigator = FakeNavigator(AddNewRemoteAlertScreen())
    private val mockRepository = mockk<RemoteAlertRepository>()
    private val mockStorageMonitor = mockk<StorageMonitor>()
    private val mockAppPreferencesDataStore = mockk<AppPreferencesDataStore>()
    private val mockAnalytics = mockk<Analytics>(relaxed = true)

    @Before
    fun setup() {
        // Setup default mock behaviors
        every { mockStorageMonitor.getAvailableStorageInGB() } returns 50L
        coEvery { mockAppPreferencesDataStore.hideBatteryOptReminder } returns flowOf(false)
    }

    @Test
    fun `when presenter is initialized for new alert then isEditMode is false`() =
        runTest {
            val screen = AddNewRemoteAlertScreen()
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val state = awaitItem()

                assertThat(state.isEditMode).isFalse()
                assertThat(state.selectedAlertType).isEqualTo(AlertType.BATTERY)
                assertThat(state.threshold).isEqualTo(10)
                assertThat(state.availableStorage).isEqualTo(50)
            }
        }

    @Test
    fun `when presenter is initialized for edit mode then loads existing alert`() =
        runTest {
            val existingAlert = RemoteAlert.BatteryAlert(alertId = 123, batteryPercentage = 25)
            coEvery { mockRepository.getRemoteAlertById(123) } returns existingAlert

            val screen = AddNewRemoteAlertScreen(alertId = 123)
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val state = awaitItem()

                assertThat(state.isEditMode).isTrue()
                assertThat(state.selectedAlertType).isEqualTo(AlertType.BATTERY)
                assertThat(state.threshold).isEqualTo(25)
            }
        }

    @Test
    fun `when SaveNotification event is triggered for new alert then saves and navigates back`() =
        runTest {
            val screen = AddNewRemoteAlertScreen()
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            val notification = RemoteAlert.BatteryAlert(batteryPercentage = 15)
            val savedSlot = slot<RemoteAlert>()
            coEvery { mockRepository.saveRemoteAlert(capture(savedSlot)) } returns Unit

            presenter.test {
                val state = awaitItem()
                state.eventSink(AddNewRemoteAlertScreen.Event.SaveNotification(notification))

                coVerify { mockRepository.saveRemoteAlert(any()) }
                assertThat(savedSlot.captured).isInstanceOf(RemoteAlert.BatteryAlert::class.java)
                assertThat((savedSlot.captured as RemoteAlert.BatteryAlert).batteryPercentage).isEqualTo(15)
                mockNavigator.awaitPop()
            }
        }

    @Test
    fun `when SaveNotification event is triggered in edit mode then updates and navigates back`() =
        runTest {
            val existingAlert = RemoteAlert.BatteryAlert(alertId = 123, batteryPercentage = 20)
            coEvery { mockRepository.getRemoteAlertById(123) } returns existingAlert
            coEvery { mockRepository.updateRemoteAlert(any()) } returns 1

            val screen = AddNewRemoteAlertScreen(alertId = 123)
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            val updatedNotification = RemoteAlert.BatteryAlert(batteryPercentage = 30)
            val updatedSlot = slot<RemoteAlert>()
            coEvery { mockRepository.updateRemoteAlert(capture(updatedSlot)) } returns 1

            presenter.test {
                val state = awaitItem()
                state.eventSink(AddNewRemoteAlertScreen.Event.SaveNotification(updatedNotification))

                coVerify { mockRepository.updateRemoteAlert(any()) }
                assertThat(updatedSlot.captured).isInstanceOf(RemoteAlert.BatteryAlert::class.java)
                val updated = updatedSlot.captured as RemoteAlert.BatteryAlert
                assertThat(updated.alertId).isEqualTo(123)
                assertThat(updated.batteryPercentage).isEqualTo(30)
                mockNavigator.awaitPop()
            }
        }

    @Test
    fun `when NavigateBack event is triggered then pops navigator`() =
        runTest {
            val screen = AddNewRemoteAlertScreen()
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val state = awaitItem()
                state.eventSink(AddNewRemoteAlertScreen.Event.NavigateBack)

                mockNavigator.awaitPop()
            }
        }

    @Test
    fun `when UpdateAlertType event is triggered then state reflects new alert type`() =
        runTest {
            val screen = AddNewRemoteAlertScreen()
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.selectedAlertType).isEqualTo(AlertType.BATTERY)

                initialState.eventSink(AddNewRemoteAlertScreen.Event.UpdateAlertType(AlertType.STORAGE))

                val updatedState = awaitItem()
                assertThat(updatedState.selectedAlertType).isEqualTo(AlertType.STORAGE)
            }
        }

    @Test
    fun `when UpdateThreshold event is triggered then state reflects new threshold`() =
        runTest {
            val screen = AddNewRemoteAlertScreen()
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.threshold).isEqualTo(10)

                initialState.eventSink(AddNewRemoteAlertScreen.Event.UpdateThreshold(25))

                val updatedState = awaitItem()
                assertThat(updatedState.threshold).isEqualTo(25)
            }
        }

    @Test
    fun `when ShowBatteryOptimizationSheet event is triggered then showBatteryOptSheet becomes true`() =
        runTest {
            val screen = AddNewRemoteAlertScreen()
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.showBatteryOptSheet).isFalse()

                initialState.eventSink(AddNewRemoteAlertScreen.Event.ShowBatteryOptimizationSheet)

                val updatedState = awaitItem()
                assertThat(updatedState.showBatteryOptSheet).isTrue()
            }
        }

    @Test
    fun `when DismissBatteryOptimizationSheet event is triggered then showBatteryOptSheet becomes false`() =
        runTest {
            val screen = AddNewRemoteAlertScreen()
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val initialState = awaitItem()
                initialState.eventSink(AddNewRemoteAlertScreen.Event.ShowBatteryOptimizationSheet)
                val shownState = awaitItem()
                assertThat(shownState.showBatteryOptSheet).isTrue()

                shownState.eventSink(AddNewRemoteAlertScreen.Event.DismissBatteryOptimizationSheet)

                val dismissedState = awaitItem()
                assertThat(dismissedState.showBatteryOptSheet).isFalse()
            }
        }

    @Test
    fun `when HideBatteryOptimizationReminder event is triggered then saves preference`() =
        runTest {
            val screen = AddNewRemoteAlertScreen()
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            coEvery { mockAppPreferencesDataStore.setHideBatteryOptReminder(true) } returns Unit

            presenter.test {
                val state = awaitItem()
                state.eventSink(AddNewRemoteAlertScreen.Event.HideBatteryOptimizationReminder)

                coVerify { mockAppPreferencesDataStore.setHideBatteryOptReminder(true) }
            }
        }

    @Test
    fun `when editing storage alert then loads correct values`() =
        runTest {
            val existingAlert = RemoteAlert.StorageAlert(alertId = 456, storageMinSpaceGb = 15)
            coEvery { mockRepository.getRemoteAlertById(456) } returns existingAlert

            val screen = AddNewRemoteAlertScreen(alertId = 456)
            presenter =
                AddNewRemoteAlertPresenter(
                    screen = screen,
                    navigator = mockNavigator,
                    remoteAlertRepository = mockRepository,
                    storageMonitor = mockStorageMonitor,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    analytics = mockAnalytics,
                )

            presenter.test {
                val state = awaitItem()

                assertThat(state.isEditMode).isTrue()
                assertThat(state.selectedAlertType).isEqualTo(AlertType.STORAGE)
                assertThat(state.threshold).isEqualTo(15)
            }
        }
}
