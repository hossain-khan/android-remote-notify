package dev.hossain.remotenotify.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: NotificationType,
    val threshold: Int,
)

enum class NotificationType {
    BATTERY,
    STORAGE,
}
