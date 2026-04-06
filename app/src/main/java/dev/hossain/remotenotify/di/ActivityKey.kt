package dev.hossain.remotenotify.di

import android.app.Activity
import dev.zacsweers.metro.MapKey
import kotlin.reflect.KClass

/**
 * A Metro multi-binding [MapKey] used for registering a [Activity] into the top level graphs.
 *
 * With [MapKey.implicitClassKey] enabled, the annotated class is used as the map key automatically,
 * so callers do not need to specify the class reference in the annotation argument.
 * The [value] parameter defaults to [Nothing] as a sentinel; Metro infers the actual class.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@MapKey(implicitClassKey = true)
annotation class ActivityKey(
    val value: KClass<out Activity> = Nothing::class,
)
