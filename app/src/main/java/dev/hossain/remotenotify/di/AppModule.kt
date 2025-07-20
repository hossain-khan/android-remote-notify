package dev.hossain.remotenotify.di

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import java.time.Clock

@BindingContainer
object AppModule { // TODO: rename to AppBindings or similar
    @Provides
    fun provideBatteryMonitor(context: Context): BatteryMonitor = BatteryMonitor(context)

    @Provides
    fun provideStorageMonitor(context: Context): StorageMonitor = StorageMonitor(context)

    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    fun provideFirebaseAnalytics(context: Context): FirebaseAnalytics {
        FirebaseApp.initializeApp(context)
        return Firebase.analytics
    }
}
