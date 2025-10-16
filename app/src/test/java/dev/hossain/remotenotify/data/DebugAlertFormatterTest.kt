package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.model.DeviceAlert
import dev.hossain.remotenotify.model.RemoteAlert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DebugAlertFormatterTest {
    private val alertFormatter = AlertFormatter()

    @Test
    fun `debug storage alert formatting`() {
        val testCases = listOf(
            RemoteAlert.StorageAlert(alertId = 1L, storageMinSpaceGb = 10, currentStorageGb = 15.0),
            RemoteAlert.StorageAlert(alertId = 1L, storageMinSpaceGb = 10, currentStorageGb = 10.0),
            RemoteAlert.StorageAlert(alertId = 1L, storageMinSpaceGb = 10, currentStorageGb = 6.0),
            RemoteAlert.StorageAlert(alertId = 1L, storageMinSpaceGb = 10, currentStorageGb = null),
        )

        testCases.forEach { alert ->
            val result = alertFormatter.format(alert, DeviceAlert.FormatType.EXTENDED_TEXT)
            println("==========================================")
            println("Input: storageMinSpaceGb=${alert.storageMinSpaceGb}, currentStorageGb=${alert.currentStorageGb}")
            println("Output:")
            println(result)
            println("==========================================\n")
        }
    }
}
