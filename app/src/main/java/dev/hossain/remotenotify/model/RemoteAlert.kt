package dev.hossain.remotenotify.model

import dev.hossain.remotenotify.db.AlertConfigEntity

const val BATTERY_PERCENTAGE_NONE = -1
const val STORAGE_MIN_SPACE_GB_NONE = -1

sealed interface RemoteAlert {
    val alertId: Long

    data class BatteryAlert(
        override val alertId: Long = 0,
        val batteryPercentage: Int,
    ) : RemoteAlert

    data class StorageAlert(
        override val alertId: Long = 0,
        val storageMinSpaceGb: Int,
    ) : RemoteAlert
}

internal fun RemoteAlert.toAlertConfigEntity(): AlertConfigEntity =
    when (this) {
        is RemoteAlert.BatteryAlert ->
            AlertConfigEntity(
                id = alertId,
                batteryPercentage = batteryPercentage,
                type = AlertType.BATTERY,
                storageMinSpaceGb = 0,
            )
        is RemoteAlert.StorageAlert ->
            AlertConfigEntity(
                id = alertId,
                storageMinSpaceGb = storageMinSpaceGb,
                type = AlertType.STORAGE,
                batteryPercentage = 0,
            )
    }

internal fun AlertConfigEntity.toRemoteAlert(): RemoteAlert =
    when (type) {
        AlertType.BATTERY -> RemoteAlert.BatteryAlert(id, batteryPercentage)
        AlertType.STORAGE -> RemoteAlert.StorageAlert(id, storageMinSpaceGb)
    }

/**
 * The alert type display name based on [RemoteAlert].
 */
internal fun RemoteAlert.toTypeDisplayName(): String =
    when (this) {
        is RemoteAlert.BatteryAlert -> "Battery"
        is RemoteAlert.StorageAlert -> "Storage"
    }
