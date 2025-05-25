package dev.hossain.remotenotify.platform

import dev.hossain.remotenotify.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the application version information.
 */
interface AppVersionProvider {
    /**
     * Gets the formatted app version string.
     * @return A string in the format "v{VERSION_NAME} ({GIT_SHA})"
     */
    suspend fun getAppVersion(): String
}

/**
 * Default implementation of [AppVersionProvider] that uses BuildConfig and BuildInfo.
 */
@Singleton
class DefaultAppVersionProvider @Inject constructor() : AppVersionProvider {
    override suspend fun getAppVersion(): String {
        return "v${BuildConfig.VERSION_NAME} (${BuildInfo.GIT_SHA})"
    }
}