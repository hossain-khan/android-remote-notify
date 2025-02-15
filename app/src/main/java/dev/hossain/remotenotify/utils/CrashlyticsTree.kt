package dev.hossain.remotenotify.utils

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * A [Timber.Tree] that logs ERROR or higher severity messages to Crashlytics.
 */
class CrashlyticsTree : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        // Log only WARN or higher severity messages to Crashlytics
        if (priority >= Log.WARN) {
            FirebaseCrashlytics.getInstance().apply {
                // Log the message
                log(message)

                // Record the exception if available
                t?.let { recordException(it) }
            }
        }
    }
}
