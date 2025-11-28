package dev.hossain.remotenotify.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDateTime

/**
 * Unit tests for [DeviceAlert] class to ensure format methods work correctly.
 */
class DeviceAlertTest {
    private val fixedTimestamp = LocalDateTime.of(2024, 3, 15, 14, 30, 45)

    @Test
    fun `toText formats battery alert with both current level and threshold`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                batteryLevel = 8,
                batteryThresholdPercent = 15,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.TEXT)

        assertThat(result).contains("Battery Alert")
        assertThat(result).contains("Google Pixel 7")
        assertThat(result).contains("Android 14")
        assertThat(result).contains("Current 8%")
        assertThat(result).contains("Threshold: 15%")
        assertThat(result).contains("Mar 15, 2024")
    }

    @Test
    fun `toText formats battery alert with only battery level`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Samsung",
                deviceModel = "Galaxy S23",
                androidVersion = "13",
                batteryLevel = 10,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.TEXT)

        assertThat(result).contains("Battery Level is at 10%")
    }

    @Test
    fun `toText formats battery alert with no battery data`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.TEXT)

        assertThat(result).contains("Battery Level is low")
    }

    @Test
    fun `toText formats storage alert with both current and threshold`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Samsung",
                deviceModel = "Galaxy S23",
                androidVersion = "13",
                availableStorageGb = 4.5,
                storageThresholdGb = 10.0,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.TEXT)

        assertThat(result).contains("Storage Alert")
        assertThat(result).contains("Storage Space Critical")
        assertThat(result).contains("Current 4.5 GB")
        assertThat(result).contains("Threshold: 10.0 GB")
    }

    @Test
    fun `toText formats storage alert with only available storage`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Samsung",
                deviceModel = "Galaxy S23",
                androidVersion = "13",
                availableStorageGb = 8.5,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.TEXT)

        assertThat(result).contains("Storage Space Available: 8.5 GB")
    }

    @Test
    fun `toText formats storage alert with no storage data`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.TEXT)

        assertThat(result).contains("Storage Space is low")
    }

    @Test
    fun `toExtendedText formats battery alert with current level and threshold`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                batteryLevel = 8,
                batteryThresholdPercent = 15,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.EXTENDED_TEXT)

        assertThat(result).contains("Alert: Battery Low")
        assertThat(result).contains("Device: Google Pixel 7")
        assertThat(result).contains("System: Android 14")
        assertThat(result).contains("critically low")
        assertThat(result).contains("Current: 8%")
        assertThat(result).contains("Threshold: 15%")
        assertThat(result).contains("connect your device to a charger")
    }

    @Test
    fun `toExtendedText formats battery alert with only battery level`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                batteryLevel = 5,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.EXTENDED_TEXT)

        assertThat(result).contains("critically low at 5%")
    }

    @Test
    fun `toExtendedText formats battery alert with no battery data`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.EXTENDED_TEXT)

        assertThat(result).contains("Device battery is critically low")
    }

    @Test
    fun `toExtendedText formats storage alert with current and threshold`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Samsung",
                deviceModel = "Galaxy S23",
                androidVersion = "13",
                availableStorageGb = 5.0,
                storageThresholdGb = 10.0,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.EXTENDED_TEXT)

        assertThat(result).contains("Alert: Storage Low")
        assertThat(result).contains("critically low")
        assertThat(result).contains("Current: 5.0 GB")
        assertThat(result).contains("Threshold: 10.0 GB")
        assertThat(result).contains("removing unused apps")
    }

    @Test
    fun `toExtendedText formats storage alert with only available storage`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Samsung",
                deviceModel = "Galaxy S23",
                androidVersion = "13",
                availableStorageGb = 8.5,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.EXTENDED_TEXT)

        assertThat(result).contains("Available storage space is low (8.5 GB)")
    }

    @Test
    fun `toExtendedText formats storage alert with no storage data`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Samsung",
                deviceModel = "Galaxy S23",
                androidVersion = "13",
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.EXTENDED_TEXT)

        assertThat(result).contains("Available storage space is low")
    }

    @Test
    fun `toHtml formats battery alert with proper HTML structure`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                batteryLevel = 8,
                batteryThresholdPercent = 15,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.HTML)

        assertThat(result).contains("<div")
        assertThat(result).contains("<h2")
        assertThat(result).contains("Battery Alert")
        assertThat(result).contains("Google Pixel 7")
        assertThat(result).contains("Android 14")
        assertThat(result).contains("Current: 8%")
        assertThat(result).contains("Threshold: 15%")
        assertThat(result).contains("Remote Notify")
    }

    @Test
    fun `toHtml formats storage alert with proper HTML structure`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Samsung",
                deviceModel = "Galaxy S23",
                androidVersion = "13",
                availableStorageGb = 5.0,
                storageThresholdGb = 10.0,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.HTML)

        assertThat(result).contains("<div")
        assertThat(result).contains("Storage Alert")
        assertThat(result).contains("Samsung Galaxy S23")
        assertThat(result).contains("Current: 5.0 GB")
        assertThat(result).contains("Threshold: 10.0 GB")
    }

    @Test
    fun `toJson formats battery alert correctly`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                batteryLevel = 8,
                batteryThresholdPercent = 15,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.JSON)

        assertThat(result).contains("\"alertType\":\"BATTERY\"")
        assertThat(result).contains("\"deviceModel\":\"Google Pixel 7\"")
        assertThat(result).contains("\"androidVersion\":\"14\"")
        assertThat(result).contains("\"batteryLevel\":8")
        assertThat(result).contains("\"batteryThresholdPercent\":15")
        assertThat(result).contains("\"isoDateTime\":")
    }

    @Test
    fun `toJson formats storage alert correctly`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Samsung",
                deviceModel = "Galaxy S23",
                androidVersion = "13",
                availableStorageGb = 4.5,
                storageThresholdGb = 10.0,
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.JSON)

        assertThat(result).contains("\"alertType\":\"STORAGE\"")
        assertThat(result).contains("\"deviceModel\":\"Samsung Galaxy S23\"")
        assertThat(result).contains("\"androidVersion\":\"13\"")
        assertThat(result).contains("\"availableStorageGb\":4.5")
        assertThat(result).contains("\"storageThresholdGb\":10.0")
    }

    @Test
    fun `device name is properly capitalized for lowercase brand`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.TEXT)

        assertThat(result).contains("Google Pixel 7")
    }

    @Test
    fun `battery alert uses correct emoji`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.BATTERY,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.EXTENDED_TEXT)

        assertThat(result).contains("🪫")
    }

    @Test
    fun `storage alert uses correct emoji`() {
        val alert =
            DeviceAlert(
                alertType = AlertType.STORAGE,
                deviceBrand = "Google",
                deviceModel = "Pixel 7",
                androidVersion = "14",
                timestamp = fixedTimestamp,
            )

        val result = alert.format(DeviceAlert.FormatType.EXTENDED_TEXT)

        assertThat(result).contains("💾")
    }
}
