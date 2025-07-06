package dev.hossain.remotenotify.di

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.worker.ObserveDeviceHealthWorker
import me.tatarka.inject.annotations.ContributesTo
import me.tatarka.inject.annotations.Provides

// Metro module to contribute the WorkerFactory
@ContributesTo(AppScope::class)
interface WorkerModule {
    @Provides
    fun provideWorkerFactory(
        batteryMonitor: BatteryMonitor,
        storageMonitor: StorageMonitor,
        repository: RemoteAlertRepository,
        notifiers: Set<@JvmSuppressWildcards NotificationSender>,
        analytics: Analytics,
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
                            analytics = analytics,
                        )
                    else -> null
                }
            }
        }
}
