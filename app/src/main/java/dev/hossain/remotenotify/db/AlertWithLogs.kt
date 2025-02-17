package dev.hossain.remotenotify.db

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class to represent [AlertConfigEntity] with [AlertCheckLogEntity] as a list.
 * A one to many relationship.
 */
data class AlertWithLogs(
    @Embedded val alert: AlertConfigEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "alert_config_id",
    )
    val logs: List<AlertCheckLogEntity>,
)
