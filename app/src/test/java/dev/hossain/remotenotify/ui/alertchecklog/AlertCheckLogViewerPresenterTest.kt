package dev.hossain.remotenotify.ui.alertchecklog

import app.cash.molecule.RecompositionClock
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.NotifierType
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Tests for [AlertCheckLogViewerPresenter].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AlertCheckLogViewerPresenterTest {

    private lateinit var navigator: Navigator
    private lateinit var appPreferencesDataStore: AppPreferencesDataStore
    private lateinit var remoteAlertRepository: RemoteAlertRepository
    private lateinit var analytics: Analytics

    // Test data timestamps
    private val now = System.currentTimeMillis()
    private val timeT0 = now
    private val timeT1 = now - TimeUnit.HOURS.toMillis(1)
    private val timeT2 = now - TimeUnit.HOURS.toMillis(2)
    private val timeT3 = now - TimeUnit.DAYS.toMillis(1) // Yesterday
    private val timeT4 = now - TimeUnit.DAYS.toMillis(2) // Day before yesterday

    // Test data
    private val sampleLog1 = AlertCheckLog(id = 1, checkedOn = timeT1, alertType = AlertType.BATTERY, isAlertSent = true, notifierType = NotifierType.EMAIL, status = "Sent", details = "Battery low: 10%")
    private val sampleLog2 = AlertCheckLog(id = 2, checkedOn = timeT2, alertType = AlertType.STORAGE, isAlertSent = false, notifierType = null, status = "Not Sent", details = "Storage OK: 50%")
    private val sampleLog3 = AlertCheckLog(id = 3, checkedOn = timeT3, alertType = AlertType.BATTERY, isAlertSent = true, notifierType = NotifierType.WEBHOOK, status = "Sent", details = "Battery low: 5%")
    private val sampleLog4 = AlertCheckLog(id = 4, checkedOn = timeT4, alertType = AlertType.BATTERY, isAlertSent = false, notifierType = NotifierType.EMAIL, status = "Not Sent", details = "Battery OK: 30%")

    private val allLogs = listOf(sampleLog1, sampleLog2, sampleLog3, sampleLog4).sortedByDescending { it.checkedOn } // Presenter sorts by default

    @Before
    fun setUp() {
        navigator = mockk(relaxed = true)
        appPreferencesDataStore = mockk()
        remoteAlertRepository = mockk()
        analytics = mockk(relaxed = true)

        // Default mocks
        every { appPreferencesDataStore.workerIntervalFlow() } returns flowOf(60L) // Default interval
        every { remoteAlertRepository.getAllAlertCheckLogs() } returns flowOf(allLogs) // Default logs
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial state and data loading - starts loading, then shows logs and correct interval`() = runTest {
        val initialLogsFlow = MutableStateFlow<List<AlertCheckLog>>(emptyList())
        every { remoteAlertRepository.getAllAlertCheckLogs() } returns initialLogsFlow
        val workerInterval = 90L
        every { appPreferencesDataStore.workerIntervalFlow() } returns flowOf(workerInterval)

        moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            var state = awaitItem() // Initial state before logs are emitted by flow
            assertThat(state.isLoading).isTrue()
            assertThat(state.logs).isEmpty()
            assertThat(state.filteredLogs).isEmpty()
            assertThat(state.checkIntervalMinutes).isEqualTo(workerInterval)
            assertThat(state.showTriggeredOnly).isFalse()
            assertThat(state.selectedAlertType).isNull()
            assertThat(state.selectedNotifierType).isNull()
            assertThat(state.dateRange).isNull()

            // Emit logs
            initialLogsFlow.value = allLogs
            state = awaitItem() // State after logs are emitted

            assertThat(state.isLoading).isFalse()
            assertThat(state.logs).isEqualTo(allLogs)
            assertThat(state.filteredLogs).isEqualTo(allLogs) // Initially no filters
            assertThat(state.checkIntervalMinutes).isEqualTo(workerInterval)

            // Impression effect
            coVerify { analytics.logScreenView(AlertCheckLogViewerScreen::class) }
        }
    }

    @Test
    fun `event NavigateBack invokes navigator pop`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            val state = awaitItem() // Consume initial state
            state.eventSink(AlertCheckLogScreenEvent.NavigateBack)
            coVerify { navigator.pop() }
        }
    }

    @Test
    fun `event ToggleTriggeredOnly filters logs to show only triggered alerts and back`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            var state = awaitItem() // Initial state with all logs

            // Toggle to show triggered only
            state.eventSink(AlertCheckLogScreenEvent.ToggleTriggeredOnly)
            state = awaitItem()
            assertThat(state.showTriggeredOnly).isTrue()
            assertThat(state.filteredLogs).containsExactlyElementsIn(allLogs.filter { it.isAlertSent })
            assertThat(state.filteredLogs).containsExactly(sampleLog1, sampleLog3)


            // Toggle back
            state.eventSink(AlertCheckLogScreenEvent.ToggleTriggeredOnly)
            state = awaitItem()
            assertThat(state.showTriggeredOnly).isFalse()
            assertThat(state.filteredLogs).isEqualTo(allLogs)
        }
    }

    @Test
    fun `event FilterByAlertType filters logs and clears filter`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            var state = awaitItem()

            // Filter by BATTERY
            state.eventSink(AlertCheckLogScreenEvent.FilterByAlertType(AlertType.BATTERY))
            state = awaitItem()
            assertThat(state.selectedAlertType).isEqualTo(AlertType.BATTERY)
            assertThat(state.filteredLogs).containsExactlyElementsIn(allLogs.filter { it.alertType == AlertType.BATTERY })
            assertThat(state.filteredLogs).containsExactly(sampleLog1, sampleLog3, sampleLog4)


            // Clear filter by passing null
            state.eventSink(AlertCheckLogScreenEvent.FilterByAlertType(null))
            state = awaitItem()
            assertThat(state.selectedAlertType).isNull()
            assertThat(state.filteredLogs).isEqualTo(allLogs)
        }
    }

    @Test
    fun `event FilterByNotifierType filters logs and clears filter`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            var state = awaitItem()

            // Filter by EMAIL
            state.eventSink(AlertCheckLogScreenEvent.FilterByNotifierType(NotifierType.EMAIL))
            state = awaitItem()
            assertThat(state.selectedNotifierType).isEqualTo(NotifierType.EMAIL)
            assertThat(state.filteredLogs).containsExactlyElementsIn(allLogs.filter { it.notifierType == NotifierType.EMAIL })
            assertThat(state.filteredLogs).containsExactly(sampleLog1, sampleLog4)

            // Clear filter by passing null
            state.eventSink(AlertCheckLogScreenEvent.FilterByNotifierType(null))
            state = awaitItem()
            assertThat(state.selectedNotifierType).isNull()
            assertThat(state.filteredLogs).isEqualTo(allLogs)
        }
    }

    @Test
    fun `event FilterByDateRange filters logs and clears filter`() = runTest {
        // Using existing sampleLog timestamps: timeT0, timeT1, timeT2, timeT3, timeT4
        // Range: from timeT3 (inclusive) to timeT1 (inclusive)
        val startDate = timeT3
        val endDate = timeT1

        val expectedLogsInDateRange = allLogs.filter { it.checkedOn >= startDate && it.checkedOn <= endDate }
                                            .sortedByDescending { it.checkedOn }
        assertThat(expectedLogsInDateRange).containsExactly(sampleLog1, sampleLog3) // sampleLog2 is T2, sampleLog4 is T4

        moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            var state = awaitItem()

            state.eventSink(AlertCheckLogScreenEvent.FilterByDateRange(startDate, endDate))
            state = awaitItem()
            assertThat(state.dateRange).isNotNull()
            assertThat(state.dateRange?.first).isEqualTo(startDate)
            assertThat(state.dateRange?.second).isEqualTo(endDate)
            assertThat(state.filteredLogs).isEqualTo(expectedLogsInDateRange)

            // Clear filter
            state.eventSink(AlertCheckLogScreenEvent.FilterByDateRange(null, null))
            state = awaitItem()
            assertThat(state.dateRange).isNull()
            assertThat(state.filteredLogs).isEqualTo(allLogs)
        }
    }


    @Test
    fun `event ClearFilters resets all filters to default values`() = runTest {
        moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            var state = awaitItem()

            // Apply some filters
            state.eventSink(AlertCheckLogScreenEvent.ToggleTriggeredOnly) // showTriggeredOnly = true
            state = awaitItem()
            state.eventSink(AlertCheckLogScreenEvent.FilterByAlertType(AlertType.BATTERY))
            state = awaitItem()
            state.eventSink(AlertCheckLogScreenEvent.FilterByNotifierType(NotifierType.EMAIL))
            state = awaitItem()
            val testDate = System.currentTimeMillis()
            state.eventSink(AlertCheckLogScreenEvent.FilterByDateRange(testDate - 1000, testDate))
            state = awaitItem()


            // Verify filters are applied (brief check, main focus is on clearing)
            assertThat(state.showTriggeredOnly).isTrue()
            assertThat(state.selectedAlertType).isEqualTo(AlertType.BATTERY)
            assertThat(state.selectedNotifierType).isEqualTo(NotifierType.EMAIL)
            assertThat(state.dateRange).isNotNull()

            // Clear filters
            state.eventSink(AlertCheckLogScreenEvent.ClearFilters)
            state = awaitItem()

            // Verify all filters are reset
            assertThat(state.showTriggeredOnly).isFalse()
            assertThat(state.selectedAlertType).isNull()
            assertThat(state.selectedNotifierType).isNull()
            assertThat(state.dateRange).isNull()
            assertThat(state.filteredLogs).isEqualTo(allLogs) // Reverts to all logs
        }
    }

    @Test
    fun `event ExportLogs is processed without error`() = runTest {
         moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            val state = awaitItem()
            // This event primarily triggers UI (like showing a share sheet or snackbar),
            // which is not directly testable in the presenter.
            // We just ensure the event is received and doesn't cause a crash.
            state.eventSink(AlertCheckLogScreenEvent.ExportLogs)
            // No specific state change in the presenter is expected for this event.
            // We just consume any potential recomposition.
            expectMostRecentItem()
            // Analytics for export could be verified if added
            // coVerify { analytics.logExportLogs() } // Example if such analytics existed
        }
    }

    @Test
    fun `combined filters work correctly - triggered, battery type, specific date`() = runTest {
        // Test Data:
        // sampleLog1: T1, BATTERY, isAlertSent = true, EMAIL
        // sampleLog2: T2, STORAGE, isAlertSent = false, null
        // sampleLog3: T3, BATTERY, isAlertSent = true, WEBHOOK
        // sampleLog4: T4, BATTERY, isAlertSent = false, EMAIL

        val filterDate = timeT1 // Exact date of sampleLog1

        moleculeFlow(RecompositionClock.Immediate) {
            AlertCheckLogViewerPresenter(navigator, appPreferencesDataStore, remoteAlertRepository, analytics)
        }.test {
            var state = awaitItem()

            // 1. Filter by showTriggeredOnly = true
            state.eventSink(AlertCheckLogScreenEvent.ToggleTriggeredOnly)
            state = awaitItem()
            // Expected: sampleLog1, sampleLog3
            assertThat(state.filteredLogs).containsExactly(sampleLog1, sampleLog3)

            // 2. Additionally filter by AlertType = BATTERY
            state.eventSink(AlertCheckLogScreenEvent.FilterByAlertType(AlertType.BATTERY))
            state = awaitItem()
            // Expected: sampleLog1, sampleLog3 (both are BATTERY)
            assertThat(state.filteredLogs).containsExactly(sampleLog1, sampleLog3)

            // 3. Additionally filter by a date range that ONLY includes sampleLog1 (timeT1)
            // To make the range specific to a single day, set start and end of that day.
            val cal = Calendar.getInstance().apply { timeInMillis = filterDate }
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis

            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val dayEnd = cal.timeInMillis

            state.eventSink(AlertCheckLogScreenEvent.FilterByDateRange(dayStart, dayEnd))
            state = awaitItem()
            // Expected: sampleLog1 (isAlertSent=true, type=BATTERY, checkedOn=timeT1)
            assertThat(state.filteredLogs).containsExactly(sampleLog1)

            // 4. Additionally filter by NotifierType = EMAIL (sampleLog1 is EMAIL)
            state.eventSink(AlertCheckLogScreenEvent.FilterByNotifierType(NotifierType.EMAIL))
            state = awaitItem()
            assertThat(state.filteredLogs).containsExactly(sampleLog1) // Still sampleLog1

            // 5. Change NotifierType to WEBHOOK (sampleLog1 is not WEBHOOK, so list should be empty)
            state.eventSink(AlertCheckLogScreenEvent.FilterByNotifierType(NotifierType.WEBHOOK))
            state = awaitItem()
            assertThat(state.filteredLogs).isEmpty()

            // Clear all filters
            state.eventSink(AlertCheckLogScreenEvent.ClearFilters)
            state = awaitItem()
            assertThat(state.filteredLogs).isEqualTo(allLogs)
        }
    }
}
