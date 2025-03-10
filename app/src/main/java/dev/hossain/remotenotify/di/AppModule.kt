package dev.hossain.remotenotify.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import java.time.Clock

@ContributesTo(AppScope::class)
@Module
class AppModule {
    @Provides
    fun provideBatteryMonitor(
        @ApplicationContext context: Context,
    ): BatteryMonitor = BatteryMonitor(context)

    @Provides
    fun provideStorageMonitor(
        @ApplicationContext context: Context,
    ): StorageMonitor = StorageMonitor(context)

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context,
    ): FirebaseAnalytics {
        FirebaseApp.initializeApp(context)
        return Firebase.analytics
    }
}
