package dev.hossain.remotenotify.data

import com.squareup.anvil.annotations.ContributesBinding
import dev.hossain.remotenotify.db.NotificationDao
import dev.hossain.remotenotify.db.NotificationEntity
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.toNotificationEntity
import dev.hossain.remotenotify.model.toRemoteNotification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface RemoteAlertRepository {
    suspend fun saveRemoteNotification(notification: RemoteAlert)

    suspend fun getAllRemoteNotifications(): List<RemoteAlert>

    fun getAllRemoteNotificationsFlow(): Flow<List<RemoteAlert>>

    suspend fun deleteRemoteNotification(notification: RemoteAlert)
}

@ContributesBinding(AppScope::class)
class RemoteAlertRepositoryImpl
    @Inject
    constructor(
        private val notificationDao: NotificationDao,
    ) : RemoteAlertRepository {
        override suspend fun saveRemoteNotification(notification: RemoteAlert) {
            val entity = notification.toNotificationEntity()
            notificationDao.insert(entity)
        }

        override suspend fun getAllRemoteNotifications(): List<RemoteAlert> =
            notificationDao.getAll().map { it.toRemoteNotification() }

        override fun getAllRemoteNotificationsFlow(): Flow<List<RemoteAlert>> =
            notificationDao.getAllFlow().map { notificationEntities: List<NotificationEntity> ->
                notificationEntities.map {
                    it.toRemoteNotification()
                }
            }

        override suspend fun deleteRemoteNotification(notification: RemoteAlert) {
            val entity = notification.toNotificationEntity()
            notificationDao.delete(entity)
        }
    }
