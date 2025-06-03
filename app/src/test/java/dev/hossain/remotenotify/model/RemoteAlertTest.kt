package dev.hossain.remotenotify.model

import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.db.AlertConfigEntity
import org.junit.Test

class RemoteAlertTest {
    @Test
    fun `given battery alert, when toAlertConfigEntity, then returns correct entity`() {
        val batteryAlert = RemoteAlert.BatteryAlert(alertId = 1L, batteryPercentage = 20)
        val entity = batteryAlert.toAlertConfigEntity()

        assertThat(entity.id).isEqualTo(1L)
        assertThat(entity.batteryPercentage).isEqualTo(20)
        assertThat(entity.type).isEqualTo(AlertType.BATTERY)
        assertThat(entity.storageMinSpaceGb).isEqualTo(0)
    }

    @Test
    fun `given storage alert, when toAlertConfigEntity, then returns correct entity`() {
        val storageAlert = RemoteAlert.StorageAlert(alertId = 2L, storageMinSpaceGb = 5)
        val entity = storageAlert.toAlertConfigEntity()

        assertThat(entity.id).isEqualTo(2L)
        assertThat(entity.storageMinSpaceGb).isEqualTo(5)
        assertThat(entity.type).isEqualTo(AlertType.STORAGE)
        assertThat(entity.batteryPercentage).isEqualTo(0)
    }

    @Test
    fun `given battery alert, when toAlertType, then returns BATTERY`() {
        val batteryAlert = RemoteAlert.BatteryAlert(batteryPercentage = 20)
        val alertType = batteryAlert.toAlertType()

        assertThat(alertType).isEqualTo(AlertType.BATTERY)
    }

    @Test
    fun `given storage alert, when toAlertType, then returns STORAGE`() {
        val storageAlert = RemoteAlert.StorageAlert(storageMinSpaceGb = 5)
        val alertType = storageAlert.toAlertType()

        assertThat(alertType).isEqualTo(AlertType.STORAGE)
    }

    @Test
    fun `given battery alert, when toTypeDisplayName, then returns Battery`() {
        val batteryAlert = RemoteAlert.BatteryAlert(batteryPercentage = 20)
        val displayName = batteryAlert.toTypeDisplayName()

        assertThat(displayName).isEqualTo("Battery")
    }

    @Test
    fun `given storage alert, when toTypeDisplayName, then returns Storage`() {
        val storageAlert = RemoteAlert.StorageAlert(storageMinSpaceGb = 5)
        val displayName = storageAlert.toTypeDisplayName()

        assertThat(displayName).isEqualTo("Storage")
    }

    @Test
    fun `given battery alert config entity, when toRemoteAlert, then returns correct BatteryAlert`() {
        val entity =
            AlertConfigEntity(
                id = 1L,
                batteryPercentage = 20,
                type = AlertType.BATTERY,
                storageMinSpaceGb = 0,
            )
        val remoteAlert = entity.toRemoteAlert()

        assertThat(remoteAlert).isInstanceOf(RemoteAlert.BatteryAlert::class.java)
        val batteryAlert = remoteAlert as RemoteAlert.BatteryAlert
        assertThat(batteryAlert.alertId).isEqualTo(1L)
        assertThat(batteryAlert.batteryPercentage).isEqualTo(20)
    }

    @Test
    fun `given storage alert config entity, when toRemoteAlert, then returns correct StorageAlert`() {
        val entity =
            AlertConfigEntity(
                id = 2L,
                storageMinSpaceGb = 5,
                type = AlertType.STORAGE,
                batteryPercentage = 0,
            )
        val remoteAlert = entity.toRemoteAlert()

        assertThat(remoteAlert).isInstanceOf(RemoteAlert.StorageAlert::class.java)
        val storageAlert = remoteAlert as RemoteAlert.StorageAlert
        assertThat(storageAlert.alertId).isEqualTo(2L)
        assertThat(storageAlert.storageMinSpaceGb).isEqualTo(5)
    }
}
