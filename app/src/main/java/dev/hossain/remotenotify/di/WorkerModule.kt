package dev.hossain.remotenotify.di

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.worker.ObserveDeviceHealthWorker

// Anvil module to contribute the WorkerFactory
@Module
@ContributesTo(AppScope::class)
object WorkerModule {
    @Provides
    fun provideWorkerFactory(
        batteryMonitor: BatteryMonitor,
        storageMonitor: StorageMonitor,
        repository: RemoteAlertRepository,
        notifiers: Set<@JvmSuppressWildcards NotificationSender>,
    ): WorkerFactory =
        object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): CoroutineWorker? {
                val workerClass =
                    Class
                        .forName(workerClassName)
                        .asSubclass(CoroutineWorker::class.java)
                return when (workerClass) {
                    ObserveDeviceHealthWorker::class.java ->
                        ObserveDeviceHealthWorker(
                            context = appContext,
                            workerParams = workerParameters,
                            batteryMonitor = batteryMonitor,
                            storageMonitor = storageMonitor,
                            repository = repository,
                            notifiers = notifiers,
                        )
                    else -> null
                }
            }
        }
}
