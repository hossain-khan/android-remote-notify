package dev.hossain.remotenotify.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertConfigDao {
    @Query("SELECT * FROM alert_config")
    suspend fun getAll(): List<AlertConfigEntity>

    @Query("SELECT * FROM alert_config")
    fun getAllFlow(): Flow<List<AlertConfigEntity>>

    @Insert
    suspend fun insert(notification: AlertConfigEntity)

    @Delete
    suspend fun delete(notification: AlertConfigEntity)
}
