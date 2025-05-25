package dev.hossain.remotenotify.platform

import dev.hossain.remotenotify.BuildConfig

/**
 * Contains build-related information.
 */
object BuildInfo {
    /**
     * The Git SHA of the current build.
     */
    val GIT_SHA: String = BuildConfig.GIT_COMMIT_HASH
}