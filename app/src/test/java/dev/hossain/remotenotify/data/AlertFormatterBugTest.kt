package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.model.DeviceAlert
import dev.hossain.remotenotify.model.RemoteAlert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Test to reproduce the bug where threshold value is incorrectly shown in notifications.
 *
 * Bug report:
 * First notification: Current: 9.0 GB, Threshold: 10.0 GB ✓
 * Second notification: Current: 6.0 GB, Threshold: 6.0 GB ✗ (should be 10.0 GB)
 */
@RunWith(RobolectricTestRunner::class)
class AlertFormatterBugTest {
    private val alertFormatter = AlertFormatter()

    @Test
    fun `bug reproduction - storage threshold should not change between notifications`() {
        // Given: User configured threshold is 10 GB

        // First notification: Current storage is 9 GB (above threshold)
        // This should not trigger, but let's test the formatting anyway
        val firstAlert =
            RemoteAlert.StorageAlert(
                alertId = 1L,
                storageMinSpaceGb = 10, // Threshold: 10 GB
                currentStorageGb = 9.0, // Current: 9.0 GB
            )

        // When
        val firstResult = alertFormatter.format(firstAlert, DeviceAlert.FormatType.EXTENDED_TEXT)

        // Then
        assertTrue("First notification should show current storage", firstResult.contains("Current: 9.0 GB"))
        assertTrue("First notification should show threshold", firstResult.contains("Threshold: 10.0 GB"))

        // Second notification: Current storage dropped to 6 GB (below threshold of 10 GB)
        val secondAlert =
            RemoteAlert.StorageAlert(
                alertId = 1L,
                storageMinSpaceGb = 10, // Threshold should STILL be: 10 GB
                currentStorageGb = 6.0, // Current: 6.0 GB
            )

        // When
        val secondResult = alertFormatter.format(secondAlert, DeviceAlert.FormatType.EXTENDED_TEXT)

        // Then
        assertTrue("Second notification should show current storage as 6.0 GB", secondResult.contains("Current: 6.0 GB"))
        assertTrue("Second notification should show threshold as 10.0 GB", secondResult.contains("Threshold: 10.0 GB"))

        // The bug would cause the threshold to show as 6.0 GB instead of 10.0 GB
        assertFalse(
            "Bug check: Threshold should NOT be 6.0 GB (same as current)",
            secondResult.contains("Threshold: 6.0 GB"),
        )
    }

    @Test
    fun `storage alert threshold must always match configured storageMinSpaceGb`() {
        // Test various current storage values to ensure threshold remains constant
        val testCases =
            listOf(
                Pair(15.0, "15.0 GB available"),
                Pair(8.0, "8.0 GB available"),
                Pair(5.0, "5.0 GB available"),
                Pair(2.0, "2.0 GB available"),
            )

        testCases.forEach { (currentStorage, description) ->
            // Given
            val alert =
                RemoteAlert.StorageAlert(
                    alertId = 1L,
                    storageMinSpaceGb = 10, // Threshold is always 10 GB
                    currentStorageGb = currentStorage,
                )

            // When
            val result = alertFormatter.format(alert, DeviceAlert.FormatType.EXTENDED_TEXT)

            // Then
            assertTrue(
                "[$description] Should show current storage as $currentStorage GB",
                result.contains("Current: $currentStorage GB"),
            )
            assertTrue(
                "[$description] Should always show threshold as 10.0 GB regardless of current storage",
                result.contains("Threshold: 10.0 GB"),
            )
            assertFalse(
                "[$description] Bug check: Threshold should NOT match current storage value when they differ",
                currentStorage != 10.0 && result.contains("Threshold: $currentStorage GB"),
            )
        }
    }

    @Test
    fun `battery alert threshold must always match configured batteryPercentage`() {
        // Test the same scenario for battery alerts
        val testCases =
            listOf(
                Pair(25, "25% battery"),
                Pair(15, "15% battery"),
                Pair(10, "10% battery"),
                Pair(5, "5% battery"),
            )

        testCases.forEach { (currentLevel, description) ->
            // Given
            val alert =
                RemoteAlert.BatteryAlert(
                    alertId = 1L,
                    batteryPercentage = 20, // Threshold is always 20%
                    currentBatteryLevel = currentLevel,
                )

            // When
            val result = alertFormatter.format(alert, DeviceAlert.FormatType.EXTENDED_TEXT)

            // Then
            assertTrue(
                "[$description] Should show current battery as $currentLevel%",
                result.contains("Current: $currentLevel%"),
            )
            assertTrue(
                "[$description] Should always show threshold as 20% regardless of current level",
                result.contains("Threshold: 20%"),
            )
            assertFalse(
                "[$description] Bug check: Threshold should NOT match current battery level when they differ",
                currentLevel != 20 && result.contains("Threshold: $currentLevel%"),
            )
        }
    }
}
