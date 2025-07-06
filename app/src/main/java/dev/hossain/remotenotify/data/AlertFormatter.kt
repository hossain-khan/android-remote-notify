package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.DeviceAlert
import dev.hossain.remotenotify.model.DeviceAlert.FormatType
import dev.hossain.remotenotify.model.RemoteAlert
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.SingleIn

@SingleIn(AppScope::class)
class AlertFormatter
    @Inject
    constructor() {
        fun format(
            remoteAlert: RemoteAlert,
            formatType: FormatType = FormatType.TEXT,
        ): String {
            val deviceAlert =
                when (remoteAlert) {
                    is RemoteAlert.BatteryAlert ->
                        DeviceAlert(
                            alertType = AlertType.BATTERY,
                            batteryLevel = remoteAlert.currentBatteryLevel ?: remoteAlert.batteryPercentage,
                            batteryThresholdPercent =
                                if (remoteAlert.currentBatteryLevel != null) {
                                    remoteAlert.batteryPercentage
                                } else {
                                    null
                                },
                        )
                    is RemoteAlert.StorageAlert ->
                        DeviceAlert(
                            alertType = AlertType.STORAGE,
                            availableStorageGb = remoteAlert.currentStorageGb ?: remoteAlert.storageMinSpaceGb.toDouble(),
                            storageThresholdGb =
                                if (remoteAlert.currentStorageGb !=
                                    null
                                ) {
                                    remoteAlert.storageMinSpaceGb.toDouble()
                                } else {
                                    null
                                },
                        )
                }
            return when (formatType) {
                FormatType.EXTENDED_TEXT -> deviceAlert.toExtendedText()
                FormatType.HTML -> deviceAlert.toHtml()
                FormatType.JSON -> deviceAlert.toJson()
                FormatType.TEXT -> deviceAlert.toText()
            }
        }
    }
