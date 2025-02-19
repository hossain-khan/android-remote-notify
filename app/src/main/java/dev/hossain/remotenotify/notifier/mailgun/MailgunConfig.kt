package dev.hossain.remotenotify.notifier.mailgun

import dev.hossain.remotenotify.BuildConfig

object MailgunConfig {
    /**
     * https://documentation.mailgun.com/docs/mailgun/user-manual/mg_security/
     */
    const val API_KEY = BuildConfig.EMAIL_API_KEY
    const val DOMAIN = "notify.liquidlabs.ca"
    const val FROM_EMAIL = "alert@notify.liquidlabs.ca"
}
