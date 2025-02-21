package dev.hossain.remotenotify.utils

import android.content.Context
import android.os.PowerManager

/**
 * Helper class to check if app is ignoring battery optimizations.
 *
 * - https://developer.android.com/training/monitoring-device-state/doze-standby.html
 */
object BatteryOptimizationHelper {
    /**
     * - https://developer.android.com/reference/android/os/PowerManager#isIgnoringBatteryOptimizations(java.lang.String)
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
