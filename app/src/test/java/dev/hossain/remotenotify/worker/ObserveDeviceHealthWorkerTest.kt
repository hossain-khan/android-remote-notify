package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
class ObserveDeviceHealthWorkerTest {
    @MockK
    private lateinit var workerParameters: WorkerParameters

    @MockK
    private lateinit var batteryMonitor: BatteryMonitor

    @MockK
    private lateinit var storageMonitor: StorageMonitor

    @MockK
    private lateinit var repository: RemoteAlertRepository

    @MockK
    private lateinit var notificationSender: NotificationSender

    @MockK
    private lateinit var analytics: Analytics

    private lateinit var worker: ObserveDeviceHealthWorker
    private lateinit var context: Context

    // No longer necessary now that we are using `Timber.uprootAll()`
    /*companion object { // Use a companion object for static initialization
        private var firebaseInitialized = false

        @JvmStatic // Important for JUnit to recognize the @BeforeClass method
        @BeforeClass
        fun setup() {
            // Avoid `./gradlew :app:testReleaseUnitTest` test failure
            // - java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process
            // dev.hossain.remotenotify. Make sure to call FirebaseApp.initializeApp(Context) first.
            if (!firebaseInitialized) {
                val context = ApplicationProvider.getApplicationContext<Context>()
                FirebaseApp.initializeApp(context)
                firebaseInitialized = true
            }
        }
    }*/

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        // Mock Timber to prevent FirebaseCrashlytics errors
        val timber = mockk<Timber.Tree>(relaxed = true)
        Timber.uprootAll()
        Timber.plant(timber)

        // Configure default behavior for notification sender
        coEvery { notificationSender.hasValidConfig() } returns true
        every { notificationSender.notifierType } returns NotifierType.EMAIL
        coEvery { notificationSender.sendNotification(any()) } returns true

        // Mock analytics methods
        coEvery { analytics.logWorkerJob(any(), any()) } just Runs
        coEvery { analytics.logWorkSuccess() } just Runs
        coEvery { analytics.logWorkFailed(any(), any()) } just Runs
        coEvery { analytics.logAlertSent(any(), any()) } just Runs

        // Add this to suppress errors from worker params
        every { workerParameters.runAttemptCount } returns 0
        every { workerParameters.taskExecutor } returns mockk()
        every { workerParameters.id } returns java.util.UUID.randomUUID()
        every { workerParameters.tags } returns setOf()

        worker =
            ObserveDeviceHealthWorker(
                context = context,
                workerParams = workerParameters,
                batteryMonitor = batteryMonitor,
                storageMonitor = storageMonitor,
                repository = repository,
                notifiers = setOf(notificationSender),
                analytics = analytics,
            )

        // Create a spy for the worker and mock setProgress
        worker = spyk(worker)
        coEvery { worker.setProgress(any()) } just Runs
    }

    @After
    fun tearDown() {
        // Remove all trees to clean up after test
        Timber.uprootAll()
    }

    @Test
    fun `doWork returns success when no alerts exist`() =
        runTest {
            // Given
            coEvery { repository.getAllRemoteAlert() } returns emptyList()
            every { batteryMonitor.getBatteryLevel() } returns 80
            every { storageMonitor.getAvailableStorageInGB() } returns 20L

            // When
            val result = worker.doWork()

            // Then
            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 0) { notificationSender.sendNotification(any()) }
            coVerify(exactly = 0) { repository.insertAlertCheckLog(any(), any(), any(), any(), any()) }
        }

    @Ignore("Breakpoint is not working to debug the test.")
    @Test
    fun `doWork logs battery check but doesn't notify when threshold not met`() =
        runTest {
            // Given
            val batteryAlert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 20,
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(batteryAlert)
            every { batteryMonitor.getBatteryLevel() } returns 50 // Battery level above threshold
            every { storageMonitor.getAvailableStorageInGB() } returns 20L
            coEvery { repository.getLatestCheckForAlert(1L) } returns flowOf(null)

            // When
            val result = worker.doWork()

            // Then
            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 0) { notificationSender.sendNotification(any()) }
            coVerify(exactly = 1) { repository.insertAlertCheckLog(1L, AlertType.BATTERY, 50, false, null) }
        }

    @Test
    fun `doWork sends battery notification when below threshold`() =
        runTest {
            // Given
            val batteryAlert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 20,
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(batteryAlert)
            every { batteryMonitor.getBatteryLevel() } returns 15 // Battery level below threshold
            every { storageMonitor.getAvailableStorageInGB() } returns 20L
            coEvery { repository.getLatestCheckForAlert(1L) } returns flowOf(null)

            // When
            val result = worker.doWork()

            // Then
            assertEquals(ListenableWorker.Result.success(), result)
            coVerify { notificationSender.sendNotification(batteryAlert) }
            coVerify { repository.insertAlertCheckLog(1L, AlertType.BATTERY, 15, true, NotifierType.EMAIL) }
        }

    @Ignore("Breakpoint is not working to debug the test.")
    @Test
    fun `doWork logs storage check but doesn't notify when threshold not met`() =
        runTest {
            // Given
            val storageAlert =
                RemoteAlert.StorageAlert(
                    alertId = 2L,
                    storageMinSpaceGb = 5,
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(storageAlert)
            every { batteryMonitor.getBatteryLevel() } returns 80
            every { storageMonitor.getAvailableStorageInGB() } returns 10L // above threshold
            coEvery { repository.getLatestCheckForAlert(2L) } returns flowOf(null)

            // When
            val result = worker.doWork()

            // Then
            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 0) { notificationSender.sendNotification(any()) }
            coVerify(exactly = 1) { repository.insertAlertCheckLog(2L, AlertType.STORAGE, 10, false, null) }
        }

    @Test
    fun `doWork sends storage notification when below threshold`() =
        runTest {
            // Given
            val storageAlert =
                RemoteAlert.StorageAlert(
                    alertId = 2L,
                    storageMinSpaceGb = 10,
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(storageAlert)
            every { batteryMonitor.getBatteryLevel() } returns 80
            every { storageMonitor.getAvailableStorageInGB() } returns 8L // below threshold
            coEvery { repository.getLatestCheckForAlert(2L) } returns flowOf(null)

            // When
            val result = worker.doWork()

            // Then
            assertEquals(ListenableWorker.Result.success(), result)
            coVerify { notificationSender.sendNotification(storageAlert) }
            coVerify { repository.insertAlertCheckLog(2L, AlertType.STORAGE, 8, true, NotifierType.EMAIL) }
        }

    @Test
    fun `doWork doesn't notify when alert was sent within 24 hours`() =
        runTest {
            // Given
            val batteryAlert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 20,
                )

            val currentTime = System.currentTimeMillis()
            val alertLog =
                AlertCheckLog(
                    checkedOn = currentTime - (20 * 60 * 60 * 1000), // 20 hours ago (less than 24)
                    alertType = AlertType.BATTERY,
                    isAlertSent = true,
                    notifierType = NotifierType.EMAIL,
                    stateValue = 15,
                    configId = 1L,
                    configBatteryPercentage = 20,
                    configStorageMinSpaceGb = 0,
                    configCreatedOn = currentTime - (30 * 24 * 60 * 60 * 1000), // 30 days ago
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(batteryAlert)
            every { batteryMonitor.getBatteryLevel() } returns 15 // Below threshold
            every { storageMonitor.getAvailableStorageInGB() } returns 20L
            coEvery { repository.getLatestCheckForAlert(1L) } returns flowOf(alertLog)

            // When
            val result = worker.doWork()

            // Then
            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 0) { notificationSender.sendNotification(any()) }
            coVerify(exactly = 0) { repository.insertAlertCheckLog(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `doWork notifies when alert was sent more than 24 hours ago`() =
        runTest {
            // Given
            val batteryAlert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 20,
                )

            val currentTime = System.currentTimeMillis()
            val alertLog =
                AlertCheckLog(
                    checkedOn = currentTime - (25 * 60 * 60 * 1000), // 25 hours ago (more than 24)
                    alertType = AlertType.BATTERY,
                    isAlertSent = true,
                    notifierType = NotifierType.EMAIL,
                    stateValue = 15,
                    configId = 1L,
                    configBatteryPercentage = 20,
                    configStorageMinSpaceGb = 0,
                    configCreatedOn = currentTime - (30 * 24 * 60 * 60 * 1000), // 30 days ago
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(batteryAlert)
            every { batteryMonitor.getBatteryLevel() } returns 15 // Below threshold
            every { storageMonitor.getAvailableStorageInGB() } returns 20L
            coEvery { repository.getLatestCheckForAlert(1L) } returns flowOf(alertLog)

            // When
            val result = worker.doWork()

            // Then
            assertEquals(ListenableWorker.Result.success(), result)
            coVerify { notificationSender.sendNotification(batteryAlert) }
            coVerify { repository.insertAlertCheckLog(1L, AlertType.BATTERY, 15, true, NotifierType.EMAIL) }
        }

    @Test
    fun `doWork returns failure when exception occurs`() =
        runTest {
            // Given
            coEvery { repository.getAllRemoteAlert() } throws RuntimeException("Test exception")

            // When
            val result = worker.doWork()

            // Then
            assertEquals(ListenableWorker.Result.failure(), result)
        }

    @Test
    fun `doWork logs worker job started`() =
        runTest {
            // Given
            coEvery { repository.getAllRemoteAlert() } returns emptyList()
            every { batteryMonitor.getBatteryLevel() } returns 80
            every { storageMonitor.getAvailableStorageInGB() } returns 20L

            // When
            worker.doWork()

            // Then
            coVerify { analytics.logWorkerJob(any(), 0L) }
        }

    @Test
    fun `doWork logs work success on successful completion`() =
        runTest {
            // Given
            coEvery { repository.getAllRemoteAlert() } returns emptyList()
            every { batteryMonitor.getBatteryLevel() } returns 80
            every { storageMonitor.getAvailableStorageInGB() } returns 20L

            // When
            worker.doWork()

            // Then
            coVerify { analytics.logWorkSuccess() }
        }

    @Test
    fun `doWork logs work failed when exception occurs`() =
        runTest {
            // Given
            coEvery { repository.getAllRemoteAlert() } throws RuntimeException("Test exception")

            // When
            worker.doWork()

            // Then
            coVerify { analytics.logWorkFailed(null, any()) }
        }

    @Test
    fun `sendNotification logs alert sent when notification successful`() =
        runTest {
            // Given
            val batteryAlert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 20,
                )

            // Create a mock notifier that will return success
            val mockNotifier =
                mockk<NotificationSender> {
                    every { notifierType } returns NotifierType.EMAIL
                    coEvery { hasValidConfig() } returns true
                    coEvery { sendNotification(any()) } returns true // Ensure it returns success
                }

            worker =
                ObserveDeviceHealthWorker(
                    context = context,
                    workerParams = workerParameters,
                    batteryMonitor = batteryMonitor,
                    storageMonitor = storageMonitor,
                    repository = repository,
                    notifiers = setOf(mockNotifier),
                    analytics = analytics,
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(batteryAlert)
            every { batteryMonitor.getBatteryLevel() } returns 15 // Below threshold
            every { storageMonitor.getAvailableStorageInGB() } returns 20L
            coEvery { repository.getLatestCheckForAlert(1L) } returns flowOf(null)

            // Mock the saveAlertCheckLog method since it's referenced in the success path
            coEvery { repository.insertAlertCheckLog(any(), any(), any(), any(), any()) } returns Unit

            // When
            worker.doWork()

            // Then
            coVerify { analytics.logAlertSent(AlertType.BATTERY, NotifierType.EMAIL) }
        }

    @Test
    fun `sendNotification logs work failed when notification fails`() =
        runTest {
            // Given
            val batteryAlert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 20,
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(batteryAlert)
            every { batteryMonitor.getBatteryLevel() } returns 15 // Below threshold
            every { storageMonitor.getAvailableStorageInGB() } returns 20L
            coEvery { repository.getLatestCheckForAlert(1L) } returns flowOf(null)

            // Make the notification fail
            val exception = RuntimeException("Notification failed")
            coEvery { notificationSender.sendNotification(any()) } throws exception

            // When
            worker.doWork()

            // Then
            coVerify { analytics.logWorkFailed(NotifierType.EMAIL, any()) }
        }

    @Test
    fun `doWork logs correct alert count in analytics`() =
        runTest {
            // Given
            val batteryAlert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 20,
                )
            val storageAlert =
                RemoteAlert.StorageAlert(
                    alertId = 2L,
                    storageMinSpaceGb = 10,
                )

            coEvery { repository.getAllRemoteAlert() } returns listOf(batteryAlert, storageAlert)
            every { batteryMonitor.getBatteryLevel() } returns 80
            every { storageMonitor.getAvailableStorageInGB() } returns 20L
            coEvery { repository.getLatestCheckForAlert(any()) } returns flowOf(null)

            // When
            worker.doWork()

            // Then
            coVerify { analytics.logWorkerJob(any(), 2L) }
        }
}
