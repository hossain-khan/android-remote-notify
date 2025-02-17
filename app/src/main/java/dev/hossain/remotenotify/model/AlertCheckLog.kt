package dev.hossain.remotenotify.model

import dev.hossain.remotenotify.db.AlertCheckLogEntity

data class AlertCheckLog(
    val checkedOn: Long,
    val alertType: AlertType,
    val isAlertSent: Boolean,
)

fun AlertCheckLogEntity.toAlertCheckLog(): AlertCheckLog =
    AlertCheckLog(
        checkedOn = checkedAt,
        alertType = alertType,
        isAlertSent = alertTriggered,
    )
