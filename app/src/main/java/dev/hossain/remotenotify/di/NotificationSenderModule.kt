package dev.hossain.remotenotify.di

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.TelegramNotificationSender

@Module
abstract class NotificationSenderModule {
    @Binds
    @IntoSet
    abstract fun bindTelegramNotificationSender(sender: TelegramNotificationSender): NotificationSender
}
