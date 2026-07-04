package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.theme.ComposeAppTheme
import org.junit.Rule
import org.junit.Test

class ConfigureNotificationMediumUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun webhookNotifierShowsSyntaxHighlightedPayloadPreviews() {
        composeTestRule.setContent {
            ComposeAppTheme {
                ConfigureNotificationMediumUi(
                    state =
                        testState(
                            notifierType = NotifierType.WEBHOOK_REST_API,
                            configValidationResult = ConfigValidationResult(isValid = true),
                            alertMediumConfig = AlertMediumConfig.WebhookConfig("https://example.com"),
                            sampleJsonPayload =
                                listOf(
                                    """{"alertType":"BATTERY","batteryLevel":10}""",
                                    """{"alertType":"STORAGE","availableStorageGb":2}""",
                                ),
                        ),
                )
            }
        }

        composeTestRule.onAllNodesWithTag("syntax-highlighted-code", useUnmergedTree = true).assertCountEquals(2)
        composeTestRule.onNodeWithText("Test Configuration").assertIsDisplayed()
    }

    @Test
    fun nonWebhookNotifierDoesNotShowSyntaxHighlightedPayloadPreview() {
        composeTestRule.setContent {
            ComposeAppTheme {
                ConfigureNotificationMediumUi(
                    state =
                        testState(
                            notifierType = NotifierType.TELEGRAM,
                            configValidationResult = ConfigValidationResult(isValid = false),
                            alertMediumConfig = AlertMediumConfig.TelegramConfig("bot-token", "chat-id"),
                            sampleJsonPayload = emptyList(),
                        ),
                )
            }
        }

        composeTestRule.onAllNodesWithTag("syntax-highlighted-code", useUnmergedTree = true).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("Test Configuration").assertCountEquals(0)
    }

    @Test
    fun validConfigurationShowsTestConfigurationButton() {
        composeTestRule.setContent {
            ComposeAppTheme {
                ConfigureNotificationMediumUi(
                    state =
                        testState(
                            notifierType = NotifierType.EMAIL,
                            configValidationResult = ConfigValidationResult(isValid = true),
                            alertMediumConfig =
                                AlertMediumConfig.EmailConfig(
                                    apiKey = "api-key",
                                    domain = "mg.example.com",
                                    fromEmail = "alerts@example.com",
                                    toEmail = "user@example.com",
                                ),
                            sampleJsonPayload = emptyList(),
                        ),
                )
            }
        }

        composeTestRule.onNodeWithText("Test Configuration").assertIsDisplayed()
    }

    private fun testState(
        notifierType: NotifierType,
        configValidationResult: ConfigValidationResult,
        alertMediumConfig: AlertMediumConfig?,
        sampleJsonPayload: List<String>,
    ) = ConfigureNotificationMediumScreen.State(
        notifierType = notifierType,
        isConfigured = alertMediumConfig != null,
        configValidationResult = configValidationResult,
        alertMediumConfig = alertMediumConfig,
        showValidationError = false,
        sampleJsonPayload = sampleJsonPayload,
        snackbarMessage = null,
        eventSink = {},
    )
}
