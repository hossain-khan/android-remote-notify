package dev.hossain.remotenotify.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertConfigDao {
    @Query("SELECT * FROM alert_config ORDER BY created_on ASC")
    suspend fun getAll(): List<AlertConfigEntity>

    @Query("SELECT * FROM alert_config ORDER BY created_on ASC")
    fun getAllFlow(): Flow<List<AlertConfigEntity>>

    @Query("SELECT * FROM alert_config WHERE id = :alertId")
    suspend fun getById(alertId: Long): AlertConfigEntity?

    @Insert
    suspend fun insert(notification: AlertConfigEntity)

    @Update
    suspend fun update(notification: AlertConfigEntity)

    @Delete
    suspend fun delete(notification: AlertConfigEntity)

    @Transaction
    @Query("SELECT * FROM alert_config WHERE id = :alertConfigId")
    fun getAlertWithLogs(alertConfigId: Long): Flow<AlertWithLogs>
}
