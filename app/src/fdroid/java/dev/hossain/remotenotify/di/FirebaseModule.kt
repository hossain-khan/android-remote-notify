package dev.hossain.remotenotify.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.Module

/**
 * F-Droid version of FirebaseModule that provides no Firebase dependencies.
 * Analytics are handled via local logging only.
 */
@ContributesTo(AppScope::class)
@Module
class FirebaseModule {
    // No Firebase dependencies for F-Droid builds
    // Analytics implementation uses Timber logging instead
}