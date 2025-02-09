package dev.hossain.remotenotify.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications")
    fun getAll(): List<NotificationEntity>

    @Insert
    fun insert(notification: NotificationEntity)

    @Delete
    fun delete(notification: NotificationEntity)
}
