package dev.hossain.remotenotify.di

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import kotlin.reflect.KClass

@DependencyGraph(AppScope::class)
interface AppGraph {
    @Provides fun provideApplicationContext(application: Application): Context = application

    /**
     * A multibinding map of activity classes to their providers accessible for
     * [ComposeAppComponentFactory].
     */
    @Multibinds val activityProviders: Map<KClass<out Activity>, Provider<Activity>>

    val workManager: WorkManager

    @Provides
    fun providesWorkManager(application: Context): WorkManager = WorkManager.getInstance(application)

//  @Multibinds
//  val workerProviders:
//    Map<KClass<out ListenableWorker>, Provider<MetroWorkerFactory.WorkerInstanceFactory<*>>>
//
    val workerFactory: WorkerFactory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
        ): AppGraph
    }
}
