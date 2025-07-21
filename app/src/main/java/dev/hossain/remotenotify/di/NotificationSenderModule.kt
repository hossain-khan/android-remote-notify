package dev.hossain.remotenotify.di

import androidx.annotation.Keep
import dev.hossain.remotenotify.notifier.MailgunEmailNotificationSender
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.SlackWebhookRequestSender
import dev.hossain.remotenotify.notifier.TelegramNotificationSender
import dev.hossain.remotenotify.notifier.TwilioNotificationSender
import dev.hossain.remotenotify.notifier.WebhookRequestSender
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/**
 * https://zacsweers.github.io/metro/bindings/#multibindings
 */
@Keep
@BindingContainer
object NotificationSenderModule { // TODO rename this to NotificationSenderMultibinding
    @Provides
    @IntoSet
    fun bindTelegramNotificationSender(sender: TelegramNotificationSender): NotificationSender = sender

    @Provides
    @IntoSet
    fun bindSlackWebhookRequestSender(sender: SlackWebhookRequestSender): NotificationSender = sender

    @Provides
    @IntoSet
    fun bindWebhookNotificationSender(sender: WebhookRequestSender): NotificationSender = sender

    @Provides
    @IntoSet
    fun bindTwilioNotificationSender(sender: TwilioNotificationSender): NotificationSender = sender

    @Provides
    @IntoSet
    fun bindMailgunEmailNotificationSender(sender: MailgunEmailNotificationSender): NotificationSender = sender
}
