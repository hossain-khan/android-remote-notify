package dev.hossain.remotenotify.di

import androidx.annotation.Keep
import dev.hossain.remotenotify.notifier.MailgunEmailNotificationSender
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.SlackWebhookRequestSender
import dev.hossain.remotenotify.notifier.TelegramNotificationSender
import dev.hossain.remotenotify.notifier.TwilioNotificationSender
import dev.hossain.remotenotify.notifier.WebhookRequestSender
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

@Keep
@ContributesTo(AppScope::class)
interface NotificationSenderModule {
    @IntoSet
    @Provides
    fun bindTelegramNotificationSender(sender: TelegramNotificationSender): NotificationSender = sender

    @IntoSet
    @Provides
    fun bindSlackWebhookRequestSender(sender: SlackWebhookRequestSender): NotificationSender = sender

    @IntoSet
    @Provides
    fun bindWebhookNotificationSender(sender: WebhookRequestSender): NotificationSender = sender

    @IntoSet
    @Provides
    fun bindTwilioNotificationSender(sender: TwilioNotificationSender): NotificationSender = sender

    @IntoSet
    @Provides
    fun bindMailgunEmailNotificationSender(sender: MailgunEmailNotificationSender): NotificationSender = sender
}
