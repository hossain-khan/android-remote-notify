package dev.hossain.remotenotify.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dev.zacsweers.metro.Inject
import timber.log.Timber

/**
 * Monitors the device battery level and provides utilities for battery status tracking.
 *
 * Uses Android's sticky broadcast [Intent.ACTION_BATTERY_CHANGED] to read the current
 * battery percentage without registering a persistent receiver.
 */
@Inject
class BatteryMonitor(
    private val context: Context,
) {
    /**
     * Returns the current battery level as a percentage (0–100),
     * or -1 if the battery level cannot be determined.
     */
    fun getBatteryLevel(): Int {
        val batteryIntent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        Timber.d("Battery level check: $batteryLevel%")
        return batteryLevel
    }

    /**
     * Registers a [BroadcastReceiver] to listen for battery level change events.
     */
    fun registerBatteryLevelReceiver(receiver: BroadcastReceiver) {
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    /**
     * Unregisters a previously registered battery level [BroadcastReceiver].
     */
    fun unregisterBatteryLevelReceiver(receiver: BroadcastReceiver) {
        context.unregisterReceiver(receiver)
    }
}
