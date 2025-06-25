package dev.hossain.remotenotify.analytics

import com.slack.circuit.runtime.screen.Screen
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.optional.SingleIn
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType
import timber.log.Timber
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * F-Droid implementation of [Analytics] interface.
 * This version logs events to Timber instead of Firebase Analytics for privacy compliance.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class AnalyticsImpl
    @Inject
    constructor() : Analytics {
        
        override suspend fun logScreenView(circuitScreen: KClass<out Screen>) {
            Timber.d("📊 [F-Droid Analytics] Screen view: ${circuitScreen.simpleName}")
        }

        override suspend fun logWorkerJob(
            interval: Long,
            alertsCount: Long,
        ) {
            Timber.d("📊 [F-Droid Analytics] Worker job started - interval: ${interval}ms, alerts: $alertsCount")
        }

        override suspend fun logWorkSuccess() {
            Timber.d("📊 [F-Droid Analytics] Worker job completed successfully")
        }

        override suspend fun logWorkFailed(
            notifierType: NotifierType?,
            exception: Throwable?,
        ) {
            Timber.d("📊 [F-Droid Analytics] Worker job failed - notifier: $notifierType, error: ${exception?.message}")
        }

        override suspend fun logNotifierConfigured(notifierType: NotifierType) {
            Timber.d("📊 [F-Droid Analytics] Notifier configured: $notifierType")
        }

        override suspend fun logAlertAdded(alertType: AlertType) {
            Timber.d("📊 [F-Droid Analytics] Alert added: $alertType")
        }

        override suspend fun logAlertSent(
            alertType: AlertType,
            notifierType: NotifierType,
        ) {
            Timber.d("📊 [F-Droid Analytics] Alert sent - type: $alertType, notifier: $notifierType")
        }

        override suspend fun logSendFeedback() {
            Timber.d("📊 [F-Droid Analytics] User initiated feedback")
        }

        override suspend fun logViewTutorial(isComplete: Boolean) {
            val status = if (isComplete) "completed" else "started"
            Timber.d("📊 [F-Droid Analytics] Tutorial $status")
        }

        override suspend fun logOptimizeBatteryInfoShown() {
            Timber.d("📊 [F-Droid Analytics] Battery optimization info shown")
        }

        override suspend fun logOptimizeBatteryGoToSettings() {
            Timber.d("📊 [F-Droid Analytics] User went to battery optimization settings")
        }

        override suspend fun logOptimizeBatteryIgnore() {
            Timber.d("📊 [F-Droid Analytics] User ignored battery optimization")
        }
    }