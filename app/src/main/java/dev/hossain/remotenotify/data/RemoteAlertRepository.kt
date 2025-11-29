package dev.hossain.remotenotify.data

import dev.hossain.remotenotify.db.AlertCheckLogDao
import dev.hossain.remotenotify.db.AlertCheckLogEntity
import dev.hossain.remotenotify.db.AlertConfigDao
import dev.hossain.remotenotify.db.AlertConfigEntity
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.toAlertCheckLog
import dev.hossain.remotenotify.model.toAlertConfigEntity
import dev.hossain.remotenotify.model.toRemoteAlert
import dev.hossain.remotenotify.notifier.NotifierType
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface RemoteAlertRepository {
    suspend fun saveRemoteAlert(remoteAlert: RemoteAlert)

    /**
     * Updates an existing remote alert configuration in the repository.
     *
     * @param remoteAlert The alert to update. Must have a valid alertId corresponding to an existing alert.
     * @return The number of rows updated (0 if the alert was not found).
     */
    suspend fun updateRemoteAlert(remoteAlert: RemoteAlert): Int

    /**
     * Retrieves a remote alert by its unique identifier.
     *
     * @param alertId The unique ID of the alert to retrieve.
     * @return The [RemoteAlert] if found, or `null` if no alert with the given ID exists.
     */
    suspend fun getRemoteAlertById(alertId: Long): RemoteAlert?

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
@Inject
class RemoteAlertRepositoryImpl
    constructor(
        private val alertConfigDao: AlertConfigDao,
        private val alertCheckLogDao: AlertCheckLogDao,
    ) : RemoteAlertRepository {
        override suspend fun saveRemoteAlert(remoteAlert: RemoteAlert) {
            val entity = remoteAlert.toAlertConfigEntity()
            alertConfigDao.insert(entity)
        }

        override suspend fun updateRemoteAlert(remoteAlert: RemoteAlert): Int {
            val entity = remoteAlert.toAlertConfigEntity()
            return alertConfigDao.update(entity)
        }

        override suspend fun getRemoteAlertById(alertId: Long): RemoteAlert? = alertConfigDao.getById(alertId)?.toRemoteAlert()

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
