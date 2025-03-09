package dev.hossain.remotenotify.di

import androidx.annotation.Keep
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dev.hossain.remotenotify.notifier.MailgunEmailNotificationSender
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.SlackWebhookRequestSender
import dev.hossain.remotenotify.notifier.TelegramNotificationSender
import dev.hossain.remotenotify.notifier.TwilioNotificationSender
import dev.hossain.remotenotify.notifier.WebhookRequestSender

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
