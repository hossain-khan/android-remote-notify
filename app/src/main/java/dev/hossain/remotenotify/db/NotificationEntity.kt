package dev.hossain.remotenotify.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.hossain.remotenotify.model.BATTERY_PERCENTAGE_NONE
import dev.hossain.remotenotify.model.STORAGE_MIN_SPACE_GB_NONE

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: NotificationType,
    @ColumnInfo(name = "battery_percentage", defaultValue = BATTERY_PERCENTAGE_NONE.toString())
    val batteryPercentage: Int,
    @ColumnInfo(name = "storage_min_space_gb", defaultValue = STORAGE_MIN_SPACE_GB_NONE.toString())
    val storageMinSpaceGb: Int,
)

enum class NotificationType {
    BATTERY,
    STORAGE,
}
