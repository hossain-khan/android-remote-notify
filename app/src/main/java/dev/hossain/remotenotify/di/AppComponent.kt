package dev.hossain.remotenotify.di

import android.app.Activity
import android.content.Context
import dev.hossain.remotenotify.RemoteAlertApp
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Component
import dev.zacsweers.metro.SingleIn
import javax.inject.Provider

@SingleIn(AppScope::class)
@Component(
    modules = [
        AppModule::class,
        CircuitModule::class,
        DatabaseModule::class,
        NetworkModule::class,
        NotificationSenderModule::class,
    ],
)
interface AppComponent {
    val activityProviders: Map<Class<out Activity>, @JvmSuppressWildcards Provider<Activity>>

    /**
     * Injects dependencies into [RemoteAlertApp].
     */
    fun inject(app: RemoteAlertApp)

    @Component.Factory
    interface Factory {
        fun create(
            @ApplicationContext context: Context,
        ): AppComponent
    }

    companion object {
        fun create(context: Context): AppComponent = 
            dev.zacsweers.metro.createComponent<AppComponent.Factory>().create(context)
    }
}
