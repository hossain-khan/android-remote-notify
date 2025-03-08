package dev.hossain.remotenotify.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

    private lateinit var worker: ObserveDeviceHealthWorker
    private lateinit var context: Context

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        context = ApplicationProvider.getApplicationContext()

        // Configure default behavior for notification sender
        coEvery { notificationSender.hasValidConfig() } returns true
        every { notificationSender.notifierType } returns NotifierType.EMAIL
        coEvery { notificationSender.sendNotification(any()) } returns true

        // Add this to suppress errors from worker params
        every { workerParameters.runAttemptCount } returns 0
        every { workerParameters.taskExecutor } returns mockk()
        every { workerParameters.id } returns java.util.UUID.randomUUID()
        every { workerParameters.tags } returns setOf()

        // Create worker with mocked dependencies
        worker =
            ObserveDeviceHealthWorker(
                context = context,
                workerParams = workerParameters,
                batteryMonitor = batteryMonitor,
                storageMonitor = storageMonitor,
                repository = repository,
                notifiers = setOf(notificationSender),
            )
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
}
