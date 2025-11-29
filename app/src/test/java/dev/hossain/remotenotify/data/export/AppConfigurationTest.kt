package dev.hossain.remotenotify.data.export

import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.model.AlertType
import org.junit.Test

class AppConfigurationTest {
    @Test
    fun `AlertConfig for battery has correct values`() {
        val alertConfig =
            AlertConfig(
                type = AlertType.BATTERY,
                batteryPercentage = 20,
            )

        assertThat(alertConfig.type).isEqualTo(AlertType.BATTERY)
        assertThat(alertConfig.batteryPercentage).isEqualTo(20)
        assertThat(alertConfig.storageMinSpaceGb).isNull()
    }

    @Test
    fun `AlertConfig for storage has correct values`() {
        val alertConfig =
            AlertConfig(
                type = AlertType.STORAGE,
                storageMinSpaceGb = 5,
            )

        assertThat(alertConfig.type).isEqualTo(AlertType.STORAGE)
        assertThat(alertConfig.storageMinSpaceGb).isEqualTo(5)
        assertThat(alertConfig.batteryPercentage).isNull()
    }

    @Test
    fun `AppConfiguration has correct default version`() {
        val config = AppConfiguration()

        assertThat(config.version).isEqualTo(AppConfiguration.CURRENT_VERSION)
    }

    @Test
    fun `NotifierConfigs getConfiguredNotifierTypes returns empty list when no configs`() {
        val notifiers = NotifierConfigs()

        assertThat(notifiers.getConfiguredNotifierTypes()).isEmpty()
    }

    @Test
    fun `NotifierConfigs getConfiguredNotifierTypes returns telegram when configured`() {
        val notifiers =
            NotifierConfigs(
                telegram = EncryptedConfig(encrypted = true, data = "test"),
            )

        val types = notifiers.getConfiguredNotifierTypes()
        assertThat(types).hasSize(1)
        assertThat(types[0].name).isEqualTo("TELEGRAM")
    }

    @Test
    fun `NotifierConfigs getConfiguredNotifierTypes returns all configured types`() {
        val notifiers =
            NotifierConfigs(
                telegram = EncryptedConfig(encrypted = true, data = "test"),
                email = EncryptedConfig(encrypted = true, data = "test"),
                webhook = EncryptedConfig(encrypted = true, data = "test"),
            )

        val types = notifiers.getConfiguredNotifierTypes()
        assertThat(types).hasSize(3)
    }

    @Test
    fun `EncryptedConfig has correct default encrypted value`() {
        val config = EncryptedConfig(data = "test")

        assertThat(config.encrypted).isTrue()
    }

    @Test
    fun `ImportValidationResult Valid contains configuration`() {
        val appConfig =
            AppConfiguration(
                alerts = listOf(AlertConfig(type = AlertType.BATTERY, batteryPercentage = 20)),
            )

        val result = ImportValidationResult.Valid(appConfig)

        assertThat(result.configuration).isEqualTo(appConfig)
    }

    @Test
    fun `ImportValidationResult Invalid contains errors`() {
        val errors = listOf("Error 1", "Error 2")

        val result = ImportValidationResult.Invalid(errors)

        assertThat(result.errors).containsExactly("Error 1", "Error 2")
    }

    @Test
    fun `ConfigOperationResult Success is singleton`() {
        val result1 = ConfigOperationResult.Success
        val result2 = ConfigOperationResult.Success

        assertThat(result1).isSameInstanceAs(result2)
    }

    @Test
    fun `ConfigOperationResult Error contains message and exception`() {
        val exception = RuntimeException("Test error")
        val result = ConfigOperationResult.Error("Failed to import", exception)

        assertThat(result.message).isEqualTo("Failed to import")
        assertThat(result.exception).isEqualTo(exception)
    }
}
