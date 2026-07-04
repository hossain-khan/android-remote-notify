package dev.hossain.remotenotify.ui.backup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.hossain.remotenotify.data.export.AlertConfig
import dev.hossain.remotenotify.data.export.AppConfiguration
import dev.hossain.remotenotify.data.export.AppPreferences
import dev.hossain.remotenotify.data.export.NotifierConfigs
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.theme.ComposeAppTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class BackupRestoreUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun idleStateRendersExportAndImportActionsAndSecurityNotice() {
        val eventSink = mockk<(BackupRestoreScreen.Event) -> Unit>(relaxed = true)
        composeRule.setContent {
            ComposeAppTheme {
                BackupRestoreScreenUi(
                    state =
                        BackupRestoreScreen.State(
                            isLoading = false,
                            message = null,
                            eventSink = eventSink,
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Backup & Restore").assertIsDisplayed()
        composeRule.onNodeWithText("Export Configuration").assertIsDisplayed()
        composeRule.onNodeWithText("Export to File").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Import Configuration").assertIsDisplayed()
        composeRule.onNodeWithText("Import from File").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithText("Security Notice").assertIsDisplayed()
    }

    @Test
    fun loadingStateDisablesButtons() {
        val eventSink = mockk<(BackupRestoreScreen.Event) -> Unit>(relaxed = true)
        composeRule.setContent {
            ComposeAppTheme {
                BackupRestoreScreenUi(
                    state =
                        BackupRestoreScreen.State(
                            isLoading = true,
                            message = null,
                            eventSink = eventSink,
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Export to File").assertIsNotEnabled()
        composeRule.onNodeWithText("Import from File").assertIsNotEnabled()
    }

    @Test
    fun exportDialogDemandsMinimumPasswordLength() {
        val eventSink = mockk<(BackupRestoreScreen.Event) -> Unit>(relaxed = true)
        composeRule.setContent {
            ComposeAppTheme {
                BackupRestoreScreenUi(
                    state =
                        BackupRestoreScreen.State(
                            isLoading = false,
                            showExportDialog = true,
                            eventSink = eventSink,
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Enter Password").assertIsDisplayed()

        // Export button should be disabled for password < MIN_PASSWORD_LENGTH (6)
        composeRule.onNodeWithText("Export").assertIsNotEnabled()

        // Type short password (5 chars)
        composeRule.onNodeWithText("Password").performTextInput("12345")
        composeRule.onNodeWithText("Export").assertIsNotEnabled()

        // Type 6th char
        composeRule.onNodeWithText("Password").performTextInput("6")
        composeRule.onNodeWithText("Export").assertIsEnabled()

        // Cancel clicks raise DismissExportDialog
        composeRule.onNodeWithText("Cancel").performClick()
        verify { eventSink(BackupRestoreScreen.Event.DismissExportDialog) }
    }

    @Test
    fun importPasswordDialogShowsErrorAndInteractiveControls() {
        val eventSink = mockk<(BackupRestoreScreen.Event) -> Unit>(relaxed = true)
        composeRule.setContent {
            ComposeAppTheme {
                BackupRestoreScreenUi(
                    state =
                        BackupRestoreScreen.State(
                            isLoading = false,
                            showImportPasswordDialog = true,
                            message = "Invalid password. Please try again.",
                            eventSink = eventSink,
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Enter Password").assertIsDisplayed()
        composeRule.onNodeWithText("Invalid password. Please try again.").assertIsDisplayed()

        // Clicking cancel triggers event
        composeRule.onNodeWithText("Cancel").performClick()
        verify { eventSink(BackupRestoreScreen.Event.DismissImportPasswordDialog) }
    }

    @Test
    fun importConfirmationDialogShowsCorrectSummaryOfPendingConfig() {
        val eventSink = mockk<(BackupRestoreScreen.Event) -> Unit>(relaxed = true)
        val pendingConfig =
            AppConfiguration(
                version = AppConfiguration.CURRENT_VERSION,
                alerts =
                    listOf(
                        AlertConfig(type = AlertType.BATTERY, batteryPercentage = 15),
                        AlertConfig(type = AlertType.STORAGE, storageMinSpaceGb = 5),
                    ),
                notifiers = NotifierConfigs(),
                preferences = AppPreferences(workerIntervalMinutes = 30),
            )

        composeRule.setContent {
            ComposeAppTheme {
                BackupRestoreScreenUi(
                    state =
                        BackupRestoreScreen.State(
                            isLoading = false,
                            showImportConfirmDialog = true,
                            pendingImportConfig = pendingConfig,
                            eventSink = eventSink,
                        ),
                )
            }
        }

        composeRule.onNodeWithText("Confirm Import").assertIsDisplayed()
        composeRule.onNodeWithText("This will replace your current configuration with:").assertIsDisplayed()
        composeRule.onNodeWithText("• 2 alert(s)").assertIsDisplayed()
        composeRule.onNodeWithText("• 0 notification channel(s)").assertIsDisplayed()
        composeRule.onNodeWithText("• Check interval: 30 minutes").assertIsDisplayed()
        composeRule.onNodeWithText("Warning: This will delete your existing alerts and notification settings.").assertIsDisplayed()

        composeRule.onNodeWithText("Import").performClick()
        verify { eventSink(BackupRestoreScreen.Event.ConfirmImport) }
    }
}
