package dev.hossain.remotenotify.notifier

import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.RemoteAlert
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Unit tests for [NotificationSender] extension functions.
 */
class NotificationSenderExtTest {
    /**
     * Test implementation of [NotificationSender] for testing purposes.
     */
    private class TestNotificationSender(
        override val notifierType: NotifierType,
    ) : NotificationSender {
        override suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean = true

        override suspend fun hasValidConfig(): Boolean = true

        override suspend fun saveConfig(alertMediumConfig: AlertMediumConfig) {
            // No-op for test
        }

        override suspend fun getConfig(): AlertMediumConfig =
            AlertMediumConfig.TelegramConfig(
                botToken = "test",
                chatId = "test",
            )

        override suspend fun clearConfig() {
            // No-op for test
        }

        override suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            ConfigValidationResult(isValid = true, errors = emptyMap())
    }

    @Test
    fun `of extension finds sender by notifier type`() {
        val emailSender = TestNotificationSender(NotifierType.EMAIL)
        val telegramSender = TestNotificationSender(NotifierType.TELEGRAM)
        val twilioSender = TestNotificationSender(NotifierType.TWILIO)

        val senders = setOf(emailSender, telegramSender, twilioSender)

        assertThat(senders.of(NotifierType.EMAIL)).isEqualTo(emailSender)
        assertThat(senders.of(NotifierType.TELEGRAM)).isEqualTo(telegramSender)
        assertThat(senders.of(NotifierType.TWILIO)).isEqualTo(twilioSender)
    }

    @Test
    fun `of extension throws exception when sender not found`() {
        val emailSender = TestNotificationSender(NotifierType.EMAIL)
        val senders = setOf(emailSender)

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                senders.of(NotifierType.TELEGRAM)
            }

        assertThat(exception.message).contains("Sender for notifier type not found")
        assertThat(exception.message).contains("TELEGRAM")
    }

    @Test
    fun `of extension throws exception for empty set`() {
        val senders = emptySet<NotificationSender>()

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                senders.of(NotifierType.EMAIL)
            }

        assertThat(exception.message).contains("Sender for notifier type not found")
    }

    @Test
    fun `of extension finds correct sender from all notifier types`() {
        // Create a sender for each notifier type
        val senders =
            NotifierType.entries
                .map { notifierType ->
                    TestNotificationSender(notifierType)
                }.toSet()

        // Verify each type can be found
        NotifierType.entries.forEach { notifierType ->
            val foundSender = senders.of(notifierType)
            assertThat(foundSender.notifierType).isEqualTo(notifierType)
        }
    }
}
