package dev.hossain.remotenotify.ui.alertchecklog

import com.google.common.truth.Truth.assertThat
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AlertCheckLogViewerPresenterTest {
    // System under test
    private lateinit var presenter: AlertCheckLogViewerPresenter

    // Mock dependencies
    private val mockNavigator = FakeNavigator(AlertCheckLogViewerScreen)
    private val mockAppPreferencesDataStore = mockk<AppPreferencesDataStore>()
    private val mockRepository = mockk<RemoteAlertRepository>()
    private val mockAnalytics = mockk<Analytics>(relaxed = true)

    private val sampleLogs =
        listOf(
            AlertCheckLog(
                checkedOn = 1000L,
                alertType = AlertType.BATTERY,
                isAlertSent = true,
                notifierType = NotifierType.EMAIL,
                stateValue = 15,
                configId = 1,
                configBatteryPercentage = 20,
                configStorageMinSpaceGb = 0,
                configCreatedOn = 500L,
            ),
            AlertCheckLog(
                checkedOn = 2000L,
                alertType = AlertType.STORAGE,
                isAlertSent = false,
                notifierType = null,
                stateValue = 50,
                configId = 2,
                configBatteryPercentage = 0,
                configStorageMinSpaceGb = 10,
                configCreatedOn = 800L,
            ),
            AlertCheckLog(
                checkedOn = 3000L,
                alertType = AlertType.BATTERY,
                isAlertSent = true,
                notifierType = NotifierType.TELEGRAM,
                stateValue = 10,
                configId = 1,
                configBatteryPercentage = 20,
                configStorageMinSpaceGb = 0,
                configCreatedOn = 500L,
            ),
        )

    @Before
    fun setup() {
        // Setup default mock behaviors
        coEvery { mockAppPreferencesDataStore.workerIntervalFlow } returns flowOf(60L)
        coEvery { mockRepository.getAllAlertCheckLogs() } returns flowOf(sampleLogs)

        presenter =
            AlertCheckLogViewerPresenter(
                navigator = mockNavigator,
                appPreferencesDataStore = mockAppPreferencesDataStore,
                remoteAlertRepository = mockRepository,
                analytics = mockAnalytics,
            )
    }

    @Test
    fun `when presenter is initialized then state contains all logs`() =
        runTest {
            presenter.test {
                val state = awaitItem()

                assertThat(state.logs).hasSize(3)
                assertThat(state.filteredLogs).hasSize(3)
                assertThat(state.isLoading).isFalse()
                assertThat(state.showTriggeredOnly).isFalse()
                assertThat(state.selectedAlertType).isNull()
                assertThat(state.selectedNotifierType).isNull()
            }
        }

    @Test
    fun `when ToggleTriggeredOnly event is triggered then filters logs`() =
        runTest {
            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.filteredLogs).hasSize(3)

                initialState.eventSink(AlertCheckLogViewerScreen.Event.ToggleTriggeredOnly)

                val filteredState = awaitItem()
                assertThat(filteredState.showTriggeredOnly).isTrue()
                assertThat(filteredState.filteredLogs).hasSize(2) // Only logs with isAlertSent = true
                assertThat(filteredState.filteredLogs.all { it.isAlertSent }).isTrue()
            }
        }

    @Test
    fun `when FilterByAlertType event is triggered then filters by alert type`() =
        runTest {
            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.filteredLogs).hasSize(3)

                initialState.eventSink(AlertCheckLogViewerScreen.Event.FilterByAlertType(AlertType.BATTERY))

                val filteredState = awaitItem()
                assertThat(filteredState.selectedAlertType).isEqualTo(AlertType.BATTERY)
                assertThat(filteredState.filteredLogs).hasSize(2) // Only battery logs
                assertThat(filteredState.filteredLogs.all { it.alertType == AlertType.BATTERY }).isTrue()
            }
        }

    @Test
    fun `when FilterByNotifierType event is triggered then filters by notifier type`() =
        runTest {
            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.filteredLogs).hasSize(3)

                initialState.eventSink(AlertCheckLogViewerScreen.Event.FilterByNotifierType(NotifierType.EMAIL))

                val filteredState = awaitItem()
                assertThat(filteredState.selectedNotifierType).isEqualTo(NotifierType.EMAIL)
                assertThat(filteredState.filteredLogs).hasSize(1) // Only email logs
                assertThat(filteredState.filteredLogs[0].notifierType).isEqualTo(NotifierType.EMAIL)
            }
        }

    @Test
    fun `when FilterByDateRange event is triggered then filters by date range`() =
        runTest {
            presenter.test {
                val initialState = awaitItem()
                assertThat(initialState.filteredLogs).hasSize(3)

                // Filter logs between 1500L and 2500L
                initialState.eventSink(AlertCheckLogViewerScreen.Event.FilterByDateRange(1500L, 2500L))

                val filteredState = awaitItem()
                assertThat(filteredState.dateRange).isEqualTo(Pair(1500L, 2500L))
                assertThat(filteredState.filteredLogs).hasSize(1) // Only log at 2000L
                assertThat(filteredState.filteredLogs[0].checkedOn).isEqualTo(2000L)
            }
        }

    @Test
    fun `when ClearFilters event is triggered then resets all filters`() =
        runTest {
            presenter.test {
                val initialState = awaitItem()

                // Apply multiple filters
                initialState.eventSink(AlertCheckLogViewerScreen.Event.ToggleTriggeredOnly)
                val filteredState1 = awaitItem()

                filteredState1.eventSink(AlertCheckLogViewerScreen.Event.FilterByAlertType(AlertType.BATTERY))
                val filteredState2 = awaitItem()

                // Clear all filters
                filteredState2.eventSink(AlertCheckLogViewerScreen.Event.ClearFilters)

                val clearedState = awaitItem()
                assertThat(clearedState.showTriggeredOnly).isFalse()
                assertThat(clearedState.selectedAlertType).isNull()
                assertThat(clearedState.selectedNotifierType).isNull()
                assertThat(clearedState.dateRange).isEqualTo(Pair(null, null))
                assertThat(clearedState.filteredLogs).hasSize(3) // All logs visible again
            }
        }

    @Test
    fun `when NavigateBack event is triggered then pops navigator`() =
        runTest {
            presenter.test {
                val state = awaitItem()
                state.eventSink(AlertCheckLogViewerScreen.Event.NavigateBack)

                mockNavigator.awaitPop()
            }
        }

    @Test
    fun `when multiple filters are applied then logs match all criteria`() =
        runTest {
            presenter.test {
                val initialState = awaitItem()

                // Apply triggered only filter
                initialState.eventSink(AlertCheckLogViewerScreen.Event.ToggleTriggeredOnly)
                val state1 = awaitItem()

                // Apply alert type filter
                state1.eventSink(AlertCheckLogViewerScreen.Event.FilterByAlertType(AlertType.BATTERY))
                val state2 = awaitItem()

                // Should only show battery logs that were triggered
                assertThat(state2.filteredLogs).hasSize(2)
                assertThat(state2.filteredLogs.all { it.alertType == AlertType.BATTERY && it.isAlertSent }).isTrue()
            }
        }

    @Test
    fun `when repository returns empty logs then state reflects empty state`() =
        runTest {
            coEvery { mockRepository.getAllAlertCheckLogs() } returns flowOf(emptyList())

            val emptyPresenter =
                AlertCheckLogViewerPresenter(
                    navigator = mockNavigator,
                    appPreferencesDataStore = mockAppPreferencesDataStore,
                    remoteAlertRepository = mockRepository,
                    analytics = mockAnalytics,
                )

            emptyPresenter.test {
                val state = awaitItem()

                assertThat(state.logs).isEmpty()
                assertThat(state.filteredLogs).isEmpty()
            }
        }

    @Test
    fun `when ExportLogs event is triggered then event is received`() =
        runTest {
            presenter.test {
                val state = awaitItem()
                // Export is a no-op in current implementation but should not crash
                state.eventSink(AlertCheckLogViewerScreen.Event.ExportLogs(state.filteredLogs))

                // No assertion needed, just verifying no crash
            }
        }

    @Test
    fun `when date range has only start date then filters correctly`() =
        runTest {
            presenter.test {
                val initialState = awaitItem()

                initialState.eventSink(AlertCheckLogViewerScreen.Event.FilterByDateRange(2000L, null))

                val filteredState = awaitItem()
                assertThat(filteredState.filteredLogs).hasSize(2) // Logs at 2000L and 3000L
                assertThat(filteredState.filteredLogs.all { it.checkedOn >= 2000L }).isTrue()
            }
        }

    @Test
    fun `when date range has only end date then filters correctly`() =
        runTest {
            presenter.test {
                val initialState = awaitItem()

                initialState.eventSink(AlertCheckLogViewerScreen.Event.FilterByDateRange(null, 2000L))

                val filteredState = awaitItem()
                assertThat(filteredState.filteredLogs).hasSize(2) // Logs at 1000L and 2000L
                assertThat(filteredState.filteredLogs.all { it.checkedOn <= 2000L }).isTrue()
            }
        }
}
