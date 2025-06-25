package dev.hossain.remotenotify.utils

import android.util.Log
import timber.log.Timber

/**
 * F-Droid version of CrashlyticsTree that doesn't use Firebase Crashlytics.
 * This version only logs to the Android log system.
 */
class CrashlyticsTree : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        // For F-Droid builds, only log to Android system log
        if (priority >= Log.WARN) {
            Log.println(priority, tag ?: "RemoteNotify", message)
            t?.let { 
                Log.println(priority, tag ?: "RemoteNotify", "Exception: ${it.message}")
            }
        }
    }
}