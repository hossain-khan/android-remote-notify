package dev.hossain.remotenotify.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.TelegramNotificationSender
import dev.hossain.remotenotify.notifier.TwilioNotificationSender
import dev.hossain.remotenotify.notifier.WebhookRequestSender

@Module
abstract class NotificationSenderModule {
    @Binds
    @IntoSet
    abstract fun bindTelegramNotificationSender(sender: TelegramNotificationSender): NotificationSender

    @Binds
    @IntoSet
    abstract fun bindWebhookNotificationSender(sender: WebhookRequestSender): NotificationSender

    @Binds
    @IntoSet
    abstract fun bindTwilioNotificationSender(sender: TwilioNotificationSender): NotificationSender
}
