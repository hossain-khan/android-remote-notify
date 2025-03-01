package dev.hossain.remotenotify.data

import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.db.AlertCheckLogDao
import dev.hossain.remotenotify.db.AlertConfigDao
import dev.hossain.remotenotify.db.AlertConfigEntity
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class RemoteAlertRepositoryImplTest {
    // System under test
    private lateinit var repository: RemoteAlertRepositoryImpl

    // Mock dependencies
    private val mockAlertConfigDao = mockk<AlertConfigDao>(relaxed = true)
    private val mockAlertCheckLogDao = mockk<AlertCheckLogDao>(relaxed = true)

    @Before
    fun setup() {
        repository =
            RemoteAlertRepositoryImpl(
                alertConfigDao = mockAlertConfigDao,
                alertCheckLogDao = mockAlertCheckLogDao,
            )
    }

    @Test
    fun `saveRemoteAlert should call dao insert with correct entity`() =
        runTest {
            // Given
            val testAlert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 15,
                )

            // When
            repository.saveRemoteAlert(testAlert)

            // Then
            coVerify {
                mockAlertConfigDao.insert(any<AlertConfigEntity>())
            }
        }

    @Test
    fun `getAllRemoteAlert should return mapped alerts from dao`() =
        runTest {
            // Given
            val testEntityList =
                listOf(
                    AlertConfigEntity(
                        id = 1L,
                        type = AlertType.BATTERY,
                        batteryPercentage = 20,
                        storageMinSpaceGb = 0,
                    ),
                    AlertConfigEntity(
                        id = 2L,
                        type = AlertType.STORAGE,
                        batteryPercentage = 0,
                        storageMinSpaceGb = 5,
                    ),
                )

            coEvery { mockAlertConfigDao.getAll() } returns testEntityList

            // When
            val result = repository.getAllRemoteAlert()

            // Then
            assertThat(result).hasSize(2)

            val batteryAlert = result[0] as RemoteAlert.BatteryAlert
            assertThat(batteryAlert.alertId).isEqualTo(1L)
            assertThat(batteryAlert.batteryPercentage).isEqualTo(20)

            val storageAlert = result[1] as RemoteAlert.StorageAlert
            assertThat(storageAlert.alertId).isEqualTo(2L)
            assertThat(storageAlert.storageMinSpaceGb).isEqualTo(5)
        }
}
