package dev.hossain.remotenotify.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications")
    suspend fun getAll(): List<NotificationEntity>

    @Query("SELECT * FROM notifications")
    fun getAllFlow(): Flow<List<NotificationEntity>>

    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Delete
    suspend fun delete(notification: NotificationEntity)
}
