package dev.hossain.remotenotify

import android.app.Application
import dev.hossain.remotenotify.di.AppComponent

/**
 * Application class for the app with key initializations.
 */
class ComposeApp : Application() {
    private val appComponent: AppComponent by lazy { AppComponent.create(this) }

    fun appComponent(): AppComponent = appComponent
}
