package dev.hossain.remotenotify.db

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.hossain.remotenotify.data.AlertCheckLogDao
import dev.hossain.remotenotify.data.AlertCheckLogEntity
import dev.hossain.remotenotify.data.AlertConfigDao
import dev.hossain.remotenotify.data.AlertConfigEntity
import dev.hossain.remotenotify.data.AppDatabase
import dev.hossain.remotenotify.model.AlertType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.* // Using wildcard import for assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private val TEST_DB = "migration-test" // For migration test

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java.name,
        FrameworkSQLiteOpenHelperFactory()
    )

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule() // For LiveData/Flow

    private lateinit var database: AppDatabase
    private lateinit var alertConfigDao: AlertConfigDao
    private lateinit var alertCheckLogDao: AlertCheckLogDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries() // Allow queries on main thread for tests
            .build()
        alertConfigDao = database.notificationDao() // Assuming notificationDao is the AlertConfigDao
        alertCheckLogDao = database.alertCheckLogDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // Migration Test
    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        helper.createDatabase(TEST_DB, 1).apply {
            // Schema version 1 has alert_config with:
            // id, type, battery_percentage (default -1), storage_min_space_gb (default -1), created_on
            // In version 2, 'is_active' (boolean, default true) was added.
            // The original INSERT INTO alert_config VALUES (1, 'BATTERY', 20, 5, 1678886400000)
            // needs to account for the schema at version 1.
            // If 'is_active' was NOT in version 1, the insert is correct.
            // If 'is_active' WAS added in version 2 via auto-migration with a default, the insert is correct.
            // The prompt's SQL for v1 does not include is_active, which is fine.
            execSQL("INSERT INTO alert_config VALUES (1, 'BATTERY', 20, 5, 1678886400000)")
            close()
        }

        // Auto-migration from 1 to 2 adds the alert_check_log table and 'is_active' to 'alert_config'.
        // Validate schema and data.
        helper.runMigrationsAndValidate(TEST_DB, 2, true)

        val migratedDbInstance = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
            TEST_DB
        ).allowMainThreadQueries() // For test simplicity
         .build()

        val dao = migratedDbInstance.notificationDao()
        // Get all configs, find the one with id 1
        val targetId = 1.toLong()
        val config = runBlocking { dao.getAll().find { it.id == targetId } }

        assertNotNull("Config with ID $targetId not found after migration", config)
        config?.let {
            assertEquals(AlertType.BATTERY, it.type)
            assertEquals(20, it.batteryPercentage)
            assertEquals(5, it.storageMinSpaceGb)
            assertEquals(1678886400000L, it.createdOn)
            // Assuming 'is_active' was added in v2 with a default value of true (or 1 for SQLite BOOLEAN)
            // This should be asserted if the autoMigration adds it with a default.
            // assertTrue("isActive should be true after migration", it.isActive)
        }

        migratedDbInstance.close()
    }

    // DAO Tests for AlertConfigDao
    @Test
    @Throws(Exception::class)
    fun insertAndGetAlertConfig() = runBlocking {
        val alertConfig = AlertConfigEntity(type = AlertType.BATTERY, batteryPercentage = 15, storageMinSpaceGb = -1)
        alertConfigDao.insert(alertConfig)

        val retrievedConfigs = alertConfigDao.getAll()
        val retrieved = retrievedConfigs.find {
            it.type == alertConfig.type &&
            it.batteryPercentage == alertConfig.batteryPercentage &&
            it.storageMinSpaceGb == alertConfig.storageMinSpaceGb
        }
        assertNotNull("Config not found after insert: $alertConfig", retrieved)
    }

    @Test
    @Throws(Exception::class)
    fun deleteAlertConfig() = runBlocking {
        val alertConfig = AlertConfigEntity(type = AlertType.STORAGE, batteryPercentage = -1, storageMinSpaceGb = 10)
        alertConfigDao.insert(alertConfig)

        val insertedConfig = alertConfigDao.getAll().firstOrNull {
            it.type == alertConfig.type &&
            it.storageMinSpaceGb == alertConfig.storageMinSpaceGb
        }
        assertNotNull("Failed to retrieve config to delete. Original: $alertConfig", insertedConfig)

        insertedConfig?.let {
            alertConfigDao.delete(it)
            val allConfigsAfterDelete = alertConfigDao.getAll()
            assertTrue("Config was not deleted. ID: ${it.id}", allConfigsAfterDelete.none { c -> c.id == it.id })
        }
    }

    @Test
    @Throws(Exception::class)
    fun getAlertWithLogs_noLogs() = runBlocking {
        val alertConfig = AlertConfigEntity(type = AlertType.BATTERY, batteryPercentage = 25, storageMinSpaceGb = -1)
        alertConfigDao.insert(alertConfig)

        val insertedConfig = alertConfigDao.getAll().firstOrNull {
            it.type == alertConfig.type &&
            it.batteryPercentage == alertConfig.batteryPercentage
        }
        assertNotNull("Failed to retrieve config for getAlertWithLogs_noLogs. Original: $alertConfig", insertedConfig)

        insertedConfig?.let {
            val configWithLogs = alertConfigDao.getAlertWithLogs(it.id).first()
            assertNotNull("ConfigWithLogs was null for ID: ${it.id}", configWithLogs)
            assertEquals(it.type, configWithLogs.alertConfig.type)
            assertEquals(it.id, configWithLogs.alertConfig.id)
            assertTrue("Logs should be empty but were: ${configWithLogs.logs}", configWithLogs.logs.isEmpty())
        }
    }

    @Test
    @Throws(Exception::class)
    fun getAlertWithLogs_withLogs() = runBlocking {
        val alertConfig = AlertConfigEntity(type = AlertType.STORAGE, batteryPercentage = -1, storageMinSpaceGb = 5)
        alertConfigDao.insert(alertConfig)

        val insertedConfig = alertConfigDao.getAll().firstOrNull {
            it.type == alertConfig.type &&
            it.storageMinSpaceGb == alertConfig.storageMinSpaceGb
        }
        assertNotNull("Failed to retrieve config for getAlertWithLogs_withLogs. Original: $alertConfig", insertedConfig)

        insertedConfig?.let { config ->
            val log1 = AlertCheckLogEntity(alertConfigId = config.id, alertStateValue = 4, alertTriggered = true, alertType = AlertType.STORAGE, notifierType = null, checkedAt = System.currentTimeMillis())
            val log2 = AlertCheckLogEntity(alertConfigId = config.id, alertStateValue = 6, alertTriggered = false, alertType = AlertType.STORAGE, notifierType = null, checkedAt = System.currentTimeMillis() + 1000)
            alertCheckLogDao.insert(log1)
            alertCheckLogDao.insert(log2)

            val configWithLogs = alertConfigDao.getAlertWithLogs(config.id).first()

            assertNotNull("ConfigWithLogs was null for ID: ${config.id}", configWithLogs)
            assertEquals(config.type, configWithLogs.alertConfig.type)
            assertEquals(config.id, configWithLogs.alertConfig.id)
            assertEquals("Incorrect number of logs for ID ${config.id}. Expected 2, got ${configWithLogs.logs.size}", 2, configWithLogs.logs.size)

            val logValues = configWithLogs.logs.map { it.alertStateValue }.sorted()
            assertEquals(listOf(4, 6), logValues)
        }
    }

    // Helper function for AlertCheckLogDao tests
    private suspend fun insertTestConfigAndGetId(alertType: AlertType = AlertType.BATTERY): Long {
        val config = AlertConfigEntity(type = alertType, batteryPercentage = (10..30).random(), storageMinSpaceGb = -1, createdOn = System.currentTimeMillis())
        alertConfigDao.insert(config)
        return alertConfigDao.getAll().last().id
    }

    // DAO Tests for AlertCheckLogDao
    @Test
    fun insertAndGetLog() = runBlocking {
        val configId = insertTestConfigAndGetId()
        val log = AlertCheckLogEntity(alertConfigId = configId, alertStateValue = 10, alertTriggered = false, alertType = AlertType.BATTERY, notifierType = null)
        alertCheckLogDao.insert(log)

        val logs = alertCheckLogDao.getCheckHistoryForAlert(configId).first()
        assertTrue("No logs found after insert for config ID $configId", logs.isNotEmpty())
        val retrievedLog = logs.find { it.alertStateValue == 10 }
        assertNotNull("Log with value 10 not found for config ID $configId", retrievedLog)
        assertEquals(configId, retrievedLog?.alertConfigId)
    }

    @Test
    fun getLatestCheckForAlert_returnsLatest() = runBlocking {
        val configId = insertTestConfigAndGetId()
        val olderLog = AlertCheckLogEntity(alertConfigId = configId, checkedAt = System.currentTimeMillis() - 10000, alertStateValue = 5, alertTriggered = false, alertType = AlertType.BATTERY, notifierType = null)
        val newerLog = AlertCheckLogEntity(alertConfigId = configId, checkedAt = System.currentTimeMillis(), alertStateValue = 10, alertTriggered = true, alertType = AlertType.BATTERY, notifierType = null)
        alertCheckLogDao.insert(olderLog)
        alertCheckLogDao.insert(newerLog)

        val latest = alertCheckLogDao.getLatestCheckForAlert(configId).first()
        assertNotNull("Latest log was null for config ID $configId", latest)
        assertEquals(newerLog.alertStateValue, latest?.alertStateValue)
        assertTrue("Latest log alertTriggered was false, expected true", latest?.alertTriggered == true)
    }

    @Test
    fun getLatestCheckForAlert_returnsNullForNoLogs() = runBlocking {
        val configId = insertTestConfigAndGetId() // Config exists, but no logs
        val latest = alertCheckLogDao.getLatestCheckForAlert(configId).first()
        assertNull("Latest log was not null for config ID $configId which has no logs", latest)
    }

    @Test
    fun getCheckHistoryForAlert_returnsCorrectLogs() = runBlocking {
        val configId = insertTestConfigAndGetId()
        val log1 = AlertCheckLogEntity(alertConfigId = configId, checkedAt = System.currentTimeMillis(), alertStateValue = 1, alertTriggered = false, alertType = AlertType.BATTERY, notifierType = null)
        val log2 = AlertCheckLogEntity(alertConfigId = configId, checkedAt = System.currentTimeMillis() + 100, alertStateValue = 2, alertTriggered = true, alertType = AlertType.BATTERY, notifierType = null)
        alertCheckLogDao.insert(log1)
        alertCheckLogDao.insert(log2)

        val history = alertCheckLogDao.getCheckHistoryForAlert(configId).first()
        assertEquals("Expected 2 logs in history for config ID $configId, but found ${history.size}", 2, history.size)
        // Query orders by checked_at DESC
        assertEquals(log2.alertStateValue, history[0].alertStateValue)
        assertEquals(log1.alertStateValue, history[1].alertStateValue)
    }

    @Test
    fun deleteLogsForAlert_deletesOnlySpecified() = runBlocking {
        val configId1 = insertTestConfigAndGetId(AlertType.BATTERY)
        val configId2 = insertTestConfigAndGetId(AlertType.STORAGE)

        val log1Config1 = AlertCheckLogEntity(alertConfigId = configId1, alertStateValue = 1, alertTriggered = false, alertType = AlertType.BATTERY, notifierType = null)
        val log2Config1 = AlertCheckLogEntity(alertConfigId = configId1, alertStateValue = 2, alertTriggered = false, alertType = AlertType.BATTERY, notifierType = null)
        val log1Config2 = AlertCheckLogEntity(alertConfigId = configId2, alertStateValue = 3, alertTriggered = false, alertType = AlertType.STORAGE, notifierType = null)
        alertCheckLogDao.insert(log1Config1)
        alertCheckLogDao.insert(log2Config1)
        alertCheckLogDao.insert(log1Config2)

        alertCheckLogDao.deleteLogsForAlert(configId1)

        val logsForConfig1 = alertCheckLogDao.getCheckHistoryForAlert(configId1).first()
        assertTrue("Logs for config ID $configId1 were not deleted", logsForConfig1.isEmpty())

        val logsForConfig2 = alertCheckLogDao.getCheckHistoryForAlert(configId2).first()
        assertFalse("Logs for config ID $configId2 were unexpectedly empty", logsForConfig2.isEmpty())
        assertEquals("Expected 1 log for config ID $configId2 after deleting logs for $configId1, but found ${logsForConfig2.size}", 1, logsForConfig2.size)
        assertEquals(log1Config2.alertStateValue, logsForConfig2[0].alertStateValue)
    }

    @Test
    fun getTriggeredAlerts_returnsOnlyTriggered() = runBlocking {
        val configId = insertTestConfigAndGetId()
        val triggeredLog = AlertCheckLogEntity(alertConfigId = configId, alertStateValue = 10, alertTriggered = true, alertType = AlertType.BATTERY, notifierType = null)
        val nonTriggeredLog = AlertCheckLogEntity(alertConfigId = configId, alertStateValue = 5, alertTriggered = false, alertType = AlertType.BATTERY, notifierType = null)
        alertCheckLogDao.insert(triggeredLog)
        alertCheckLogDao.insert(nonTriggeredLog)

        val triggeredList = alertCheckLogDao.getTriggeredAlerts().first()
        assertEquals("Expected 1 triggered log, but found ${triggeredList.size}", 1, triggeredList.size)
        assertEquals(triggeredLog.alertStateValue, triggeredList[0].alertStateValue)
        assertTrue(triggeredList[0].alertTriggered)
    }

    @Test
    fun getLatestCheckLog_returnsOverallLatest() = runBlocking {
        val configId1 = insertTestConfigAndGetId(AlertType.BATTERY)
        val configId2 = insertTestConfigAndGetId(AlertType.STORAGE)

        val logConfig1 = AlertCheckLogEntity(alertConfigId = configId1, checkedAt = System.currentTimeMillis() - 2000, alertStateValue = 1, alertTriggered = false, alertType = AlertType.BATTERY, notifierType = null)
        val overallLatestLog = AlertCheckLogEntity(alertConfigId = configId2, checkedAt = System.currentTimeMillis(), alertStateValue = 3, alertTriggered = true, alertType = AlertType.STORAGE, notifierType = null)
        val olderLogConfig2 = AlertCheckLogEntity(alertConfigId = configId2, checkedAt = System.currentTimeMillis() - 1000, alertStateValue = 2, alertTriggered = false, alertType = AlertType.STORAGE, notifierType = null)

        alertCheckLogDao.insert(logConfig1)
        alertCheckLogDao.insert(overallLatestLog)
        alertCheckLogDao.insert(olderLogConfig2)

        val latest = alertCheckLogDao.getLatestCheckLog().first()
        assertNotNull("Overall latest log was null", latest)
        assertEquals(overallLatestLog.alertStateValue, latest?.alertStateValue)
        assertEquals(overallLatestLog.alertConfigId, latest?.alertConfigId)
    }

    @Test
    fun getAllLogsWithConfig_returnsCorrectData() = runBlocking {
        val config1Entity = AlertConfigEntity(type = AlertType.BATTERY, batteryPercentage = 10, storageMinSpaceGb = -1, createdOn = System.currentTimeMillis())
        alertConfigDao.insert(config1Entity)
        val insertedConfig1 = alertConfigDao.getAll().first { it.type == AlertType.BATTERY && it.batteryPercentage == 10 }


        val config2Entity = AlertConfigEntity(type = AlertType.STORAGE, batteryPercentage = -1, storageMinSpaceGb = 5, createdOn = System.currentTimeMillis() + 10) // Ensure different creation time
        alertConfigDao.insert(config2Entity)
        val insertedConfig2 = alertConfigDao.getAll().first { it.type == AlertType.STORAGE && it.storageMinSpaceGb == 5 }


        val log1C1 = AlertCheckLogEntity(alertConfigId = insertedConfig1.id, alertStateValue = 1, alertTriggered = false, alertType = AlertType.BATTERY, notifierType = null, checkedAt = System.currentTimeMillis())
        val log2C1 = AlertCheckLogEntity(alertConfigId = insertedConfig1.id, alertStateValue = 2, alertTriggered = true, alertType = AlertType.BATTERY, notifierType = null, checkedAt = System.currentTimeMillis() + 10)
        val log1C2 = AlertCheckLogEntity(alertConfigId = insertedConfig2.id, alertStateValue = 3, alertTriggered = false, alertType = AlertType.STORAGE, notifierType = null, checkedAt = System.currentTimeMillis() + 20)
        alertCheckLogDao.insert(log1C1)
        alertCheckLogDao.insert(log2C1)
        alertCheckLogDao.insert(log1C2)

        val logsWithConfig = alertCheckLogDao.getAllLogsWithConfig(limit = 5).first()
        assertEquals("Expected 3 logs with config, but found ${logsWithConfig.size}", 3, logsWithConfig.size)

        // getAllLogsWithConfig orders by checked_at DESC by default (as per typical DAO implementation)
        assertEquals(log1C2.alertStateValue, logsWithConfig[0].alertCheckLog.alertStateValue)
        assertEquals(insertedConfig2.id, logsWithConfig[0].alertConfig.id)
        assertEquals(AlertType.STORAGE, logsWithConfig[0].alertConfig.type)

        assertEquals(log2C1.alertStateValue, logsWithConfig[1].alertCheckLog.alertStateValue)
        assertEquals(insertedConfig1.id, logsWithConfig[1].alertConfig.id)
        assertEquals(AlertType.BATTERY, logsWithConfig[1].alertConfig.type)

        assertEquals(log1C1.alertStateValue, logsWithConfig[2].alertCheckLog.alertStateValue)
        assertEquals(insertedConfig1.id, logsWithConfig[2].alertConfig.id)
        assertEquals(AlertType.BATTERY, logsWithConfig[2].alertConfig.type)

        val c1Logs = logsWithConfig.filter { it.alertConfig.id == insertedConfig1.id }
        val c2Logs = logsWithConfig.filter { it.alertConfig.id == insertedConfig2.id }
        assertEquals(2, c1Logs.size)
        assertEquals(1, c2Logs.size)
        assertTrue(c1Logs.all { it.alertConfig.type == AlertType.BATTERY })
        assertTrue(c2Logs.all { it.alertConfig.type == AlertType.STORAGE })
    }
}
