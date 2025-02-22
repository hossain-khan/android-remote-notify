package dev.hossain.remotenotify.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertCheckLogDao {
    @Insert
    suspend fun insert(log: AlertCheckLogEntity)

    @Query(
        """
        SELECT * FROM alert_check_log
        ORDER BY checked_at DESC
    """,
    )
    fun getAllCheckLogs(): Flow<List<AlertCheckLogEntity>>

    @Query(
        """
        SELECT * FROM alert_check_log 
        WHERE alert_config_id = :alertConfigId 
        ORDER BY checked_at DESC 
        LIMIT 1
    """,
    )
    fun getLatestCheckForAlert(alertConfigId: Long): Flow<AlertCheckLogEntity?>

    @Query(
        """
        SELECT * FROM alert_check_log 
        WHERE alert_config_id = :alertConfigId 
        ORDER BY checked_at DESC
    """,
    )
    fun getCheckHistoryForAlert(alertConfigId: Long): Flow<List<AlertCheckLogEntity>>

    @Query("DELETE FROM alert_check_log WHERE alert_config_id = :alertConfigId")
    suspend fun deleteLogsForAlert(alertConfigId: Long)

    @Query(
        """
        SELECT * FROM alert_check_log 
        WHERE is_alert_triggered = 1 
        ORDER BY checked_at DESC
    """,
    )
    fun getTriggeredAlerts(): Flow<List<AlertCheckLogEntity>>

    @Query(
        """
        SELECT * FROM alert_check_log 
        ORDER BY checked_at DESC 
        LIMIT 1
    """,
    )
    fun getLatestCheckLog(): Flow<AlertCheckLogEntity?>

    @Transaction
    @Query(
        """
        SELECT * FROM alert_check_log
        ORDER BY checked_at DESC
    """,
    )
    fun getAllLogsWithConfig(): Flow<List<AlertLogWithConfig>>
}
