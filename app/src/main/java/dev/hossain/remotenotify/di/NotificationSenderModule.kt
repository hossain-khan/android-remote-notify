package dev.hossain.remotenotify.di

import androidx.annotation.Keep
import dev.hossain.remotenotify.notifier.MailgunEmailNotificationSender
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.SlackWebhookRequestSender
import dev.hossain.remotenotify.notifier.TelegramNotificationSender
import dev.hossain.remotenotify.notifier.TwilioNotificationSender
import dev.hossain.remotenotify.notifier.WebhookRequestSender
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Module

@Keep
@Module
abstract class NotificationSenderModule {
    @Binds
    @IntoSet
    abstract fun bindTelegramNotificationSender(sender: TelegramNotificationSender): NotificationSender

    @Binds
    @IntoSet
    abstract fun bindSlackWebhookRequestSender(sender: SlackWebhookRequestSender): NotificationSender

    @Binds
    @IntoSet
    abstract fun bindWebhookNotificationSender(sender: WebhookRequestSender): NotificationSender

    @Binds
    @IntoSet
    abstract fun bindTwilioNotificationSender(sender: TwilioNotificationSender): NotificationSender

    @Binds
    @IntoSet
    abstract fun bindMailgunEmailNotificationSender(sender: MailgunEmailNotificationSender): NotificationSender
}
