package dev.hossain.remotenotify

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkerFactory
import dev.hossain.remotenotify.di.AppComponent
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
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .setWorkerFactory(workerFactory)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        appComponent.inject(this)
    }
}
