package dev.hossain.remotenotify.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DeviceAlert(
    val alertType: AlertType,
    val deviceBrand: String = android.os.Build.BRAND,
    val deviceModel: String = android.os.Build.MODEL,
    val androidVersion: String = android.os.Build.VERSION.RELEASE,
    val batteryLevel: Int? = null,
    val availableStorageGb: Double? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    fun format(formatType: FormatType): String =
        when (formatType) {
            FormatType.JSON -> toJson()
            FormatType.TEXT -> toText()
            FormatType.EXTENDED_TEXT -> toExtendedText()
        }

    enum class FormatType {
        JSON,
        TEXT,
        EXTENDED_TEXT,
    }

    internal fun toJson(): String {
        val payload =
            DeviceAlertJsonPayload(
                alertType = alertType,
                deviceModel = deviceName(),
                androidVersion = androidVersion,
                batteryLevel = batteryLevel,
                availableStorageGb = availableStorageGb,
                isoDateTime = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            )
        return payload.toJson()
    }

    internal fun toText(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTimestamp = timestamp.format(formatter)
        val alertMessage =
            when (alertType) {
                AlertType.BATTERY -> "Battery Level: $batteryLevel%"
                AlertType.STORAGE -> "Available Storage: $availableStorageGb GB"
            }

        return """
            ${alertType.name} Alert!
            Device: ${deviceName()} ($androidVersion)
            $alertMessage
            Time: $formattedTimestamp
            """.trimIndent()
    }

    internal fun toExtendedText(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTimestamp = timestamp.format(formatter)
        val alertMessage =
            when (alertType) {
                AlertType.BATTERY -> "Battery Level: $batteryLevel%\nAction: Please check charging status."
                AlertType.STORAGE -> "Available Storage: $availableStorageGb GB\nAction: Consider clearing storage space."
            }
        val emoji =
            when (alertType) {
                AlertType.BATTERY -> "🪫"
                AlertType.STORAGE -> "💾"
            }

        return """
            $emoji ${alertType.name} Alert! $emoji

            Device: ${deviceName()} ($androidVersion)
            $alertMessage
            Time: $formattedTimestamp
            """.trimIndent()
    }

    private fun deviceName(): String =
        "${deviceBrand.replaceFirstChar {
            if (it.isLowerCase()) {
                it.titlecase(
                    Locale.US,
                )
            } else {
                it.toString()
            }
        }} $deviceModel"
}

// Example usage:
fun main() {
    val batteryAlert =
        DeviceAlert(
            alertType = AlertType.BATTERY,
            deviceBrand = "Google",
            deviceModel = "Pixel 7",
            androidVersion = "Android 14",
            batteryLevel = 15,
        )

    val storageAlert =
        DeviceAlert(
            alertType = AlertType.STORAGE,
            deviceBrand = "Samsung",
            deviceModel = "Galaxy S23",
            androidVersion = "Android 13",
            availableStorageGb = 3.2,
        )

    println("Battery Alert (JSON):")
    println(batteryAlert.format(DeviceAlert.FormatType.JSON))

    println("\nStorage Alert (TEXT):")
    println(storageAlert.format(DeviceAlert.FormatType.TEXT))

    println("\nBattery Alert (EXTENDED_TEXT):")
    println(batteryAlert.format(DeviceAlert.FormatType.EXTENDED_TEXT))
}
