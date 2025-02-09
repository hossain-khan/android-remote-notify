package app.example.di

import android.app.Activity
import android.content.Context
import com.squareup.anvil.annotations.MergeComponent
import com.squareup.anvil.annotations.optional.SingleIn
import dagger.BindsInstance
import javax.inject.Provider

@MergeComponent(
    scope = AppScope::class,
    modules = [ExampleAppModule::class, CircuitModule::class],
)
@SingleIn(AppScope::class)
interface AppComponent {
    val activityProviders: Map<Class<out Activity>, @JvmSuppressWildcards Provider<Activity>>

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
