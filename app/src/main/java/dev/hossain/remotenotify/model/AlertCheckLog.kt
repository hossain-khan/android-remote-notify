package dev.hossain.remotenotify.model

import dev.hossain.remotenotify.db.AlertCheckLogEntity

data class AlertCheckLog(
    val checkedOn: Long,
    val alertType: AlertType,
    val isAlertSent: Boolean,
    val stateValue: Int,
)

fun AlertCheckLogEntity.toAlertCheckLog(): AlertCheckLog =
    AlertCheckLog(
        checkedOn = checkedAt,
        alertType = alertType,
        isAlertSent = alertTriggered,
        stateValue = alertStateValue,
    )
