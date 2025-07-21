package dev.hossain.remotenotify.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dev.zacsweers.metro.Inject

@Inject
class BatteryMonitor(
    private val context: Context,
) {
    fun getBatteryLevel(): Int {
        val batteryIntent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else -1
    }

    fun registerBatteryLevelReceiver(receiver: BroadcastReceiver) {
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun unregisterBatteryLevelReceiver(receiver: BroadcastReceiver) {
        context.unregisterReceiver(receiver)
    }
}
