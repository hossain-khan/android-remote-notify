package dev.hossain.remotenotify

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import dev.hossain.remotenotify.di.AppGraph
import dev.hossain.remotenotify.di.ComposeAppComponentFactory
import dev.hossain.remotenotify.utils.CrashlyticsTree
import dev.hossain.remotenotify.worker.DEVICE_VITALS_CHECKER_WORKER_ID
import dev.zacsweers.metro.createGraphFactory
import timber.log.Timber

/**
 * Application class for the app with key initializations.
 */
class RemoteAlertApp :
    Application(),
    Configuration.Provider {
    /** Holder reference for the app graph for [ComposeAppComponentFactory]. */
    val appGraph: AppGraph by lazy { createGraphFactory<AppGraph.Factory>().create(this) }

    override val workManagerConfiguration: Configuration
        get() {
            Timber.i("Setting up custom WorkManager configuration")
            return Configuration
                .Builder()
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.WARN)
                .setWorkerFactory(appGraph.workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        installLoggingTree()

        // TEST WORKER CODE
//        dev.hossain.remotenotify.worker
//            .sendOneTimeWorkRequest(this)

        // Check worker updates on debug builds
        debugWorkRequestUpdates()
    }

    private fun installLoggingTree() {
        if (BuildConfig.DEBUG) {
            // Plant a debug tree for development builds
            Timber.plant(Timber.DebugTree())
        } else {
            // Plant the custom Crashlytics tree for production builds
            Timber.plant(CrashlyticsTree())
        }
    }

    private fun debugWorkRequestUpdates() {
        if (BuildConfig.DEBUG) {
            WorkManager
                .getInstance(this)
                .getWorkInfosByTagLiveData(DEVICE_VITALS_CHECKER_WORKER_ID)
                .observeForever { workInfos ->
                    Timber.d("Work status: ${workInfos?.map { it.state }}")
                }
        }
    }
}
