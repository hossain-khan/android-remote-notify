package dev.hossain.remotenotify.notifier

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [NotifierType] enum class.
 */
class NotifierTypeTest {
    @Test
    fun `email notifier has correct display name`() {
        assertThat(NotifierType.EMAIL.displayName).isEqualTo("Email")
    }

    @Test
    fun `telegram notifier has correct display name`() {
        assertThat(NotifierType.TELEGRAM.displayName).isEqualTo("Telegram")
    }

    @Test
    fun `twilio notifier has correct display name`() {
        assertThat(NotifierType.TWILIO.displayName).isEqualTo("Twilio")
    }

    @Test
    fun `webhook rest api notifier has correct display name`() {
        assertThat(NotifierType.WEBHOOK_REST_API.displayName).isEqualTo("Webhook")
    }

    @Test
    fun `webhook slack workflow notifier has correct display name`() {
        assertThat(NotifierType.WEBHOOK_SLACK_WORKFLOW.displayName).isEqualTo("Slack")
    }

    @Test
    fun `webhook discord notifier has correct display name`() {
        assertThat(NotifierType.WEBHOOK_DISCORD.displayName).isEqualTo("Discord")
    }

    @Test
    fun `all notifier types have non-empty display names`() {
        NotifierType.entries.forEach { notifierType ->
            assertThat(notifierType.displayName).isNotEmpty()
        }
    }

    @Test
    fun `notifier types count is correct`() {
        // This test ensures we don't accidentally remove or add notifier types without updating tests
        assertThat(NotifierType.entries.size).isEqualTo(6)
    }
}
