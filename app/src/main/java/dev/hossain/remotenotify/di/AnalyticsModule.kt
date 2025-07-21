package dev.hossain.remotenotify.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
object AnalyticsModule { // TODO: rename to AnalyticsBindings or similar
    @Provides
    fun provideFirebaseAnalytics(context: Context): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
}
