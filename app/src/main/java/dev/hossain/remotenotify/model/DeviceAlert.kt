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
            FormatType.EXTENDED_TEXT -> toExtendedText()
            FormatType.HTML -> toHtml()
            FormatType.JSON -> toJson()
            FormatType.TEXT -> toText()
        }

    enum class FormatType {
        EXTENDED_TEXT,
        HTML,
        JSON,
        TEXT,
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
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm:ss")
        val formattedTimestamp = timestamp.format(formatter)
        val alertMessage =
            when (alertType) {
                AlertType.BATTERY -> "Battery Level is at $batteryLevel%"
                AlertType.STORAGE -> "Storage Space Available: $availableStorageGb GB"
            }

        return buildString {
            append("⚠️ ${alertType.name.toTitleCase()} Alert")
            append("\n📱 ${deviceName()}")
            append("\n📍 Android $androidVersion")
            append("\n${getAlertEmoji()} $alertMessage")
            append("\n🕒 $formattedTimestamp")
        }
    }

    internal fun toExtendedText(): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy 'at' HH:mm:ss")
        val formattedTimestamp = timestamp.format(formatter)
        val (message, action) =
            when (alertType) {
                AlertType.BATTERY ->
                    Pair(
                        "Device battery is critically low at $batteryLevel%",
                        "Please connect your device to a charger to prevent shutdown.",
                    )
                AlertType.STORAGE ->
                    Pair(
                        "Available storage space is low ($availableStorageGb GB)",
                        "Consider removing unused apps or media files to free up space.",
                    )
            }

        return buildString {
            append("${getAlertEmoji()} Alert: ${alertType.name.toTitleCase()}\n\n")
            append("📱 Device: ${deviceName()}\n")
            append("📍 System: Android $androidVersion\n")
            append("ℹ️ Status: $message\n")
            append("⚡ Action: $action\n\n")
            append("🕒 Reported on: $formattedTimestamp")
        }
    }

    internal fun toHtml(): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy 'at' HH:mm:ss")
        val formattedTimestamp = timestamp.format(formatter)
        val (message, action) =
            when (alertType) {
                AlertType.BATTERY ->
                    Pair(
                        "Device battery is critically low at $batteryLevel%",
                        "Please connect your device to a charger to prevent shutdown.",
                    )
                AlertType.STORAGE ->
                    Pair(
                        "Available storage space is low ($availableStorageGb GB)",
                        "Consider removing unused apps or media files to free up space.",
                    )
            }

        return """
            <div style="font-family: sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h2 style="color: #d32f2f;">${getAlertEmoji()} ${alertType.name.toTitleCase()} Alert</h2>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 15px 0;">
                    <p style="margin: 5px 0;"><strong>📱 Device:</strong> ${deviceName()}</p>
                    <p style="margin: 5px 0;"><strong>📍 System:</strong> Android $androidVersion</p>
                    <p style="margin: 5px 0;"><strong>ℹ️ Status:</strong> $message</p>
                    <p style="margin: 5px 0;"><strong>⚡ Action Required:</strong> $action</p>
                    <p style="margin: 5px 0; color: #666;"><strong>🕒 Reported on:</strong> $formattedTimestamp</p>
                </div>
                <p style="font-size: 12px; color: #666;">This is an automated alert from your Android device monitoring system.</p>
            </div>
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

    private fun getAlertEmoji() =
        when (alertType) {
            AlertType.BATTERY -> "🪫"
            AlertType.STORAGE -> "💾"
        }

    private fun String.toTitleCase(): String =
        lowercase().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
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

    println("\n----------------------------------------\n")

    println("\nStorage Alert (TEXT):")
    println(storageAlert.format(DeviceAlert.FormatType.TEXT))

    println("\n----------------------------------------\n")

    println("\nBattery Alert (TEXT):")
    println(batteryAlert.format(DeviceAlert.FormatType.TEXT))

    println("\n----------------------------------------\n")

    println("\nBattery Alert (EXTENDED_TEXT):")
    println(batteryAlert.format(DeviceAlert.FormatType.EXTENDED_TEXT))

    println("\n----------------------------------------\n")

    println("\nBattery Alert (HTML):")
    println(batteryAlert.format(DeviceAlert.FormatType.HTML))
}
