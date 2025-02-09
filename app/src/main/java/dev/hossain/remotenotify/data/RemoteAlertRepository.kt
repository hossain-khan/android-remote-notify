package dev.hossain.remotenotify.data

import com.squareup.anvil.annotations.ContributesBinding
import dev.hossain.remotenotify.db.NotificationDao
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.RemoteNotification
import dev.hossain.remotenotify.model.toNotificationEntity
import dev.hossain.remotenotify.model.toRemoteNotification
import javax.inject.Inject

interface RemoteAlertRepository {
    suspend fun saveRemoteNotification(notification: RemoteNotification)

    suspend fun getAllRemoteNotifications(): List<RemoteNotification>

    suspend fun deleteRemoteNotification(notification: RemoteNotification)
}

@ContributesBinding(AppScope::class)
class RemoteAlertRepositoryImpl
    @Inject
    constructor(
        private val notificationDao: NotificationDao,
    ) : RemoteAlertRepository {
        override suspend fun saveRemoteNotification(notification: RemoteNotification) {
            val entity = notification.toNotificationEntity()
            notificationDao.insert(entity)
        }

        override suspend fun getAllRemoteNotifications(): List<RemoteNotification> =
            notificationDao.getAll().map { it.toRemoteNotification() }

        override suspend fun deleteRemoteNotification(notification: RemoteNotification) {
            val entity = notification.toNotificationEntity()
            notificationDao.delete(entity)
        }
    }
