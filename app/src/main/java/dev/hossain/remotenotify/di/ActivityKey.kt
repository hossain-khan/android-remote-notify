package dev.hossain.remotenotify.di

import android.app.Activity
import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

/**
 * A Metro map key annotation used for registering a [Activity] into the dependency graph.
 */
@MapKey
annotation class ActivityKey(
    val value: KClass<out Activity>,
)
