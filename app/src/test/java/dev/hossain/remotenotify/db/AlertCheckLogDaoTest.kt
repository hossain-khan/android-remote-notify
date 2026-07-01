package dev.hossain.remotenotify.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlertCheckLogDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var configDao: AlertConfigDao
    private lateinit var logDao: AlertCheckLogDao

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        configDao = database.notificationDao()
        logDao = database.alertCheckLogDao()

        val config = AlertConfigEntity(
            id = 1L,
            type = AlertType.BATTERY,
            batteryPercentage = 15,
            storageMinSpaceGb = 0,
            createdOn = 1000L
        )
        configDao.insert(config)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `getLatestCheckLog prioritizes triggered alert over skipped check when timestamps match regardless of insertion order`() = runTest {
        val timestamp = 5000L

        // Insert skipped check first
        logDao.insert(
            AlertCheckLogEntity(
                alertConfigId = 1L,
                checkedAt = timestamp,
                alertStateValue = 80,
                alertTriggered = false,
                alertType = AlertType.BATTERY,
                notifierType = NotifierType.EMAIL
            )
        )

        // Insert triggered alert second
        logDao.insert(
            AlertCheckLogEntity(
                alertConfigId = 1L,
                checkedAt = timestamp,
                alertStateValue = 80,
                alertTriggered = true,
                alertType = AlertType.BATTERY,
                notifierType = NotifierType.TELEGRAM
            )
        )

        val latestLog = logDao.getLatestCheckLog().first()
        assertThat(latestLog?.alertTriggered).isTrue()
        assertThat(latestLog?.notifierType).isEqualTo(NotifierType.TELEGRAM)
    }

    @Test
    fun `getLatestCheckForAlert prioritizes triggered alert over skipped check when timestamps match`() = runTest {
        val timestamp = 5000L

        // Insert skipped check first
        logDao.insert(
            AlertCheckLogEntity(
                alertConfigId = 1L,
                checkedAt = timestamp,
                alertStateValue = 80,
                alertTriggered = false,
                alertType = AlertType.BATTERY,
                notifierType = NotifierType.EMAIL
            )
        )

        // Insert triggered alert second
        logDao.insert(
            AlertCheckLogEntity(
                alertConfigId = 1L,
                checkedAt = timestamp,
                alertStateValue = 80,
                alertTriggered = true,
                alertType = AlertType.BATTERY,
                notifierType = NotifierType.TELEGRAM
            )
        )

        val latestLog = logDao.getLatestCheckForAlert(1L).first()
        assertThat(latestLog?.alertTriggered).isTrue()
        assertThat(latestLog?.notifierType).isEqualTo(NotifierType.TELEGRAM)
    }
}
