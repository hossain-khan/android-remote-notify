package dev.hossain.remotenotify.ui.addalert

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.slack.circuit.runtime.Navigator
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.utils.BatteryOptimizationHelper
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication

/**
 * Tests for [AddNewRemoteAlertPresenter].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AddNewRemoteAlertPresenterTest {
    private lateinit var navigator: Navigator
    private lateinit var remoteAlertRepository: RemoteAlertRepository
    private lateinit var storageMonitor: StorageMonitor
    private lateinit var appPreferencesDataStore: AppPreferencesDataStore
    private lateinit var analytics: Analytics
    private lateinit var mockContext: Application
    private lateinit var mockPowerManager: PowerManager

    private lateinit var lifecycleOwner: LifecycleOwner
    private lateinit var lifecycleRegistry: LifecycleRegistry


    @Before
    fun setUp() {
        navigator = mockk(relaxed = true)
        remoteAlertRepository = mockk(relaxed = true)
        storageMonitor = mockk()
        appPreferencesDataStore = mockk(relaxed = true) // relaxed for setHideBatteryOptReminder
        analytics = mockk(relaxed = true)
        mockContext = mockk<Application>(relaxed = true)
        mockPowerManager = mockk(relaxed = true)

        // Mock static LocalContext.current to return our mockContext
        // This is needed because the presenter uses LocalContext.current
        mockkStatic(LocalContext::class)
        //every { LocalContext.current } returns mockContext

        // Mock Android's Settings class for intent actions
        mockkStatic(Settings::class)
        // Mock application package name for ACTION_APPLICATION_DETAILS_SETTINGS
        every { mockContext.packageName } returns "dev.hossain.remotenotify"
        every { mockContext.applicationContext } returns mockContext


        // Mock BatteryOptimizationHelper
        mockkStatic(BatteryOptimizationHelper::class)

        // Mock PowerManager service
        every { mockContext.getSystemService(Context.POWER_SERVICE) } returns mockPowerManager

        // Default mock for appPreferencesDataStore.hideBatteryOptReminder
        coEvery { appPreferencesDataStore.hideBatteryOptReminder } returns flowOf(false)

        // Setup lifecycle for DisposableEffect testing
        lifecycleOwner = mockk()
        lifecycleRegistry = LifecycleRegistry(lifecycleOwner)
        every { lifecycleOwner.lifecycle } returns lifecycleRegistry
    }

    @After
    fun tearDown() {
        unmockkAll() // Unmock all static and regular mocks
    }

    private fun createPresenter(testLifecycleOwner: LifecycleOwner = lifecycleOwner): @Composable () -> AddNewRemoteAlertScreen.State {
        return {
            AddNewRemoteAlertPresenter(
                context = mockContext,
                navigator = navigator,
                remoteAlertRepository = remoteAlertRepository,
                storageMonitor = storageMonitor,
                appPreferencesDataStore = appPreferencesDataStore,
                analytics = analytics,
            ).present()
        }
    }

    @Test
    fun `initial state is correctly set when battery is NOT optimized`() = runTest {
        val availableStorageGb = 25L
        coEvery { storageMonitor.getAvailableStorageInGB() } returns availableStorageGb
        coEvery { appPreferencesDataStore.hideBatteryOptReminder } returns flowOf(false)
        every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns false

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state: AddNewRemoteAlertScreen.State = awaitItem()
            assertThat(state.showBatteryOptSheet).isFalse()
            assertThat(state.isBatteryOptimized).isFalse()
            assertThat(state.selectedAlertType).isEqualTo(AlertType.BATTERY)
            assertThat(state.threshold).isEqualTo(10) // Default for battery
            assertThat(state.availableStorage).isEqualTo(availableStorageGb)
            assertThat(state.storageSliderMax).isEqualTo(availableStorageGb - 1)
            assertThat(state.hideBatteryOptReminder).isFalse()

            coVerify { storageMonitor.getAvailableStorageInGB() }
            coVerify { appPreferencesDataStore.hideBatteryOptReminder }
            verify { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) }
        }
    }

    @Test
    fun `initial state is correctly set when battery IS optimized`() = runTest {
        val availableStorageGb = 30L
        coEvery { storageMonitor.getAvailableStorageInGB() } returns availableStorageGb
        coEvery { appPreferencesDataStore.hideBatteryOptReminder } returns flowOf(true) // Different value
        every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns true

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state = awaitItem()
            assertThat(state.showBatteryOptSheet).isFalse()
            assertThat(state.isBatteryOptimized).isTrue()
            assertThat(state.selectedAlertType).isEqualTo(AlertType.BATTERY)
            assertThat(state.threshold).isEqualTo(10)
            assertThat(state.availableStorage).isEqualTo(availableStorageGb)
            assertThat(state.storageSliderMax).isEqualTo(availableStorageGb - 1)
            assertThat(state.hideBatteryOptReminder).isTrue()
        }
    }

    @Test
    fun `event SaveNotification for BatteryAlert saves alert, logs analytics, and navigates back`() = runTest {
        val batteryAlert = RemoteAlert.BatteryAlert(alertId = 1L, batteryPercentage = 12)
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100 // Needs to be mocked for initial state

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state: AddNewRemoteAlertScreen.State = awaitItem() // Consume initial state

            state.eventSink(AddNewRemoteAlertScreen.Event.SaveNotification(batteryAlert))

            coVerify { analytics.logAlertAdded(AlertType.BATTERY) }
            coVerify { remoteAlertRepository.saveRemoteAlert(batteryAlert) }
            coVerify { navigator.pop() }
        }
    }

    @Test
    fun `event SaveNotification for StorageAlert saves alert, logs analytics, and navigates back`() = runTest {
        val storageAlert = RemoteAlert.StorageAlert(alertId = 1, storageMinSpaceGb = 10)
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state = awaitItem()

            state.eventSink(AddNewRemoteAlertScreen.Event.SaveNotification(storageAlert))

            coVerify { analytics.logAlertAdded(AlertType.STORAGE) }
            coVerify { remoteAlertRepository.saveRemoteAlert(storageAlert) }
            coVerify { navigator.pop() }
        }
    }

    @Test
    fun `event NavigateBack invokes navigator pop`() = runTest {
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100
        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state = awaitItem()
            state.eventSink(AddNewRemoteAlertScreen.Event.NavigateBack)
            coVerify { navigator.pop() }
        }
    }

    @Test
    fun `event ShowBatteryOptimizationSheet updates state and logs analytics`() = runTest {
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100
        every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns false // Ensure sheet can be shown

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val initialState = awaitItem()
            assertThat(initialState.showBatteryOptSheet).isFalse()

            initialState.eventSink(AddNewRemoteAlertScreen.Event.ShowBatteryOptimizationSheet)

            val updatedState = awaitItem()
            assertThat(updatedState.showBatteryOptSheet).isTrue()
            coVerify { analytics.logOptimizeBatteryInfoShown() }
        }
    }

    @Test
    fun `event DismissBatteryOptimizationSheet updates state`() = runTest {
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100
        every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns false


        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state = awaitItem()
            state.eventSink(AddNewRemoteAlertScreen.Event.ShowBatteryOptimizationSheet) // Show first
            val stateAfterShow = awaitItem()
            assertThat(stateAfterShow.showBatteryOptSheet).isTrue()

            stateAfterShow.eventSink(AddNewRemoteAlertScreen.Event.DismissBatteryOptimizationSheet)
            val stateAfterDismiss = awaitItem()
            assertThat(stateAfterDismiss.showBatteryOptSheet).isFalse()
        }
    }

    @Test
    fun `event OpenBatterySettings logs analytics, updates state, and starts activity`() = runTest {
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100
        every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns false

        val intentSlot = slot<Intent>()
        every { mockContext.startActivity(capture(intentSlot)) } just Runs // Needed for void functions

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state = awaitItem()
            state.eventSink(AddNewRemoteAlertScreen.Event.ShowBatteryOptimizationSheet) // Show first
            awaitItem() // consume state update

            state.eventSink(AddNewRemoteAlertScreen.Event.OpenBatterySettings)

            val updatedState = awaitItem()
            assertThat(updatedState.showBatteryOptSheet).isFalse()

            coVerify { analytics.logOptimizeBatteryGoToSettings() }
            verify { mockContext.startActivity(any()) } // any() because intent creation is complex
            assertThat(intentSlot.captured.action).isEqualTo(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            assertThat(intentSlot.captured.data).isEqualTo(Uri.fromParts("package", mockContext.packageName, null))
        }
    }

    @Test
    fun `event UpdateAlertType to STORAGE adjusts threshold if current threshold is higher than max storage`() = runTest {
        val initialAvailableStorage = 8L // Results in storageSliderMax = 7
        coEvery { storageMonitor.getAvailableStorageInGB() } returns initialAvailableStorage
        every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns true


        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val initialState = awaitItem()
            assertThat(initialState.selectedAlertType).isEqualTo(AlertType.BATTERY)
            // Initial threshold for BATTERY is 10, which is > initialAvailableStorage - 1 (7)
            assertThat(initialState.threshold).isEqualTo(10)

            initialState.eventSink(AddNewRemoteAlertScreen.Event.UpdateAlertType(AlertType.STORAGE))

            val finalState = awaitItem()
            assertThat(finalState.selectedAlertType).isEqualTo(AlertType.STORAGE)
            // Threshold should be adjusted to max available for storage (initialAvailableStorage - 1)
            assertThat(finalState.threshold).isEqualTo(initialAvailableStorage - 1)
        }
    }

    @Test
    fun `event UpdateAlertType to STORAGE keeps threshold if current threshold is within max storage`() = runTest {
        val initialAvailableStorage = 20L // Results in storageSliderMax = 19
        val initialThreshold = 15
        coEvery { storageMonitor.getAvailableStorageInGB() } returns initialAvailableStorage
        every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns true

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            var state = awaitItem()
            // Set initial threshold for BATTERY to be within the new storage max
            state.eventSink(AddNewRemoteAlertScreen.Event.UpdateThreshold(initialThreshold))
            state = awaitItem()
            assertThat(state.threshold).isEqualTo(initialThreshold)

            state.eventSink(AddNewRemoteAlertScreen.Event.UpdateAlertType(AlertType.STORAGE))

            val finalState = awaitItem()
            assertThat(finalState.selectedAlertType).isEqualTo(AlertType.STORAGE)
            assertThat(finalState.threshold).isEqualTo(initialThreshold) // Should remain initialThreshold
        }
    }


    @Test
    fun `event UpdateThreshold updates threshold state`() = runTest {
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100
        val newThreshold = 25

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state = awaitItem()
            state.eventSink(AddNewRemoteAlertScreen.Event.UpdateThreshold(newThreshold))

            val updatedState = awaitItem()
            assertThat(updatedState.threshold).isEqualTo(newThreshold)
        }
    }

    @Test
    fun `event HideBatteryOptimizationReminder logs analytics and updates datastore`() = runTest {
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100
        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            val state = awaitItem()
            state.eventSink(AddNewRemoteAlertScreen.Event.HideBatteryOptimizationReminder)

            // awaitItem() // Recomposition might happen
            val finalState = expectMostRecentItem()


            coVerify { analytics.logOptimizeBatteryIgnore() }
            coVerify { appPreferencesDataStore.setHideBatteryOptReminder(true) }
            // Also verify the sheet is dismissed if it was shown due to this action (though not directly specified, good practice)
            assertThat(finalState.showBatteryOptSheet).isFalse()
        }
    }

    @Test
    fun `impression effect logs screen view`() = runTest {
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100
        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            awaitItem() // Trigger LaunchedEffect for impression
            coVerify { analytics.logScreenView(AddNewRemoteAlertScreen::class) }
        }
    }

    @Test
    fun `hideBatteryOptReminder state updates when datastore flow emits new value`() = runTest {
        val hideReminderFlow = MutableSharedFlow<Boolean>()
        coEvery { appPreferencesDataStore.hideBatteryOptReminder } returns hideReminderFlow
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter().invoke()
        }.test {
            hideReminderFlow.emit(false) // Initial emission
            val initialState = awaitItem()
            assertThat(initialState.hideBatteryOptReminder).isFalse()

            hideReminderFlow.emit(true) // New emission
            val updatedState = awaitItem()
            assertThat(updatedState.hideBatteryOptReminder).isTrue()
        }
    }

    @Test
    fun `isBatteryOptimized state updates on lifecycle RESUME if helper returns different value`() = runTest {
        coEvery { storageMonitor.getAvailableStorageInGB() } returns 100
        // Initial state: not optimized
        every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns false

        // Custom lifecycle owner for this test
        val testLifecycleOwner = mockk<LifecycleOwner>()
        val testLifecycleRegistry = LifecycleRegistry(testLifecycleOwner)
        every { testLifecycleOwner.lifecycle } returns testLifecycleRegistry

        moleculeFlow(RecompositionMode.Immediate) {
            createPresenter(testLifecycleOwner).invoke()
        }.test {
            // Initial state
            testLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            testLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            testLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            val initialState = awaitItem()
            assertThat(initialState.isBatteryOptimized).isFalse()
            verify(exactly = 1) { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) }


            // Simulate app returning to foreground (ON_RESUME) and now battery is optimized
            every { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) } returns true
            testLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE) // Must pause before resume
            testLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            val updatedState = awaitItem() // This should be the state after ON_RESUME
            assertThat(updatedState.isBatteryOptimized).isTrue()
            // Check if helper was called again on resume
            verify(exactly = 2) { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(mockContext) }

            testLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            testLifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }
}
