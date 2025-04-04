package dev.hossain.remotenotify

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import dev.hossain.remotenotify.di.AppComponent
import dev.hossain.remotenotify.utils.CrashlyticsTree
import dev.hossain.remotenotify.worker.DEVICE_VITALS_CHECKER_WORKER_ID
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for the app with key initializations.
 */
class RemoteAlertApp :
    Application(),
    Configuration.Provider {
    private val appComponent: AppComponent by lazy { AppComponent.create(this) }

    fun appComponent(): AppComponent = appComponent

    @Inject
    lateinit var workerFactory: WorkerFactory

    override val workManagerConfiguration: Configuration
        get() {
            Timber.i("Setting up custom WorkManager configuration")
            return Configuration
                .Builder()
                .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.WARN)
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        installLoggingTree()
        appComponent.inject(this)

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
