//package dev.hossain.remotenotify.ui.alertlist
//
//import android.content.Context
//import androidx.work.WorkInfo
//import androidx.work.WorkManager
//import app.cash.molecule.RecompositionClock
//import app.cash.molecule.moleculeFlow
//import app.cash.turbine.test
//import com.google.common.truth.Truth.assertThat
//import dev.hossain.remotenotify.BuildConfig
//import dev.hossain.remotenotify.R
//import dev.hossain.remotenotify.RemoteNotifyApplication
//import dev.hossain.remotenotify.TestNavGraphs
//import dev.hossain.remotenotify.analytics.Analytics
//import dev.hossain.remotenotify.data.AppPreferencesDataStore
//import dev.hossain.remotenotify.data.RemoteAlert
//import dev.hossain.remotenotify.data.RemoteAlertRepository
//import dev.hossain.remotenotify.model.AlertCheckLog
//import dev.hossain.remotenotify.model.AlertType
//import dev.hossain.remotenotify.model.DeviceState
//import dev.hossain.remotenotify.model.NotificationSender
//import dev.hossain.remotenotify.model.NotifierType
//import dev.hossain.remotenotify.platform.BatteryMonitor
//import dev.hossain.remotenotify.platform.StorageMonitor
//import dev.hossain.remotenotify.service.DEVICE_VITALS_CHECKER_WORKER_ID
//import dev.hossain.remotenotify.ui.NavGraphs
//import dev.hossain.remotenotify.ui.about.AboutAppScreen
//import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
//import dev.hossain.remotenotify.ui.alertchecklog.AlertCheckLogViewerScreen
//import dev.hossain.remotenotify.ui.notifmedium.NotificationMediumListScreen
//import io.mockk.coEvery
//import io.mockk.coVerify
//import io.mockk.every
//import io.mockk.mockk
//import io.mockk.mockkStatic
//import io.mockk.unmockkAll
//import io.mockk.verify
//import kotlinx.coroutines.ExperimentalCoroutinesApi
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.emptyFlow
//import kotlinx.coroutines.flow.flowOf
//import kotlinx.coroutines.test.runTest
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import java.util.UUID
//
///**
// * Unit tests for [AlertsListPresenter].
// */
//@OptIn(ExperimentalCoroutinesApi::class)
//class AlertsListPresenterTest {
//    private lateinit var navigator: Navigator
//    private lateinit var remoteAlertRepository: RemoteAlertRepository
//    private lateinit var batteryMonitor: BatteryMonitor
//    private lateinit var storageMonitor: StorageMonitor
//    private lateinit var mockNotifiers: Set<NotificationSender>
//    private lateinit var appPreferencesDataStore: AppPreferencesDataStore
//    private lateinit var analytics: Analytics
//    private lateinit var mockContext: Context
//    private lateinit var mockWorkManager: WorkManager
//
//    // Test data
//    private val sampleAlert1 = RemoteAlert.BatteryAlert(conditionId = "bat_low_1", threshold = 15, isEnabled = true)
//    private val sampleAlert2 = RemoteAlert.StorageAlert(conditionId = "sto_low_1", threshold = 10, isEnabled = true)
//    private val sampleAlerts = listOf(sampleAlert1, sampleAlert2)
//    private val sampleLog = AlertCheckLog(id = 1, checkedOn = System.currentTimeMillis(), alertType = AlertType.BATTERY, isAlertSent = true, notifierType = NotifierType.EMAIL, status = "Sent", details = "Battery low")
//
//    @Before
//    fun setUp() {
//        navigator = mockk(relaxed = true)
//        remoteAlertRepository = mockk(relaxed = true) // relaxed for deleteRemoteAlert
//        batteryMonitor = mockk()
//        storageMonitor = mockk()
//        mockNotifiers = emptySet() // Will be configured per test
//        appPreferencesDataStore = mockk(relaxed = true) // relaxed for markEducationDialogShown
//        analytics = mockk(relaxed = true)
//        // It's better to mock Application context if WorkManager requires it.
//        mockContext = mockk<RemoteNotifyApplication>(relaxed = true)
//        mockWorkManager = mockk()
//
//        // Mock static WorkManager.getInstance(context)
//        mockkStatic(WorkManager::class)
//        every { WorkManager.getInstance(mockContext) } returns mockWorkManager
//
//        // Default mocks for flows to prevent null pointer exceptions if not overridden by a specific test
//        coEvery { batteryMonitor.getBatteryLevel() } returns flowOf(DeviceState.BatteryLevel(80))
//        coEvery { storageMonitor.getAvailableStorageInGB() } returns flowOf(DeviceState.StorageSpace(200))
//        coEvery { storageMonitor.getTotalStorageInGB() } returns flowOf(DeviceState.StorageSpace(500))
//        coEvery { remoteAlertRepository.getAllRemoteAlertFlow() } returns emptyFlow()
//        coEvery { remoteAlertRepository.getLatestCheckLog() } returns emptyFlow()
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns emptyFlow()
//        coEvery { appPreferencesDataStore.isEducationDialogShown() } returns flowOf(false) // Default: not shown
//
//        // Mock NavGraphs to avoid "No NavGraph found" error if navigator.goTo is called
//        // This is crucial if your navigator extension functions rely on finding these graphs.
//        mockkStatic(NavGraphs::class)
//        every { NavGraphs.root } returns TestNavGraphs.Root // Assuming TestNavGraphs is a test utility
//    }
//
//    @After
//    fun tearDown() {
//        unmockkAll()
//    }
//
//    private fun createPresenter(): AlertsListPresenter {
//        return AlertsListPresenter(
//            navigator = navigator,
//            remoteAlertRepository = remoteAlertRepository,
//            batteryMonitor = batteryMonitor,
//            storageMonitor = storageMonitor,
//            notifiers = mockNotifiers,
//            appPreferences = appPreferencesDataStore,
//            analytics = analytics,
//            appContext = mockContext
//        )
//    }
//
//    @Test
//    fun `initial state - data loads, no notifier, education not shown -> showEducationSheet is true`() = runTest {
//        val batteryLvl = 75
//        val availStorage = 150
//        val totalStorage = 256
//        val workInfoList = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, mutableMapOf(), mutableListOf(), 0))
//
//        coEvery { batteryMonitor.getBatteryLevel() } returns flowOf(DeviceState.BatteryLevel(batteryLvl))
//        coEvery { storageMonitor.getAvailableStorageInGB() } returns flowOf(DeviceState.StorageSpace(availStorage))
//        coEvery { storageMonitor.getTotalStorageInGB() } returns flowOf(DeviceState.StorageSpace(totalStorage))
//        coEvery { remoteAlertRepository.getAllRemoteAlertFlow() } returns flowOf(sampleAlerts)
//        coEvery { remoteAlertRepository.getLatestCheckLog() } returns flowOf(sampleLog)
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns flowOf(workInfoList)
//        coEvery { appPreferencesDataStore.isEducationDialogShown() } returns flowOf(false) // Education sheet NOT shown before
//
//        // No notifiers configured
//        val notifier1 = mockk<NotificationSender>()
//        every { notifier1.hasValidConfig() } returns false
//        mockNotifiers = setOf(notifier1)
//
//
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            val state = awaitItem()
//
//            assertThat(state.remoteAlertConfigs).isEqualTo(sampleAlerts)
//            assertThat(state.batteryPercentage).isEqualTo(batteryLvl)
//            assertThat(state.availableStorage).isEqualTo(availStorage)
//            assertThat(state.totalStorage).isEqualTo(totalStorage)
//            assertThat(state.isAnyNotifierConfigured).isFalse()
//            assertThat(state.latestAlertCheckLog).isEqualTo(sampleLog)
//            assertThat(state.workerStatus).isEqualTo(WorkerStatus.WORKING) // RUNNING maps to WORKING
//            assertThat(state.showEducationSheet).isTrue() // Should show if not previously shown AND no notifier
//
//            coVerify { analytics.logScreenView(AlertsListScreen::class) } // Impression effect
//            coVerify { appPreferencesDataStore.isEducationDialogShown() }
//        }
//    }
//
//    @Test
//    fun `initial state - data loads, notifier configured, education not shown -> showEducationSheet is false`() = runTest {
//        coEvery { appPreferencesDataStore.isEducationDialogShown() } returns flowOf(false) // Education sheet NOT shown before
//
//        // At least one notifier configured
//        val notifier1 = mockk<NotificationSender>()
//        every { notifier1.hasValidConfig() } returns true // Configured
//        mockNotifiers = setOf(notifier1)
//
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            val state = awaitItem()
//            assertThat(state.isAnyNotifierConfigured).isTrue()
//            assertThat(state.showEducationSheet).isFalse() // Should NOT show if notifier is configured
//            coVerify { analytics.logScreenView(AlertsListScreen::class) }
//        }
//    }
//
//    @Test
//    fun `initial state - data loads, no notifier, education previously shown -> showEducationSheet is false`() = runTest {
//        coEvery { appPreferencesDataStore.isEducationDialogShown() } returns flowOf(true) // Education sheet SHOWN before
//
//        // No notifiers configured
//        val notifier1 = mockk<NotificationSender>()
//        every { notifier1.hasValidConfig() } returns false
//        mockNotifiers = setOf(notifier1)
//
//
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            val state = awaitItem()
//            assertThat(state.isAnyNotifierConfigured).isFalse()
//            assertThat(state.showEducationSheet).isFalse() // Should NOT show if previously shown
//            coVerify { analytics.logScreenView(AlertsListScreen::class) }
//        }
//    }
//
//
//    @Test
//    fun `event DeleteNotification calls repository delete`() = runTest {
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            val state = awaitItem() // consume initial state
//            state.eventSink(AlertsListScreenEvent.DeleteNotification(sampleAlert1))
//            coVerify { remoteAlertRepository.deleteRemoteAlert(sampleAlert1) }
//        }
//    }
//
//    @Test
//    fun `event AddNotification navigates to AddNewRemoteAlertScreen`() = runTest {
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            val state = awaitItem()
//            state.eventSink(AlertsListScreenEvent.AddNotification)
//            verify { navigator.goTo(AddNewRemoteAlertScreen) }
//        }
//    }
//
//    @Test
//    fun `event AddNotificationDestination navigates to NotificationMediumListScreen`() = runTest {
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            val state = awaitItem()
//            state.eventSink(AlertsListScreenEvent.AddNotificationDestination)
//            verify { navigator.goTo(NotificationMediumListScreen) }
//        }
//    }
//
//    @Test
//    fun `event NavigateToAbout navigates to AboutAppScreen`() = runTest {
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            val state = awaitItem()
//            state.eventSink(AlertsListScreenEvent.NavigateToAbout)
//            verify { navigator.goTo(AboutAppScreen) }
//        }
//    }
//
//    @Test
//    fun `event DismissEducationSheet updates state, logs analytics, and marks as shown`() = runTest {
//        // Ensure sheet is shown initially for this test
//        coEvery { appPreferencesDataStore.isEducationDialogShown() } returns flowOf(false)
//        val notifier = mockk<NotificationSender>()
//        every { notifier.hasValidConfig() } returns false
//        mockNotifiers = setOf(notifier)
//
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            var state = awaitItem()
//            assertThat(state.showEducationSheet).isTrue() // Verify pre-condition
//
//            state.eventSink(AlertsListScreenEvent.DismissEducationSheet)
//            state = awaitItem() // Recomposition after event
//
//            assertThat(state.showEducationSheet).isFalse()
//            coVerify { analytics.logViewTutorial(isComplete = true) }
//            coVerify { appPreferencesDataStore.markEducationDialogShown() }
//        }
//    }
//
//    @Test
//    fun `event ShowEducationSheet updates state, logs analytics, and marks as shown`() = runTest {
//        // Ensure sheet is NOT shown initially for this test (e.g., already shown or notifier configured)
//        coEvery { appPreferencesDataStore.isEducationDialogShown() } returns flowOf(true) // Already shown
//        val notifier = mockk<NotificationSender>()
//        every { notifier.hasValidConfig() } returns true // Notifier configured
//        mockNotifiers = setOf(notifier)
//
//
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            var state = awaitItem()
//            assertThat(state.showEducationSheet).isFalse() // Verify pre-condition
//
//            state.eventSink(AlertsListScreenEvent.ShowEducationSheet)
//            state = awaitItem() // Recomposition after event
//
//            assertThat(state.showEducationSheet).isTrue()
//            coVerify { analytics.logViewTutorial(isComplete = false) }
//            coVerify { appPreferencesDataStore.markEducationDialogShown() } // As per current implementation
//        }
//    }
//
//    @Test
//    fun `event ViewAllLogs navigates to AlertCheckLogViewerScreen`() = runTest {
//        moleculeFlow(RecompositionClock.Immediate) {
//            createPresenter().present()
//        }.test {
//            val state = awaitItem()
//            state.eventSink(AlertsListScreenEvent.ViewAllLogs)
//            verify { navigator.goTo(AlertCheckLogViewerScreen) }
//        }
//    }
//
//    // Tests for workerStatus mapping
//    @Test
//    fun `workerStatus mapping - ENQUEUED results in WORKING`() = runTest {
//        val workInfoList = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.ENQUEUED, mutableMapOf(), mutableListOf(), 0))
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns flowOf(workInfoList)
//        moleculeFlow(RecompositionClock.Immediate) { createPresenter().present() }.test {
//            assertThat(awaitItem().workerStatus).isEqualTo(WorkerStatus.WORKING)
//        }
//    }
//
//    @Test
//    fun `workerStatus mapping - RUNNING results in WORKING`() = runTest {
//        val workInfoList = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.RUNNING, mutableMapOf(), mutableListOf(), 0))
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns flowOf(workInfoList)
//        moleculeFlow(RecompositionClock.Immediate) { createPresenter().present() }.test {
//            assertThat(awaitItem().workerStatus).isEqualTo(WorkerStatus.WORKING)
//        }
//    }
//
//    @Test
//    fun `workerStatus mapping - SUCCEEDED results in IDLE`() = runTest {
//        val workInfoList = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.SUCCEEDED, mutableMapOf(), mutableListOf(), 0))
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns flowOf(workInfoList)
//        moleculeFlow(RecompositionClock.Immediate) { createPresenter().present() }.test {
//            assertThat(awaitItem().workerStatus).isEqualTo(WorkerStatus.IDLE)
//        }
//    }
//
//    @Test
//    fun `workerStatus mapping - FAILED results in ERROR`() = runTest {
//        val workInfoList = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.FAILED, mutableMapOf(), mutableListOf(), 0))
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns flowOf(workInfoList)
//        moleculeFlow(RecompositionClock.Immediate) { createPresenter().present() }.test {
//            assertThat(awaitItem().workerStatus).isEqualTo(WorkerStatus.ERROR)
//        }
//    }
//
//    @Test
//    fun `workerStatus mapping - BLOCKED results in WORKING`() = runTest { // As per presenter logic
//        val workInfoList = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.BLOCKED, mutableMapOf(), mutableListOf(), 0))
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns flowOf(workInfoList)
//        moleculeFlow(RecompositionClock.Immediate) { createPresenter().present() }.test {
//            assertThat(awaitItem().workerStatus).isEqualTo(WorkerStatus.WORKING)
//        }
//    }
//
//    @Test
//    fun `workerStatus mapping - CANCELLED results in IDLE`() = runTest { // As per presenter logic
//        val workInfoList = listOf(WorkInfo(UUID.randomUUID(), WorkInfo.State.CANCELLED, mutableMapOf(), mutableListOf(), 0))
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns flowOf(workInfoList)
//        moleculeFlow(RecompositionClock.Immediate) { createPresenter().present() }.test {
//            assertThat(awaitItem().workerStatus).isEqualTo(WorkerStatus.IDLE)
//        }
//    }
//
//    @Test
//    fun `workerStatus mapping - empty WorkInfo list results in UNKNOWN`() = runTest {
//        every { mockWorkManager.getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID) } returns flowOf(emptyList())
//        moleculeFlow(RecompositionClock.Immediate) { createPresenter().present() }.test {
//            assertThat(awaitItem().workerStatus).isEqualTo(WorkerStatus.UNKNOWN)
//        }
//    }
//
//    // Impression effect (analytics.logScreenView) is verified in the initial state tests.
//}
