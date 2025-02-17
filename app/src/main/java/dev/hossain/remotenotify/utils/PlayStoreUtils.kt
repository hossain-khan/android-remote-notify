package dev.hossain.remotenotify.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import timber.log.Timber

object PlayStoreUtils {
    /**
     * Opens the Google Play Store page for the app.
     * If Play Store app is not available, it opens the web browser.
     */
    fun openPlayStoreForRating(context: Context) {
        val packageName = context.packageName
        try {
            // Try to open Play Store app first
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Play Store app not found, opening in browser")
            // If Play Store app is not available, open in browser
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName"),
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }
}
