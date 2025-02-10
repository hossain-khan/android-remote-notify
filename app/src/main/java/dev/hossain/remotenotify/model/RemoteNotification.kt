package dev.hossain.remotenotify.model

import dev.hossain.remotenotify.db.NotificationEntity

const val BATTERY_PERCENTAGE_NONE = -1
const val STORAGE_MIN_SPACE_GB_NONE = -1

sealed interface RemoteNotification {
    val alertId: Long

    data class BatteryNotification(
        override val alertId: Long = 0,
        val batteryPercentage: Int,
    ) : RemoteNotification

    data class StorageNotification(
        override val alertId: Long = 0,
        val storageMinSpaceGb: Int,
    ) : RemoteNotification
}

fun RemoteNotification.toNotificationEntity(): NotificationEntity =
    when (this) {
        is RemoteNotification.BatteryNotification ->
            NotificationEntity(
                id = alertId,
                batteryPercentage = batteryPercentage,
                type = NotificationType.BATTERY,
                storageMinSpaceGb = 0,
            )
        is RemoteNotification.StorageNotification ->
            NotificationEntity(
                id = alertId,
                storageMinSpaceGb = storageMinSpaceGb,
                type = NotificationType.STORAGE,
                batteryPercentage = 0,
            )
    }

fun NotificationEntity.toRemoteNotification(): RemoteNotification =
    when (type) {
        NotificationType.BATTERY -> RemoteNotification.BatteryNotification(id, batteryPercentage)
        NotificationType.STORAGE -> RemoteNotification.StorageNotification(id, storageMinSpaceGb)
    }
