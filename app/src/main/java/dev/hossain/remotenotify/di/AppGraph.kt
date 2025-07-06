package dev.hossain.remotenotify.di

import android.app.Activity
import android.content.Context
import androidx.work.WorkerFactory
import com.slack.circuit.foundation.Circuit
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass

@DependencyGraph
@SingleIn(AppScope::class)
interface AppGraph {
    val activityProviders: Map<KClass<out Activity>, @JvmSuppressWildcards Provider<Activity>>
    val circuit: Circuit
    val workerFactory: WorkerFactory

    @DependencyGraph.Factory
    interface Factory {
        fun create(
            @ApplicationContext @Provides context: Context,
        ): AppGraph
    }
}
