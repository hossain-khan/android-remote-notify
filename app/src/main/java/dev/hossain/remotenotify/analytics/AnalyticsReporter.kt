package dev.hossain.remotenotify.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.FirebaseAnalytics.Param.SCREEN_CLASS
import com.google.firebase.analytics.FirebaseAnalytics.Param.SCREEN_NAME
import com.google.firebase.analytics.logEvent
import com.slack.circuit.runtime.screen.Screen
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.remotenotify.analytics.Analytics.Companion.EVENT_SEND_APP_FEEDBACK
import dev.hossain.remotenotify.analytics.Analytics.Companion.EVENT_WORKER_JOB_COMPLETED
import dev.hossain.remotenotify.analytics.Analytics.Companion.EVENT_WORKER_JOB_FAILED
import dev.hossain.remotenotify.analytics.Analytics.Companion.EVENT_WORKER_JOB_STARTED
import dev.hossain.remotenotify.analytics.Analytics.Companion.eventAlertAdded
import dev.hossain.remotenotify.analytics.Analytics.Companion.eventAlertSentUsingNotifier
import dev.hossain.remotenotify.analytics.Analytics.Companion.eventConfigureNotifier
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType
import java.util.Locale
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * Interface for logging analytics events.
 */
interface Analytics {
    companion object {
        // [GA4] Event naming rules
        // Length of event name	= 1-40 characters
        // - https://support.google.com/analytics/answer/9267744
        // - https://support.google.com/analytics/answer/13316687
        // Here is what 40 characters may look like: "123456789_123456789_123456789_1234567890"

        internal const val EVENT_WORKER_JOB_STARTED = "rn_worker_job_initiated"
        internal const val EVENT_WORKER_JOB_COMPLETED = "rn_worker_job_success"
        internal const val EVENT_WORKER_JOB_FAILED = "rn_worker_job_failed"
        internal const val EVENT_SEND_APP_FEEDBACK = "rn_send_app_feedback"
        internal const val EVENT_CONFIGURE_NOTIFIER_PREFIX = "rn_configure_"
        internal const val EVENT_ALERT_ADDED_PREFIX = "rn_alert_added_"
        internal const val EVENT_ALERT_SENT_USING_PREFIX = "rn_alert_sent_"

        // NOTE: Instead of event property, I am using unique event name for each notifier and alert type.
        internal fun NotifierType.eventConfigureNotifier() = "$EVENT_CONFIGURE_NOTIFIER_PREFIX${name.lowercase(locale = Locale.US)}"

        internal fun NotifierType.eventAlertSentUsingNotifier() = "$EVENT_ALERT_SENT_USING_PREFIX${name.lowercase(locale = Locale.US)}"

        internal fun AlertType.eventAlertAdded() = "$EVENT_ALERT_ADDED_PREFIX${name.lowercase(locale = Locale.US)}"
    }

    /**
     * Logs a screen view event.
     *
     * @param circuitScreen The screen class to log.
     */
    suspend fun logScreenView(circuitScreen: KClass<out Screen>)

    /**
     * Logs worker job initiated event.
     */
    suspend fun logWorkerJob(
        interval: Long,
        alertsCount: Long,
    )

    suspend fun logWorkSuccess()

    suspend fun logWorkFailed(
        notifierType: NotifierType?,
        exception: Throwable? = null,
    )

    /**
     * Logs event when user intends to sends feedback.
     */
    suspend fun logSendFeedback()

    suspend fun logNotifierConfigured(notifierType: NotifierType)

    suspend fun logAlertAdded(alertType: AlertType)

    suspend fun logAlertSent(
        alertType: AlertType,
        notifierType: NotifierType,
    )

    suspend fun logViewTutorial(isComplete: Boolean)
}

/**
 * Implementation of [Analytics] interface.
 *
 * Uses [Firebase Analytics](https://firebase.google.com/docs/analytics/get-started?platform=android) to log different analytics.
 * See:
 * - [Events](https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Event)
 * - [Params](https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Param)
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AnalyticsImpl
    @Inject
    constructor(
        private val firebaseAnalytics: FirebaseAnalytics,
    ) : Analytics {
        override suspend fun logScreenView(circuitScreen: KClass<out Screen>) {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                param(SCREEN_NAME, requireNotNull(circuitScreen.simpleName))
                param(SCREEN_CLASS, requireNotNull(circuitScreen.qualifiedName))
            }
        }

        override suspend fun logWorkerJob(
            interval: Long,
            alertsCount: Long,
        ) {
            firebaseAnalytics.logEvent(EVENT_WORKER_JOB_STARTED) {
                param("update_interval", interval)
                param("alerts_count", alertsCount)
            }
        }

        override suspend fun logWorkSuccess() {
            firebaseAnalytics.logEvent(EVENT_WORKER_JOB_COMPLETED) {
                // The result of an operation (long). Specify 1 to indicate success and 0 to indicate failure.
                param(FirebaseAnalytics.Param.SUCCESS, 1L)
            }
        }

        override suspend fun logWorkFailed(
            notifierType: NotifierType?,
            exception: Throwable?,
        ) {
            firebaseAnalytics.logEvent(EVENT_WORKER_JOB_FAILED) {
                // The result of an operation (long). Specify 1 to indicate success and 0 to indicate failure.
                param(FirebaseAnalytics.Param.SUCCESS, 0L)
                notifierType?.let {
                    param(FirebaseAnalytics.Param.METHOD, it.name)
                }
                exception?.let {
                    // Max Length of event parameter value: 100 characters
                    param(FirebaseAnalytics.Param.VALUE, it.message?.take(100) ?: "Unknown error")
                }
            }
        }

        override suspend fun logNotifierConfigured(notifierType: NotifierType) {
            firebaseAnalytics.logEvent(notifierType.eventConfigureNotifier()) {
                param(FirebaseAnalytics.Param.METHOD, notifierType.name)
            }
        }

        override suspend fun logAlertAdded(alertType: AlertType) {
            firebaseAnalytics.logEvent(alertType.eventAlertAdded()) {}
        }

        override suspend fun logAlertSent(
            alertType: AlertType,
            notifierType: NotifierType,
        ) {
            firebaseAnalytics.logEvent(notifierType.eventAlertSentUsingNotifier()) {
                param(FirebaseAnalytics.Param.METHOD, notifierType.name)
            }
        }

        override suspend fun logSendFeedback() {
            firebaseAnalytics.logEvent(EVENT_SEND_APP_FEEDBACK) {}
        }

        /**
         * - https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Event#TUTORIAL_BEGIN()
         * - https://firebase.google.com/docs/reference/android/com/google/firebase/analytics/FirebaseAnalytics.Event#TUTORIAL_COMPLETE()
         */
        override suspend fun logViewTutorial(isComplete: Boolean) {
            if (isComplete) {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE) {}
            } else {
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN) {}
            }
        }
    }
