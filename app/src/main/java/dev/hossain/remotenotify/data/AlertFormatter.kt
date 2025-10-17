package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.DeviceAlert
import dev.hossain.remotenotify.model.DeviceAlert.FormatType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
class AlertFormatter
    constructor() {
        /**
         * Formats a [RemoteAlert] into a human-readable string representation.
         *
         * Important: This formatter ensures that the current value (battery level or storage)
         * and the threshold are ALWAYS correctly mapped when both are present:
         * - For storage: availableStorageGb (current) vs storageMinSpaceGb (threshold)
         * - For battery: currentBatteryLevel (current) vs batteryPercentage (threshold)
         *
         * When the current value is null (backward compatibility), only the threshold is shown.
         *
         * @param remoteAlert The alert to format
         * @param formatType The desired output format
         * @return Formatted string representation of the alert
         */
        fun format(
            remoteAlert: RemoteAlert,
            formatType: FormatType = FormatType.TEXT,
        ): String {
            val deviceAlert =
                when (remoteAlert) {
                    is RemoteAlert.BatteryAlert ->
                        DeviceAlert(
                            alertType = AlertType.BATTERY,
                            // Use current battery level if available, otherwise fall back to threshold for backward compatibility
                            batteryLevel = remoteAlert.currentBatteryLevel ?: remoteAlert.batteryPercentage,
                            // Only show threshold label when current value is present
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
                            // Use current storage if available, otherwise fall back to threshold for backward compatibility
                            availableStorageGb = remoteAlert.currentStorageGb ?: remoteAlert.storageMinSpaceGb.toDouble(),
                            // Only show threshold label when current value is present
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
