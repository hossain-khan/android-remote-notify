package dev.hossain.remotenotify.data

import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.DeviceAlert
import dev.hossain.remotenotify.model.DeviceAlert.FormatType
import dev.hossain.remotenotify.model.RemoteAlert
import javax.inject.Inject

@SingleIn(AppScope::class)
class AlertFormatter
    @Inject
    constructor() {
        fun format(
            remoteAlert: RemoteAlert,
            formatType: FormatType = FormatType.TEXT,
        ): String =
            when (remoteAlert) {
                is RemoteAlert.BatteryAlert -> {
                    val deviceAlert =
                        DeviceAlert(
                            alertType = AlertType.BATTERY,
                            batteryLevel = remoteAlert.batteryPercentage,
                        )
                    formatDeviceAlert(deviceAlert, formatType)
                }
                is RemoteAlert.StorageAlert -> {
                    val deviceAlert =
                        DeviceAlert(
                            alertType = AlertType.STORAGE,
                            availableStorageGb = remoteAlert.storageMinSpaceGb.toDouble(),
                        )
                    formatDeviceAlert(deviceAlert, formatType)
                }
                is RemoteAlert.PluginAlert -> {
                    formatPluginAlert(remoteAlert, formatType)
                }
            }

        private fun formatDeviceAlert(
            deviceAlert: DeviceAlert,
            formatType: FormatType,
        ): String =
            when (formatType) {
                FormatType.EXTENDED_TEXT -> deviceAlert.toExtendedText()
                FormatType.HTML -> deviceAlert.toHtml()
                FormatType.JSON -> deviceAlert.toJson()
                FormatType.TEXT -> deviceAlert.toText()
            }

        private fun formatPluginAlert(
            pluginAlert: RemoteAlert.PluginAlert,
            formatType: FormatType,
        ): String =
            when (formatType) {
                FormatType.EXTENDED_TEXT -> formatPluginAlertExtended(pluginAlert)
                FormatType.HTML -> formatPluginAlertHtml(pluginAlert)
                FormatType.JSON -> formatPluginAlertJson(pluginAlert)
                FormatType.TEXT -> formatPluginAlertText(pluginAlert)
            }

        private fun formatPluginAlertText(alert: RemoteAlert.PluginAlert): String =
            "${alert.title}\n\n${alert.message}\n\nFrom: ${alert.sourceAppName}"

        private fun formatPluginAlertExtended(alert: RemoteAlert.PluginAlert): String =
            """
            📱 Plugin Notification from ${alert.sourceAppName}
            
            📋 Title: ${alert.title}
            💬 Message: ${alert.message}
            
            📦 Source Package: ${alert.sourcePackage}
            ⏰ Priority: ${alert.priority}
            🆔 Request ID: ${alert.requestId}
            📅 Timestamp: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
            """.trimIndent()

        private fun formatPluginAlertHtml(alert: RemoteAlert.PluginAlert): String =
            """
                <html>
                <body>
                <h2>📱 Plugin Notification</h2>
                <p><strong>From:</strong> ${alert.sourceAppName}</p>
                <hr>
                <h3>${alert.title}</h3>
                <p>${alert.message}</p>
                <hr>
                <p><small>
                <strong>Source Package:</strong> ${alert.sourcePackage}<br>
                <strong>Priority:</strong> ${alert.priority}<br>
                <strong>Request ID:</strong> ${alert.requestId}<br>
                <strong>Timestamp:</strong> ${java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault(),
            ).format(java.util.Date())}
                </small></p>
                </body>
                </html>
            """.trimIndent()

        private fun formatPluginAlertJson(alert: RemoteAlert.PluginAlert): String =
            """
                {
                    "type": "plugin_notification",
                    "title": "${alert.title}",
                    "message": "${alert.message}",
                    "source_app_name": "${alert.sourceAppName}",
                    "source_package": "${alert.sourcePackage}",
                    "priority": "${alert.priority}",
                    "request_id": "${alert.requestId}",
                    "timestamp": "${java.text.SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                java.util.Locale.getDefault(),
            ).format(java.util.Date())}"
                }
            """.trimIndent()
    }
