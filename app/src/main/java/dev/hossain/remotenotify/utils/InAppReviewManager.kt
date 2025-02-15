package dev.hossain.remotenotify.utils

import android.app.Activity
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class InAppReviewManager(
    private val activity: Activity,
) {
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(activity)

    suspend fun requestReview() {
        try {
            val reviewInfo: ReviewInfo = reviewManager.requestReviewFlow().await()
            reviewManager.launchReviewFlow(activity, reviewInfo).await()
        } catch (e: Exception) {
            // Handle the error, potentially fallback to direct Play Store link
            Timber.e(e, "Failed to request review flow.")
            PlayStoreUtils.openPlayStoreForRating(activity)
        }
    }
}
