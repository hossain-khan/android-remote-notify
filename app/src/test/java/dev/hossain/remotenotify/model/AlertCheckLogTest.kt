package dev.hossain.remotenotify.model

import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.db.AlertCheckLogEntity
import dev.hossain.remotenotify.db.AlertConfigEntity
import dev.hossain.remotenotify.db.AlertLogWithConfig
import dev.hossain.remotenotify.notifier.NotifierType
import org.junit.Test

/**
 * Unit tests for [AlertCheckLog] extension functions.
 */
class AlertCheckLogTest {
    @Test
    fun `AlertCheckLogEntity toAlertCheckLog converts correctly`() {
        val entity =
            AlertCheckLogEntity(
                id = 1L,
                alertConfigId = 100L,
                checkedAt = 1710500000000L,
                alertStateValue = 8,
                alertTriggered = true,
                alertType = AlertType.BATTERY,
                notifierType = NotifierType.EMAIL,
            )

        val result = entity.toAlertCheckLog()

        assertThat(result.checkedOn).isEqualTo(1710500000000L)
        assertThat(result.alertType).isEqualTo(AlertType.BATTERY)
        assertThat(result.isAlertSent).isTrue()
        assertThat(result.notifierType).isEqualTo(NotifierType.EMAIL)
        assertThat(result.stateValue).isEqualTo(8)
        // Config fields should be 0 when converting from entity alone
        assertThat(result.configId).isEqualTo(0)
        assertThat(result.configBatteryPercentage).isEqualTo(0)
        assertThat(result.configStorageMinSpaceGb).isEqualTo(0)
        assertThat(result.configCreatedOn).isEqualTo(0)
    }

    @Test
    fun `AlertCheckLogEntity toAlertCheckLog handles null notifierType`() {
        val entity =
            AlertCheckLogEntity(
                id = 2L,
                alertConfigId = 101L,
                checkedAt = 1710500000000L,
                alertStateValue = 50,
                alertTriggered = false,
                alertType = AlertType.STORAGE,
                notifierType = null,
            )

        val result = entity.toAlertCheckLog()

        assertThat(result.notifierType).isNull()
        assertThat(result.isAlertSent).isFalse()
        assertThat(result.alertType).isEqualTo(AlertType.STORAGE)
    }

    @Test
    fun `AlertLogWithConfig toAlertCheckLog converts with config data`() {
        val logEntity =
            AlertCheckLogEntity(
                id = 1L,
                alertConfigId = 100L,
                checkedAt = 1710500000000L,
                alertStateValue = 8,
                alertTriggered = true,
                alertType = AlertType.BATTERY,
                notifierType = NotifierType.TELEGRAM,
            )

        val configEntity =
            AlertConfigEntity(
                id = 100L,
                type = AlertType.BATTERY,
                batteryPercentage = 15,
                storageMinSpaceGb = 0,
                createdOn = 1710400000000L,
            )

        val alertLogWithConfig =
            AlertLogWithConfig(
                log = logEntity,
                config = configEntity,
            )

        val result = alertLogWithConfig.toAlertCheckLog()

        // Log fields
        assertThat(result.checkedOn).isEqualTo(1710500000000L)
        assertThat(result.alertType).isEqualTo(AlertType.BATTERY)
        assertThat(result.isAlertSent).isTrue()
        assertThat(result.notifierType).isEqualTo(NotifierType.TELEGRAM)
        assertThat(result.stateValue).isEqualTo(8)

        // Config fields
        assertThat(result.configId).isEqualTo(100L)
        assertThat(result.configBatteryPercentage).isEqualTo(15)
        assertThat(result.configStorageMinSpaceGb).isEqualTo(0)
        assertThat(result.configCreatedOn).isEqualTo(1710400000000L)
    }

    @Test
    fun `AlertLogWithConfig toAlertCheckLog converts storage alert correctly`() {
        val logEntity =
            AlertCheckLogEntity(
                id = 2L,
                alertConfigId = 200L,
                checkedAt = 1710600000000L,
                alertStateValue = 5,
                alertTriggered = true,
                alertType = AlertType.STORAGE,
                notifierType = NotifierType.WEBHOOK_SLACK_WORKFLOW,
            )

        val configEntity =
            AlertConfigEntity(
                id = 200L,
                type = AlertType.STORAGE,
                batteryPercentage = 0,
                storageMinSpaceGb = 10,
                createdOn = 1710300000000L,
            )

        val alertLogWithConfig =
            AlertLogWithConfig(
                log = logEntity,
                config = configEntity,
            )

        val result = alertLogWithConfig.toAlertCheckLog()

        assertThat(result.alertType).isEqualTo(AlertType.STORAGE)
        assertThat(result.configStorageMinSpaceGb).isEqualTo(10)
        assertThat(result.notifierType).isEqualTo(NotifierType.WEBHOOK_SLACK_WORKFLOW)
    }

    @Test
    fun `AlertLogWithConfig toAlertCheckLog handles all notifier types`() {
        NotifierType.entries.forEach { notifierType ->
            val logEntity =
                AlertCheckLogEntity(
                    id = 1L,
                    alertConfigId = 100L,
                    checkedAt = 1710500000000L,
                    alertStateValue = 8,
                    alertTriggered = true,
                    alertType = AlertType.BATTERY,
                    notifierType = notifierType,
                )

            val configEntity =
                AlertConfigEntity(
                    id = 100L,
                    type = AlertType.BATTERY,
                    batteryPercentage = 15,
                    storageMinSpaceGb = 0,
                    createdOn = 1710400000000L,
                )

            val alertLogWithConfig =
                AlertLogWithConfig(
                    log = logEntity,
                    config = configEntity,
                )

            val result = alertLogWithConfig.toAlertCheckLog()

            assertThat(result.notifierType).isEqualTo(notifierType)
        }
    }
}
