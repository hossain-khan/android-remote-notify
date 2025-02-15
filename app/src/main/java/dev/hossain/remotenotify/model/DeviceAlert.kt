package dev.hossain.remotenotify.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class DeviceAlert(
    val alertType: AlertType,
    val deviceModel: String = android.os.Build.MODEL,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val batteryLevel: Int? = null,
    val availableStorageGb: Double? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {

    fun format(formatType: FormatType): String {
        return when (formatType) {
            FormatType.JSON -> toJson()
            FormatType.TEXT -> toText()
            FormatType.EXTENDED_TEXT -> toExtendedText()
        }
    }

    enum class FormatType {
        JSON, TEXT, EXTENDED_TEXT
    }

    private fun toJson(): String {
        val batteryLevelJson = batteryLevel?.let { ",\"batteryLevel\":$it" } ?: ""
        val storageGbJson = availableStorageGb?.let { ",\"availableStorageGb\":$it" } ?: ""
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val formattedTimestamp = timestamp.format(formatter)

        return """
            {
                "alertType": "${alertType.name}",
                "deviceModel": "$deviceModel",
                "androidVersion": "$androidVersion",
                "timestamp": "$formattedTimestamp"$batteryLevelJson$storageGbJson
            }
        """.trimIndent()
    }

    private fun toText(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTimestamp = timestamp.format(formatter)
        val alertMessage = when (alertType) {
            AlertType.BATTERY -> "Battery Level: $batteryLevel%"
            AlertType.STORAGE -> "Available Storage: $availableStorageGb GB"
        }

        return """
            ${alertType.name} Alert!
            Device: $deviceModel ($androidVersion)
            $alertMessage
            Time: $formattedTimestamp
        """.trimIndent()
    }

    private fun toExtendedText(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTimestamp = timestamp.format(formatter)
        val alertMessage = when (alertType) {
            AlertType.BATTERY -> "Battery Level: $batteryLevel%\nAction: Please check charging status."
            AlertType.STORAGE -> "Available Storage: $availableStorageGb GB\nAction: Consider clearing storage space."
        }
        val emoji = when (alertType) {
            AlertType.BATTERY -> "🪫"
            AlertType.STORAGE -> "💾"
        }

        return """
            $emoji ${alertType.name} Alert! $emoji

            Device: $deviceModel ($androidVersion)
            $alertMessage
            Time: $formattedTimestamp
        """.trimIndent()
    }
}

// Example usage:
fun main() {
    val batteryAlert = DeviceAlert(
        alertType = AlertType.BATTERY,
        deviceModel = "Pixel 7",
        androidVersion = "Android 14",
        batteryLevel = 15
    )

    val storageAlert = DeviceAlert(
        alertType = AlertType.STORAGE,
        deviceModel = "Samsung Galaxy S23",
        androidVersion = "Android 13",
        availableStorageGb = 3.2
    )

    println("Battery Alert (JSON):")
    println(batteryAlert.format(DeviceAlert.FormatType.JSON))

    println("\nStorage Alert (TEXT):")
    println(storageAlert.format(DeviceAlert.FormatType.TEXT))

    println("\nBattery Alert (EXTENDED_TEXT):")
    println(batteryAlert.format(DeviceAlert.FormatType.EXTENDED_TEXT))
}