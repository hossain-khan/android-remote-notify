package dev.hossain.remotenotify.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType

@Entity(
    tableName = "alert_check_log",
    foreignKeys = [
        ForeignKey(
            entity = AlertConfigEntity::class,
            parentColumns = ["id"],
            childColumns = ["alert_config_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("alert_config_id")],
)
data class AlertCheckLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "alert_config_id") val alertConfigId: Long,
    @ColumnInfo(name = "checked_at") val checkedAt: Long = System.currentTimeMillis(),
    /**
     * Battery % or Storage GB
     */
    @ColumnInfo(name = "alert_state_value") val alertStateValue: Int,
    @ColumnInfo(name = "is_alert_triggered") val alertTriggered: Boolean,
    @ColumnInfo(name = "alert_type") val alertType: AlertType,
    /**
     * The notifier that was used to send the alert.
     * This is nullable because the alert might be triggered by threshold set (e.g. battery level)
     */
    @ColumnInfo(name = "notifier_type") val notifierType: NotifierType?,
)
