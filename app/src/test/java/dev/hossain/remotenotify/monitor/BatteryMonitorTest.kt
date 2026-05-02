package dev.hossain.remotenotify.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import timber.log.Timber

/**
 * Unit tests for [BatteryMonitor].
 */
@RunWith(RobolectricTestRunner::class)
class BatteryMonitorTest {
    private lateinit var context: Context
    private lateinit var batteryMonitor: BatteryMonitor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Suppress Timber/Crashlytics logs during tests
        Timber.uprootAll()
        batteryMonitor = BatteryMonitor(context)
    }

    @Test
    fun `getBatteryLevel returns -1 when battery info is not available`() {
        // In Robolectric, no sticky ACTION_BATTERY_CHANGED broadcast is set by default,
        // so registerReceiver(null, ...) returns null, and getBatteryLevel should return -1.
        val level = batteryMonitor.getBatteryLevel()

        assertThat(level).isEqualTo(-1)
    }

    @Test
    fun `getBatteryLevel returns correct percentage when battery broadcast is available`() {
        // Send a sticky battery changed intent via the standard API (Robolectric handles this)
        val batteryIntent =
            Intent(Intent.ACTION_BATTERY_CHANGED).apply {
                putExtra(BatteryManager.EXTRA_LEVEL, 75)
                putExtra(BatteryManager.EXTRA_SCALE, 100)
            }
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(batteryIntent)

        val level = batteryMonitor.getBatteryLevel()

        assertThat(level).isEqualTo(75)
    }

    @Test
    fun `getBatteryLevel calculates percentage correctly for non-100 scale`() {
        // Battery level 3 out of 4 = 75%
        val batteryIntent =
            Intent(Intent.ACTION_BATTERY_CHANGED).apply {
                putExtra(BatteryManager.EXTRA_LEVEL, 3)
                putExtra(BatteryManager.EXTRA_SCALE, 4)
            }
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(batteryIntent)

        val level = batteryMonitor.getBatteryLevel()

        assertThat(level).isEqualTo(75)
    }

    @Test
    fun `getBatteryLevel returns -1 when level is negative`() {
        val batteryIntent =
            Intent(Intent.ACTION_BATTERY_CHANGED).apply {
                putExtra(BatteryManager.EXTRA_LEVEL, -1)
                putExtra(BatteryManager.EXTRA_SCALE, 100)
            }
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(batteryIntent)

        val level = batteryMonitor.getBatteryLevel()

        assertThat(level).isEqualTo(-1)
    }

    @Test
    fun `getBatteryLevel returns -1 when scale is zero`() {
        val batteryIntent =
            Intent(Intent.ACTION_BATTERY_CHANGED).apply {
                putExtra(BatteryManager.EXTRA_LEVEL, 50)
                putExtra(BatteryManager.EXTRA_SCALE, 0)
            }
        @Suppress("DEPRECATION")
        context.sendStickyBroadcast(batteryIntent)

        val level = batteryMonitor.getBatteryLevel()

        assertThat(level).isEqualTo(-1)
    }

    @Test
    fun `registerBatteryLevelReceiver does not throw an exception`() {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {}
            }

        // Should not throw
        batteryMonitor.registerBatteryLevelReceiver(receiver)

        // Clean up
        batteryMonitor.unregisterBatteryLevelReceiver(receiver)
    }

    @Test
    fun `unregisterBatteryLevelReceiver does not throw when receiver is registered`() {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context?,
                    intent: Intent?,
                ) {}
            }

        // Register via BatteryMonitor API first, then unregister to ensure proper cleanup
        batteryMonitor.registerBatteryLevelReceiver(receiver)
        batteryMonitor.unregisterBatteryLevelReceiver(receiver)
        // No exception should be thrown
    }
}
