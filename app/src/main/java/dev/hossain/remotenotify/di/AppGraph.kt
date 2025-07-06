package dev.hossain.remotenotify.di

import android.app.Activity
import android.content.Context
import androidx.work.WorkerFactory
import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph
@SingleIn(AppScope::class)
interface AppGraph {
    val activityProviders: Map<Class<out Activity>, @JvmSuppressWildcards Provider<Activity>>
    val circuit: Circuit
    val workerFactory: WorkerFactory

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @ApplicationContext @Provides context: Context,
        ): AppGraph
    }

    companion object {
        override fun create(context: Context): AppGraph = createGraphFactory<Factory>().create(context)
    }
}
