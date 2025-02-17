package dev.hossain.remotenotify.utils

import android.app.Activity
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * https://developer.android.com/guide/playcore/in-app-review
 */
class InAppReviewManager constructor(
    private val activity: Activity,
) {
    private val preferencesDataStore: AppPreferencesDataStore = AppPreferencesDataStore(activity.applicationContext)

    // https://developer.android.com/reference/com/google/android/play/core/review/ReviewManager
    private val reviewManager: ReviewManager = ReviewManagerFactory.create(activity)
    private val minimumDaysBetweenReviewRequest = 45L

    suspend fun requestReview() {
        try {
            val lastReviewTime = preferencesDataStore.lastReviewRequestFlow.first()
            val daysSinceLastReview =
                TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - lastReviewTime,
                )

            if (lastReviewTime == 0L || daysSinceLastReview >= minimumDaysBetweenReviewRequest) {
                val reviewInfo: ReviewInfo = reviewManager.requestReviewFlow().await()
                val reviewResult = reviewManager.launchReviewFlow(activity, reviewInfo).await()

                /*
                 * Calling the launchReviewFlow method more than once during a short period of time
                 * (for example, less than a month) might not always display a dialog.
                 */
                preferencesDataStore.saveLastReviewRequestTime(System.currentTimeMillis())

                // Since the flow might complete without showing the dialog, we can log this for debugging
                Timber.d("Review flow completed with $reviewResult. Note: The dialog may or may not have been shown.")
            } else {
                Timber.d("Using fallback: Opening Play Store directly, daysSinceLastReview: $daysSinceLastReview")
                PlayStoreUtils.openPlayStoreForRating(activity)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to request review flow")
            PlayStoreUtils.openPlayStoreForRating(activity)
        }
    }
}
