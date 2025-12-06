package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
import dev.hossain.remotenotify.ui.alertchecklog.AlertCheckLogViewerScreen
import dev.hossain.remotenotify.ui.alertmediumlist.NotificationMediumListScreen
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AlertsListPresenterTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // System under test
    private lateinit var presenter: AlertsListPresenter

    // Mock dependencies
    private val mockNavigator = FakeNavigator(AlertsListScreen)
    private val mockRepository = mockk<RemoteAlertRepository>()
    private val mockBatteryMonitor = mockk<BatteryMonitor>()
    private val mockStorageMonitor = mockk<StorageMonitor>()
    private val mockNotifiers = mockk<Set<NotificationSender>>()
    private val mockAppPreferencesDataStore = mockk<AppPreferencesDataStore>()
    private val mockAnalytics = mockk<Analytics>(relaxed = true)

    @Before
    fun setup() {
        // Setup default mock behaviors
        every { mockBatteryMonitor.getBatteryLevel() } returns 50
        every { mockStorageMonitor.getAvailableStorageInGB() } returns 10L
        every { mockStorageMonitor.getTotalStorageInGB() } returns 100L
        coEvery { mockRepository.getAllRemoteAlertFlow() } returns flowOf(emptyList())
        coEvery { mockRepository.getLatestCheckLog() } returns flowOf(null)
        coEvery { mockNotifiers.any { it.hasValidConfig() } } returns false

        presenter =
            AlertsListPresenter(
                navigator = mockNavigator,
                remoteAlertRepository = mockRepository,
                batteryMonitor = mockBatteryMonitor,
                storageMonitor = mockStorageMonitor,
                notifiers = mockNotifiers,
                appPreferencesDataStore = mockAppPreferencesDataStore,
                analytics = mockAnalytics,
            )
    }

    @Test
    fun `when presenter is initialized then state contains device info`() {
        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val state = awaitItem()

                    assertThat(state.batteryPercentage).isEqualTo(50)
                    assertThat(state.availableStorage).isEqualTo(10L)
                    assertThat(state.totalStorage).isEqualTo(100L)
                    assertThat(state.remoteAlertConfigs).isEmpty()
                    assertThat(state.isAnyNotifierConfigured).isFalse()
                }
            }
        }
    }

    @Test
    fun `when DeleteNotification event is triggered then alert is deleted from repository`() {
        val alert = RemoteAlert.BatteryAlert(alertId = 1, batteryPercentage = 20)
        coEvery { mockRepository.deleteRemoteAlert(alert) } returns Unit

        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val state = awaitItem()
                    state.eventSink(AlertsListScreen.Event.DeleteNotification(alert))

                    coVerify { mockRepository.deleteRemoteAlert(alert) }
                }
            }
        }
    }

    @Test
    fun `when EditRemoteAlert event is triggered then navigates to edit screen with alertId`() {
        val alert = RemoteAlert.BatteryAlert(alertId = 123, batteryPercentage = 20)

        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val state = awaitItem()
                    state.eventSink(AlertsListScreen.Event.EditRemoteAlert(alert))

                    assertThat(mockNavigator.awaitNextScreen())
                        .isEqualTo(AddNewRemoteAlertScreen(alertId = 123))
                }
            }
        }
    }

    @Test
    fun `when AddNotification event is triggered then navigates to add new alert screen`() {
        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val state = awaitItem()
                    state.eventSink(AlertsListScreen.Event.AddNotification)

                    assertThat(mockNavigator.awaitNextScreen())
                        .isEqualTo(AddNewRemoteAlertScreen())
                }
            }
        }
    }

    @Test
    fun `when AddNotificationDestination event is triggered then navigates to notification medium list`() {
        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val state = awaitItem()
                    state.eventSink(AlertsListScreen.Event.AddNotificationDestination)

                    assertThat(mockNavigator.awaitNextScreen())
                        .isEqualTo(NotificationMediumListScreen)
                }
            }
        }
    }

    @Test
    fun `when ShowEducationSheet event is triggered then showEducationSheet becomes true`() {
        coEvery { mockAppPreferencesDataStore.markEducationDialogShown() } returns Unit

        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val initialState = awaitItem()
                    assertThat(initialState.showEducationSheet).isFalse()

                    initialState.eventSink(AlertsListScreen.Event.ShowEducationSheet)

                    val updatedState = awaitItem()
                    assertThat(updatedState.showEducationSheet).isTrue()
                    coVerify { mockAppPreferencesDataStore.markEducationDialogShown() }
                }
            }
        }
    }

    @Test
    fun `when DismissEducationSheet event is triggered then showEducationSheet becomes false`() {
        coEvery { mockAppPreferencesDataStore.markEducationDialogShown() } returns Unit

        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val initialState = awaitItem()
                    // First show the sheet
                    initialState.eventSink(AlertsListScreen.Event.ShowEducationSheet)
                    val shownState = awaitItem()
                    assertThat(shownState.showEducationSheet).isTrue()

                    // Then dismiss it
                    shownState.eventSink(AlertsListScreen.Event.DismissEducationSheet)
                    val dismissedState = awaitItem()
                    assertThat(dismissedState.showEducationSheet).isFalse()
                }
            }
        }
    }

    @Test
    fun `when ViewAllLogs event is triggered then navigates to log viewer screen`() {
        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val state = awaitItem()
                    state.eventSink(AlertsListScreen.Event.ViewAllLogs)

                    assertThat(mockNavigator.awaitNextScreen())
                        .isEqualTo(AlertCheckLogViewerScreen)
                }
            }
        }
    }

    @Test
    fun `when repository returns alerts then state contains those alerts`() {
        val alerts =
            listOf(
                RemoteAlert.BatteryAlert(alertId = 1, batteryPercentage = 20),
                RemoteAlert.StorageAlert(alertId = 2, storageMinSpaceGb = 10),
            )
        coEvery { mockRepository.getAllRemoteAlertFlow() } returns flowOf(alerts)

        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val state = awaitItem()

                    assertThat(state.remoteAlertConfigs).hasSize(2)
                    assertThat(state.remoteAlertConfigs[0]).isInstanceOf(RemoteAlert.BatteryAlert::class.java)
                    assertThat(state.remoteAlertConfigs[1]).isInstanceOf(RemoteAlert.StorageAlert::class.java)
                }
            }
        }
    }

    @Test
    fun `when notifiers are configured then isAnyNotifierConfigured is true`() {
        coEvery { mockNotifiers.any { it.hasValidConfig() } } returns true

        composeTestRule.setContent {
            runTest {
                presenter.test {
                    val state = awaitItem()

                    assertThat(state.isAnyNotifierConfigured).isTrue()
                }
            }
        }
    }
}
