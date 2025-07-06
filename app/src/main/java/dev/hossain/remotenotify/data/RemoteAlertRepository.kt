package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.db.AlertCheckLogDao
import dev.hossain.remotenotify.db.AlertCheckLogEntity
import dev.hossain.remotenotify.db.AlertConfigDao
import dev.hossain.remotenotify.db.AlertConfigEntity
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.toAlertCheckLog
import dev.hossain.remotenotify.model.toAlertConfigEntity
import dev.hossain.remotenotify.model.toRemoteAlert
import dev.hossain.remotenotify.notifier.NotifierType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

interface RemoteAlertRepository {
    suspend fun saveRemoteAlert(remoteAlert: RemoteAlert)

    suspend fun getAllRemoteAlert(): List<RemoteAlert>

    fun getAllRemoteAlertFlow(): Flow<List<RemoteAlert>>

    suspend fun deleteRemoteAlert(remoteAlert: RemoteAlert)

    suspend fun insertAlertCheckLog(
        alertId: Long,
        alertType: AlertType,
        alertStateValue: Int,
        alertTriggered: Boolean,
        notifierType: NotifierType?,
    )

    fun getLatestCheckForAlert(alertId: Long): Flow<AlertCheckLog?>

    fun getLatestCheckLog(): Flow<AlertCheckLog?>

    fun getAllAlertCheckLogs(): Flow<List<AlertCheckLog>>
}

@ContributesBinding(AppScope::class)
class RemoteAlertRepositoryImpl
    @Inject
    constructor(
        private val alertConfigDao: AlertConfigDao,
        private val alertCheckLogDao: AlertCheckLogDao,
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

        override suspend fun insertAlertCheckLog(
            alertId: Long,
            alertType: AlertType,
            alertStateValue: Int,
            alertTriggered: Boolean,
            notifierType: NotifierType?,
        ) {
            alertCheckLogDao.insert(
                AlertCheckLogEntity(
                    alertConfigId = alertId,
                    alertType = alertType,
                    alertStateValue = alertStateValue,
                    alertTriggered = alertTriggered,
                    notifierType = notifierType,
                ),
            )
        }

        override fun getLatestCheckForAlert(alertId: Long): Flow<AlertCheckLog?> =
            alertCheckLogDao
                .getLatestCheckForAlert(alertId)
                .map { it?.toAlertCheckLog() }

        override fun getLatestCheckLog(): Flow<AlertCheckLog?> =
            alertCheckLogDao
                .getLatestCheckLog()
                .map { it?.toAlertCheckLog() }

        override fun getAllAlertCheckLogs(): Flow<List<AlertCheckLog>> =
            alertCheckLogDao
                .getAllLogsWithConfig()
                .map { logEntityList ->
                    logEntityList
                        .map { alertCheckLogEntity -> alertCheckLogEntity.toAlertCheckLog() }
                }
    }
