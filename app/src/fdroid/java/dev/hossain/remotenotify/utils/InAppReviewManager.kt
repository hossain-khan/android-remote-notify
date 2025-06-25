package dev.hossain.remotenotify.utils

import android.app.Activity
import timber.log.Timber

/**
 * F-Droid version of InAppReviewManager that doesn't use Google Play Services.
 * This version does nothing since F-Droid doesn't support Google Play In-App Review.
 */
class InAppReviewManager constructor(
    private val activity: Activity,
) {
    suspend fun requestReview() {
        Timber.d("In-app review not supported in F-Droid builds")
        // F-Droid builds don't support Google Play In-App Review
        // Users can provide feedback through other channels like GitHub issues
    }
}