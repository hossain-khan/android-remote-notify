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

    /**
     * Retrieves an alert configuration by its unique ID.
     *
     * @param alertId The unique identifier of the alert.
     * @return The alert configuration if found, null otherwise.
     */
    @Query("SELECT * FROM alert_config WHERE id = :alertId")
    suspend fun getById(alertId: Long): AlertConfigEntity?

    @Insert
    suspend fun insert(notification: AlertConfigEntity)

    /**
     * Updates an existing alert configuration.
     *
     * @param notification The alert configuration to update.
     * @return The number of rows updated (0 if no rows matched the primary key).
     */
    @Update
    suspend fun update(notification: AlertConfigEntity): Int

    @Delete
    suspend fun delete(notification: AlertConfigEntity)

    @Transaction
    @Query("SELECT * FROM alert_config WHERE id = :alertConfigId")
    fun getAlertWithLogs(alertConfigId: Long): Flow<AlertWithLogs>
}
