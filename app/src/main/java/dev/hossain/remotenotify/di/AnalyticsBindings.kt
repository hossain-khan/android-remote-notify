package dev.hossain.remotenotify.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides

@BindingContainer
object AnalyticsBindings {
    @Provides
    fun provideFirebaseAnalytics(context: Context): FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
}
