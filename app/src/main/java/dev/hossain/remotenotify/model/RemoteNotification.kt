package dev.hossain.remotenotify.model

import dev.hossain.remotenotify.db.NotificationEntity
import dev.hossain.remotenotify.db.NotificationType

const val BATTERY_PERCENTAGE_NONE = -1
const val STORAGE_MIN_SPACE_GB_NONE = -1

sealed interface RemoteNotification {
    data class BatteryNotification(
        val batteryPercentage: Int,
    ) : RemoteNotification

    data class StorageNotification(
        val storageMinSpaceGb: Int,
    ) : RemoteNotification
}

fun RemoteNotification.toNotificationEntity(): NotificationEntity =
    when (this) {
        is RemoteNotification.BatteryNotification ->
            NotificationEntity(
                type = NotificationType.BATTERY,
                batteryPercentage = batteryPercentage,
                storageMinSpaceGb = 0,
            )
        is RemoteNotification.StorageNotification ->
            NotificationEntity(
                type = NotificationType.STORAGE,
                storageMinSpaceGb = storageMinSpaceGb,
                batteryPercentage = 0,
            )
    }

fun NotificationEntity.toRemoteNotification(): RemoteNotification =
    when (type) {
        NotificationType.BATTERY -> RemoteNotification.BatteryNotification(batteryPercentage)
        NotificationType.STORAGE -> RemoteNotification.StorageNotification(storageMinSpaceGb)
    }
