package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.model.DeviceAlert
import dev.hossain.remotenotify.model.RemoteAlert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AlertFormatter] to ensure both storage and battery notifications include both current and threshold values.
 */
@RunWith(RobolectricTestRunner::class)
class AlertFormatterTest {
    private val alertFormatter = AlertFormatter()

    @Test
    fun `storage alert with current storage and threshold shows both values in extended text`() {
        // Given
        val storageAlert =
            RemoteAlert.StorageAlert(
                alertId = 1L,
                storageMinSpaceGb = 10,
                currentStorageGb = 8.5,
            )

        // When
        val result = alertFormatter.format(storageAlert, DeviceAlert.FormatType.EXTENDED_TEXT)

        // Then
        assertTrue("Should contain current storage value", result.contains("Current: 8.5 GB"))
        assertTrue("Should contain threshold value", result.contains("Threshold: 10.0 GB"))
        assertTrue("Should indicate critically low status", result.contains("critically low"))
    }

    @Test
    fun `storage alert with current storage and threshold shows both values in HTML`() {
        // Given
        val storageAlert =
            RemoteAlert.StorageAlert(
                alertId = 1L,
                storageMinSpaceGb = 10,
                currentStorageGb = 8.5,
            )

        // When
        val result = alertFormatter.format(storageAlert, DeviceAlert.FormatType.HTML)

        // Then
        assertTrue("Should contain current storage value", result.contains("Current: 8.5 GB"))
        assertTrue("Should contain threshold value", result.contains("Threshold: 10.0 GB"))
        assertTrue("Should indicate critically low status", result.contains("critically low"))
    }

    @Test
    fun `storage alert with current storage and threshold shows both values in text format`() {
        // Given
        val storageAlert =
            RemoteAlert.StorageAlert(
                alertId = 1L,
                storageMinSpaceGb = 10,
                currentStorageGb = 8.5,
            )

        // When
        val result = alertFormatter.format(storageAlert, DeviceAlert.FormatType.TEXT)

        // Then
        assertTrue("Should contain current storage value", result.contains("Current 8.5 GB"))
        assertTrue("Should contain threshold value", result.contains("Threshold: 10.0 GB"))
        assertTrue("Should indicate critical status", result.contains("Critical"))
    }

    @Test
    fun `storage alert without current storage falls back to threshold only`() {
        // Given
        val storageAlert =
            RemoteAlert.StorageAlert(
                alertId = 1L,
                storageMinSpaceGb = 10,
                // currentStorageGb is null (default)
            )

        // When
        val result = alertFormatter.format(storageAlert, DeviceAlert.FormatType.EXTENDED_TEXT)

        // Then
        assertTrue("Should contain threshold value", result.contains("10.0 GB"))
        assertTrue("Should not contain current/threshold distinction", !result.contains("Current:"))
        assertTrue("Should not contain current/threshold distinction", !result.contains("Threshold:"))
    }

    @Test
    fun `battery alert with current level and threshold shows both values in extended text`() {
        // Given
        val batteryAlert =
            RemoteAlert.BatteryAlert(
                alertId = 1L,
                batteryPercentage = 15,
                currentBatteryLevel = 8,
            )

        // When
        val result = alertFormatter.format(batteryAlert, DeviceAlert.FormatType.EXTENDED_TEXT)

        // Then
        assertTrue("Should contain current battery level", result.contains("Current: 8%"))
        assertTrue("Should contain threshold value", result.contains("Threshold: 15%"))
        assertTrue("Should indicate critically low status", result.contains("critically low"))
    }

    @Test
    fun `battery alert with current level and threshold shows both values in HTML`() {
        // Given
        val batteryAlert =
            RemoteAlert.BatteryAlert(
                alertId = 1L,
                batteryPercentage = 15,
                currentBatteryLevel = 8,
            )

        // When
        val result = alertFormatter.format(batteryAlert, DeviceAlert.FormatType.HTML)

        // Then
        assertTrue("Should contain current battery level", result.contains("Current: 8%"))
        assertTrue("Should contain threshold value", result.contains("Threshold: 15%"))
        assertTrue("Should indicate critically low status", result.contains("critically low"))
    }

    @Test
    fun `battery alert with current level and threshold shows both values in text format`() {
        // Given
        val batteryAlert =
            RemoteAlert.BatteryAlert(
                alertId = 1L,
                batteryPercentage = 15,
                currentBatteryLevel = 8,
            )

        // When
        val result = alertFormatter.format(batteryAlert, DeviceAlert.FormatType.TEXT)

        // Then
        assertTrue("Should contain current battery level", result.contains("Current 8%"))
        assertTrue("Should contain threshold value", result.contains("Threshold: 15%"))
        assertTrue("Should indicate critical status", result.contains("Critical"))
    }

    @Test
    fun `battery alert without current level falls back to threshold only`() {
        // Given
        val batteryAlert =
            RemoteAlert.BatteryAlert(
                alertId = 1L,
                batteryPercentage = 15,
                // currentBatteryLevel is null (default)
            )

        // When
        val result = alertFormatter.format(batteryAlert, DeviceAlert.FormatType.EXTENDED_TEXT)

        // Then
        assertTrue("Should contain battery percentage", result.contains("15%"))
        assertTrue("Should not contain current/threshold distinction", !result.contains("Current:"))
        assertTrue("Should not contain current/threshold distinction", !result.contains("Threshold:"))
        assertTrue("Should contain critical low message", result.contains("critically low"))
    }

    @Test
    fun `battery alert formatting maintains backward compatibility`() {
        // Given
        val batteryAlert =
            RemoteAlert.BatteryAlert(
                alertId = 1L,
                batteryPercentage = 15,
            )

        // When
        val result = alertFormatter.format(batteryAlert, DeviceAlert.FormatType.EXTENDED_TEXT)

        // Then
        assertTrue("Should contain battery percentage", result.contains("15%"))
        assertTrue("Should contain critical low message", result.contains("critically low"))
    }
}
