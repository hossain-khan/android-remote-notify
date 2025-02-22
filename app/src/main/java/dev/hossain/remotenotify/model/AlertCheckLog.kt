package dev.hossain.remotenotify.model

import dev.hossain.remotenotify.db.AlertCheckLogEntity
import dev.hossain.remotenotify.db.AlertLogWithConfig
import dev.hossain.remotenotify.notifier.NotifierType

data class AlertCheckLog(
    val checkedOn: Long,
    val alertType: AlertType,
    val isAlertSent: Boolean,
    val notifierType: NotifierType?,
    val stateValue: Int,
    val configId: Long,
    val configBatteryPercentage: Int,
    val configStorageMinSpaceGb: Int,
    val configCreatedOn: Long,
)

fun AlertCheckLogEntity.toAlertCheckLog(): AlertCheckLog =
    AlertCheckLog(
        checkedOn = checkedAt,
        alertType = alertType,
        isAlertSent = alertTriggered,
        notifierType = notifierType,
        stateValue = alertStateValue,
        configId = 0,
        configBatteryPercentage = 0,
        configStorageMinSpaceGb = 0,
        configCreatedOn = 0,
    )

fun AlertLogWithConfig.toAlertCheckLog(): AlertCheckLog =
    AlertCheckLog(
        checkedOn = log.checkedAt,
        alertType = log.alertType,
        isAlertSent = log.alertTriggered,
        notifierType = log.notifierType,
        stateValue = log.alertStateValue,
        configId = config.id,
        configBatteryPercentage = config.batteryPercentage,
        configStorageMinSpaceGb = config.storageMinSpaceGb,
        configCreatedOn = config.createdOn,
    )
