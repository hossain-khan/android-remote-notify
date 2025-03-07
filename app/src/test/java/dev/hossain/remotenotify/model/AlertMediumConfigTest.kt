package dev.hossain.remotenotify.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AlertMediumConfigTest {
    @Test
    fun `configPreviewText for TelegramConfig with short chatId`() {
        val config =
            AlertMediumConfig.TelegramConfig(
                botToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11",
                chatId = "@mychannel",
            )
        assertThat(config.configPreviewText()).isEqualTo("@mychannel")
    }

    @Test
    fun `configPreviewText for TelegramConfig with long chatId`() {
        val config =
            AlertMediumConfig.TelegramConfig(
                botToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11",
                chatId = "-100123456789012345",
            )
        assertThat(config.configPreviewText()).isEqualTo("-1001...9012345")
    }

    @Test
    fun `configPreviewText for WebhookConfig with short URL`() {
        val config =
            AlertMediumConfig.WebhookConfig(
                url = "https://example.com/api",
            )
        assertThat(config.configPreviewText()).isEqualTo("example.com/api")
    }

    @Test
    fun `configPreviewText for WebhookConfig with long URL`() {
        val config =
            AlertMediumConfig.WebhookConfig(
                url = "https://very-long-domain-name.example.com/webhooks/notifications/endpoint",
            )
        assertThat(config.configPreviewText()).isEqualTo("very-long-do...endpoint")
    }

    @Test
    fun `configPreviewText for WebhookConfig with HTTP URL`() {
        val config =
            AlertMediumConfig.WebhookConfig(
                url = "http://api.example.com/very/long/path/to/webhook/endpoint",
            )
        assertThat(config.configPreviewText()).isEqualTo("api.example....endpoint")
    }

    @Test
    fun `configPreviewText for TwilioConfig with short phone number`() {
        val config =
            AlertMediumConfig.TwilioConfig(
                accountSid = "ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                authToken = "your_auth_token",
                fromPhone = "+12345678901",
                toPhone = "+15551234",
            )
        assertThat(config.configPreviewText()).isEqualTo("+15551234")
    }

    @Test
    fun `configPreviewText for TwilioConfig with long phone number`() {
        val config =
            AlertMediumConfig.TwilioConfig(
                accountSid = "ACXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX",
                authToken = "your_auth_token",
                fromPhone = "+12345678901",
                toPhone = "+1234567890123456",
            )
        assertThat(config.configPreviewText()).isEqualTo("+12...3456")
    }

    @Test
    fun `configPreviewText for EmailConfig with short email`() {
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "key-xxxxxxxxxxxxxxxxxxxxx",
                domain = "mail.example.com",
                fromEmail = "from@example.com",
                toEmail = "user@example.com",
            )
        assertThat(config.configPreviewText()).isEqualTo("user@example.com")
    }

    @Test
    fun `configPreviewText for EmailConfig with long email but short parts`() {
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "key-xxxxxxxxxxxxxxxxxxxxx",
                domain = "mail.example.com",
                fromEmail = "from@example.com",
                toEmail = "user@very-long-domain-name.example.com",
            )
        assertThat(config.configPreviewText()).isEqualTo("user@...ple.com")
    }

    @Test
    fun `configPreviewText for EmailConfig with long username part`() {
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "key-xxxxxxxxxxxxxxxxxxxxx",
                domain = "mail.example.com",
                fromEmail = "from@example.com",
                toEmail = "very.long.username.with.dots@example.com",
            )
        assertThat(config.configPreviewText()).isEqualTo("very....@...ple.com")
    }

    @Test
    fun `configPreviewText for EmailConfig with long domain part`() {
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "key-xxxxxxxxxxxxxxxxxxxxx",
                domain = "mail.example.com",
                fromEmail = "from@example.com",
                toEmail = "user@very.long.subdomain.with.many.parts.example.com",
            )
        assertThat(config.configPreviewText()).isEqualTo("user@...ple.com")
    }

    @Test
    fun `configPreviewText for EmailConfig with both parts long`() {
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "key-xxxxxxxxxxxxxxxxxxxxx",
                domain = "mail.example.com",
                fromEmail = "from@example.com",
                toEmail = "very.long.email.address@very.long.subdomain.domain.com",
            )
        assertThat(config.configPreviewText()).isEqualTo("very....@...ain.com")
    }

    @Test
    fun `configPreviewText for EmailConfig with invalid email (no @)`() {
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "key-xxxxxxxxxxxxxxxxxxxxx",
                domain = "mail.example.com",
                fromEmail = "from@example.com",
                toEmail = "this-is-not-a-valid-email-address-without-at-symbol",
            )
        assertThat(config.configPreviewText()).isEqualTo("this-is-no...-symbol")
    }

    @Test
    fun `configPreviewText for EmailConfig with @ at start`() {
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "key-xxxxxxxxxxxxxxxxxxxxx",
                domain = "mail.example.com",
                fromEmail = "from@example.com",
                toEmail = "@invalid-email-with-at-symbol-at-beginning",
            )
        assertThat(config.configPreviewText()).isEqualTo("@invalid-e...ginning")
    }

    @Test
    fun `configPreviewText for EmailConfig with @ at end`() {
        val config =
            AlertMediumConfig.EmailConfig(
                apiKey = "key-xxxxxxxxxxxxxxxxxxxxx",
                domain = "mail.example.com",
                fromEmail = "from@example.com",
                toEmail = "invalid-email-with-at-symbol-at-end@",
            )
        assertThat(config.configPreviewText()).isEqualTo("invalid-em...at-end@")
    }
}
