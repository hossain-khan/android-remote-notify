package dev.hossain.remotenotify.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import java.time.Clock

@BindingContainer
object AppBindings {
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
