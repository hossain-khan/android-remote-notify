package dev.hossain.remotenotify.data

import com.squareup.anvil.annotations.ContributesBinding
import dev.hossain.remotenotify.db.AlertConfigDao
import dev.hossain.remotenotify.db.AlertConfigEntity
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.toAlertConfigEntity
import dev.hossain.remotenotify.model.toRemoteAlert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface RemoteAlertRepository {
    suspend fun saveRemoteAlert(remoteAlert: RemoteAlert)

    suspend fun getAllRemoteAlert(): List<RemoteAlert>

    fun getAllRemoteAlertFlow(): Flow<List<RemoteAlert>>

    suspend fun deleteRemoteAlert(remoteAlert: RemoteAlert)
}

@ContributesBinding(AppScope::class)
class RemoteAlertRepositoryImpl
    @Inject
    constructor(
        private val alertConfigDao: AlertConfigDao,
    ) : RemoteAlertRepository {
        override suspend fun saveRemoteAlert(remoteAlert: RemoteAlert) {
            val entity = remoteAlert.toAlertConfigEntity()
            alertConfigDao.insert(entity)
        }

        override suspend fun getAllRemoteAlert(): List<RemoteAlert> = alertConfigDao.getAll().map { it.toRemoteAlert() }

        override fun getAllRemoteAlertFlow(): Flow<List<RemoteAlert>> =
            alertConfigDao.getAllFlow().map { notificationEntities: List<AlertConfigEntity> ->
                notificationEntities.map {
                    it.toRemoteAlert()
                }
            }

        override suspend fun deleteRemoteAlert(remoteAlert: RemoteAlert) {
            val entity = remoteAlert.toAlertConfigEntity()
            alertConfigDao.delete(entity)
        }
    }
