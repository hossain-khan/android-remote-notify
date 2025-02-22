package dev.hossain.remotenotify.db

import androidx.room.Embedded
import androidx.room.Relation

data class AlertLogWithConfig(
    @Embedded val log: AlertCheckLogEntity,
    @Relation(
        parentColumn = "alert_config_id",
        entityColumn = "id",
    )
    val config: AlertConfigEntity,
)
