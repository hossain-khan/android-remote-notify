package dev.hossain.remotenotify.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [DeviceAlertJsonPayload] class to ensure JSON serialization works correctly.
 */
class DeviceAlertJsonPayloadTest {
    @Test
    fun `toJson serializes battery alert payload correctly`() {
        val payload =
            DeviceAlertJsonPayload(
                alertType = AlertType.BATTERY,
                deviceModel = "Google Pixel 7",
                androidVersion = "14",
                batteryLevel = 8,
                batteryThresholdPercent = 15,
                isoDateTime = "2024-03-15T14:30:45",
            )

        val result = payload.toJson()

        assertThat(result).contains("\"alertType\":\"BATTERY\"")
        assertThat(result).contains("\"deviceModel\":\"Google Pixel 7\"")
        assertThat(result).contains("\"androidVersion\":\"14\"")
        assertThat(result).contains("\"batteryLevel\":8")
        assertThat(result).contains("\"batteryThresholdPercent\":15")
        assertThat(result).contains("\"isoDateTime\":\"2024-03-15T14:30:45\"")
        // Storage fields should not be present when null
        assertThat(result).doesNotContain("availableStorageGb")
        assertThat(result).doesNotContain("storageThresholdGb")
    }

    @Test
    fun `toJson serializes storage alert payload correctly`() {
        val payload =
            DeviceAlertJsonPayload(
                alertType = AlertType.STORAGE,
                deviceModel = "Samsung Galaxy S23",
                androidVersion = "13",
                availableStorageGb = 4.5,
                storageThresholdGb = 10.0,
                isoDateTime = "2024-03-15T14:30:45",
            )

        val result = payload.toJson()

        assertThat(result).contains("\"alertType\":\"STORAGE\"")
        assertThat(result).contains("\"deviceModel\":\"Samsung Galaxy S23\"")
        assertThat(result).contains("\"androidVersion\":\"13\"")
        assertThat(result).contains("\"availableStorageGb\":4.5")
        assertThat(result).contains("\"storageThresholdGb\":10.0")
        assertThat(result).contains("\"isoDateTime\":\"2024-03-15T14:30:45\"")
        // Battery fields should not be present when null
        assertThat(result).doesNotContain("batteryLevel")
        assertThat(result).doesNotContain("batteryThresholdPercent")
    }

    @Test
    fun `toJson handles battery alert with only battery level`() {
        val payload =
            DeviceAlertJsonPayload(
                alertType = AlertType.BATTERY,
                deviceModel = "Google Pixel 7",
                androidVersion = "14",
                batteryLevel = 10,
                isoDateTime = "2024-03-15T14:30:45",
            )

        val result = payload.toJson()

        assertThat(result).contains("\"batteryLevel\":10")
        assertThat(result).doesNotContain("batteryThresholdPercent")
    }

    @Test
    fun `toJson handles storage alert with only available storage`() {
        val payload =
            DeviceAlertJsonPayload(
                alertType = AlertType.STORAGE,
                deviceModel = "Samsung Galaxy S23",
                androidVersion = "13",
                availableStorageGb = 8.5,
                isoDateTime = "2024-03-15T14:30:45",
            )

        val result = payload.toJson()

        assertThat(result).contains("\"availableStorageGb\":8.5")
        assertThat(result).doesNotContain("storageThresholdGb")
    }

    @Test
    fun `toJson produces valid JSON format`() {
        val payload =
            DeviceAlertJsonPayload(
                alertType = AlertType.BATTERY,
                deviceModel = "Google Pixel 7",
                androidVersion = "14",
                batteryLevel = 8,
                batteryThresholdPercent = 15,
                isoDateTime = "2024-03-15T14:30:45",
            )

        val result = payload.toJson()

        // Check JSON starts and ends correctly
        assertThat(result).startsWith("{")
        assertThat(result).endsWith("}")
    }
}
