package dev.hossain.remotenotify.di

import android.app.Activity
import android.content.Context
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.BindsInstance
import dev.hossain.remotenotify.RemoteAlertApp
import javax.inject.Provider

@MergeComponent(
    scope = AppScope::class,
    modules = [
        AppModule::class,
        CircuitModule::class,
        DatabaseModule::class,
        NetworkModule::class,
        NotificationSenderModule::class,
    ],
)
@SingleIn(AppScope::class)
interface AppComponent {
    val activityProviders: Map<Class<out Activity>, @JvmSuppressWildcards Provider<Activity>>

    /**
     * Injects dependencies into [RemoteAlertApp].
     */
    fun inject(app: RemoteAlertApp)

    /**
     * Injects dependencies into [PluginProvider].
     */
    fun inject(provider: dev.hossain.remotenotify.plugin.PluginProvider)

    @MergeComponent.Factory
    interface Factory {
        fun create(
            @ApplicationContext @BindsInstance context: Context,
        ): AppComponent
    }

    companion object {
        fun create(context: Context): AppComponent = DaggerAppComponent.factory().create(context)
    }
}
