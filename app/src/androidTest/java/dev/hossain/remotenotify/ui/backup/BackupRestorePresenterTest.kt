package dev.hossain.remotenotify.ui.backup

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import com.slack.circuit.runtime.Navigator
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.export.AppConfiguration
import dev.hossain.remotenotify.data.export.AppPreferences
import dev.hossain.remotenotify.data.export.ConfigOperationResult
import dev.hossain.remotenotify.data.export.ConfigurationExporter
import dev.hossain.remotenotify.data.export.ConfigurationImporter
import dev.hossain.remotenotify.data.export.ImportValidationResult
import dev.hossain.remotenotify.data.export.NotifierConfigs
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

class BackupRestorePresenterTest {
    @get:Rule
    val composeRule = createComposeRule()

    @MockK
    lateinit var navigator: Navigator

    @MockK
    lateinit var configurationExporter: ConfigurationExporter

    @MockK
    lateinit var configurationImporter: ConfigurationImporter

    @MockK
    lateinit var analytics: Analytics

    @MockK
    lateinit var mockContext: Context

    @MockK
    lateinit var mockContentResolver: ContentResolver

    private val mockUri = Uri.parse("content://dummy/backup.json")
    private val mockAppConfig =
        AppConfiguration(
            version = AppConfiguration.CURRENT_VERSION,
            exportedAt = 123456789L,
            deviceName = "Test Device",
            alerts = emptyList(),
            notifiers = NotifierConfigs(),
            preferences = AppPreferences(),
        )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { navigator.pop(any()) } returns null
        every { navigator.goTo(any()) } returns true

        every { mockContext.contentResolver } returns mockContentResolver
        every { mockContext.applicationContext } returns mockContext

        coEvery { analytics.logScreenView(any()) } just runs
        coEvery { analytics.logConfigExport(any()) } just runs
        coEvery { analytics.logConfigImport(any(), any(), any()) } just runs
        coEvery { analytics.logConfigImport(any()) } just runs
    }

    @Test
    fun screenViewIsLoggedOnPresenterLaunch() {
        renderPresenter()
        coVerify { analytics.logScreenView(BackupRestoreScreen::class) }
    }

    @Test
    fun goBackEventPopsNavigator() {
        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.GoBack)
        }
        composeRule.waitForIdle()
        io.mockk.verify { navigator.pop() }
    }

    @Test
    fun showAndDismissExportDialogUpdateState() {
        val state = renderPresenter()
        assertFalse(currentState().showExportDialog)

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ShowExportDialog)
        }
        composeRule.waitForIdle()
        assertTrue(currentState().showExportDialog)

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.DismissExportDialog)
        }
        composeRule.waitForIdle()
        assertFalse(currentState().showExportDialog)
    }

    @Test
    fun exportConfigSuccessFlowWritesFileAndLogsSuccess() {
        coEvery { configurationExporter.exportConfiguration("password123") } returns Result.success("exported-json")
        val mockOutputStream = ByteArrayOutputStream()
        every { mockContentResolver.openOutputStream(mockUri) } returns mockOutputStream

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ExportConfig("password123", mockUri))
        }
        composeRule.waitForIdle()

        coVerify { configurationExporter.exportConfiguration("password123") }
        assertEquals("exported-json", mockOutputStream.toString("UTF-8"))
        assertEquals("Configuration exported successfully", currentState().message)
        coVerify { analytics.logConfigExport(success = true) }
    }

    @Test
    fun exportConfigFailureLogsAnalyticsAndShowsErrorMessage() {
        coEvery { configurationExporter.exportConfiguration("password123") } returns Result.failure(Exception("crypto failed"))

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ExportConfig("password123", mockUri))
        }
        composeRule.waitForIdle()

        coVerify { configurationExporter.exportConfiguration("password123") }
        assertEquals("Export failed: crypto failed", currentState().message)
        coVerify { analytics.logConfigExport(success = false) }
    }

    @Test
    fun exportConfigWriteErrorLogsAnalyticsAndShowsErrorMessage() {
        coEvery { configurationExporter.exportConfiguration("password123") } returns Result.success("exported-json")
        every { mockContentResolver.openOutputStream(mockUri) } throws IOException("disk full")

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ExportConfig("password123", mockUri))
        }
        composeRule.waitForIdle()

        assertEquals("Failed to save file: disk full", currentState().message)
        coVerify { analytics.logConfigExport(success = false) }
    }

    @Test
    fun importFileSelectedValidJsonTransitionsToPasswordDialog() {
        every { mockContentResolver.openInputStream(mockUri) } returns ByteArrayInputStream("json-content".toByteArray())
        every { configurationImporter.parseAndValidate("json-content") } returns ImportValidationResult.Valid(mockAppConfig)

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
        }
        composeRule.waitForIdle()

        assertTrue(currentState().showImportPasswordDialog)
        assertEquals(mockAppConfig, currentState().pendingImportConfig)
    }

    @Test
    fun importFileSelectedInvalidJsonShowsErrorMessage() {
        every { mockContentResolver.openInputStream(mockUri) } returns ByteArrayInputStream("bad-json-content".toByteArray())
        every { configurationImporter.parseAndValidate("bad-json-content") } returns
            ImportValidationResult.Invalid(listOf("missing version", "invalid layout"))

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
        }
        composeRule.waitForIdle()

        assertFalse(currentState().showImportPasswordDialog)
        assertEquals("Invalid configuration: missing version; invalid layout", currentState().message)
    }

    @Test
    fun importFileReadErrorShowsErrorMessage() {
        every { mockContentResolver.openInputStream(mockUri) } throws IOException("permission denied")

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
        }
        composeRule.waitForIdle()

        assertFalse(currentState().showImportPasswordDialog)
        assertEquals("Failed to read file: permission denied", currentState().message)
    }

    @Test
    fun passwordEnteredCorrectPasswordTransitionsToConfirmation() {
        // Setup initial pending import config state via file selection
        every { mockContentResolver.openInputStream(mockUri) } returns ByteArrayInputStream("json-content".toByteArray())
        every { configurationImporter.parseAndValidate("json-content") } returns ImportValidationResult.Valid(mockAppConfig)
        every { configurationImporter.testPassword(mockAppConfig, "correct-password") } returns true

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.PasswordEntered("correct-password"))
        }
        composeRule.waitForIdle()

        assertFalse(currentState().showImportPasswordDialog)
        assertTrue(currentState().showImportConfirmDialog)
    }

    @Test
    fun passwordEnteredIncorrectPasswordShowsError() {
        every { mockContentResolver.openInputStream(mockUri) } returns ByteArrayInputStream("json-content".toByteArray())
        every { configurationImporter.parseAndValidate("json-content") } returns ImportValidationResult.Valid(mockAppConfig)
        every { configurationImporter.testPassword(mockAppConfig, "wrong-password") } returns false

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.PasswordEntered("wrong-password"))
        }
        composeRule.waitForIdle()

        assertTrue(currentState().showImportPasswordDialog)
        assertFalse(currentState().showImportConfirmDialog)
        assertEquals("Invalid password. Please try again.", currentState().message)
    }

    @Test
    fun dismissImportPasswordDialogClearsPendingState() {
        every { mockContentResolver.openInputStream(mockUri) } returns ByteArrayInputStream("json-content".toByteArray())
        every { configurationImporter.parseAndValidate("json-content") } returns ImportValidationResult.Valid(mockAppConfig)

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.DismissImportPasswordDialog)
        }
        composeRule.waitForIdle()

        assertFalse(currentState().showImportPasswordDialog)
        assertNull(currentState().pendingImportConfig)
    }

    @Test
    fun confirmImportSuccessFlowImportsAndLogsSuccess() {
        every { mockContentResolver.openInputStream(mockUri) } returns ByteArrayInputStream("json-content".toByteArray())
        every { configurationImporter.parseAndValidate("json-content") } returns ImportValidationResult.Valid(mockAppConfig)
        every { configurationImporter.testPassword(mockAppConfig, "pass") } returns true
        coEvery { configurationImporter.importConfiguration(mockAppConfig, "pass", replaceExisting = true) } returns
            ConfigOperationResult.Success

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
            state.eventSink(BackupRestoreScreen.Event.PasswordEntered("pass"))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ConfirmImport)
        }
        composeRule.waitForIdle()

        coVerify { configurationImporter.importConfiguration(mockAppConfig, "pass", replaceExisting = true) }
        assertEquals("Configuration imported successfully", currentState().message)
        coVerify { analytics.logConfigImport(success = true, alertsCount = 0, notifiersCount = 0) }
        assertNull(currentState().pendingImportConfig)
    }

    @Test
    fun confirmImportErrorFlowShowsFailureAndLogsAnalytics() {
        every { mockContentResolver.openInputStream(mockUri) } returns ByteArrayInputStream("json-content".toByteArray())
        every { configurationImporter.parseAndValidate("json-content") } returns ImportValidationResult.Valid(mockAppConfig)
        every { configurationImporter.testPassword(mockAppConfig, "pass") } returns true
        coEvery { configurationImporter.importConfiguration(mockAppConfig, "pass", replaceExisting = true) } returns
            ConfigOperationResult.Error("decrypt error")

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
            state.eventSink(BackupRestoreScreen.Event.PasswordEntered("pass"))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ConfirmImport)
        }
        composeRule.waitForIdle()

        coVerify { configurationImporter.importConfiguration(mockAppConfig, "pass", replaceExisting = true) }
        assertEquals("Import failed: decrypt error", currentState().message)
        coVerify { analytics.logConfigImport(success = false) }
    }

    @Test
    fun dismissImportConfirmDialogClearsPendingState() {
        every { mockContentResolver.openInputStream(mockUri) } returns ByteArrayInputStream("json-content".toByteArray())
        every { configurationImporter.parseAndValidate("json-content") } returns ImportValidationResult.Valid(mockAppConfig)
        every { configurationImporter.testPassword(mockAppConfig, "pass") } returns true

        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(mockUri))
            state.eventSink(BackupRestoreScreen.Event.PasswordEntered("pass"))
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.DismissImportConfirmDialog)
        }
        composeRule.waitForIdle()

        assertFalse(currentState().showImportConfirmDialog)
        assertNull(currentState().pendingImportConfig)
    }

    @Test
    fun clearMessageEventResetsMessageToNull() {
        coEvery { configurationExporter.exportConfiguration("password123") } returns Result.failure(Exception("error"))
        val state = renderPresenter()
        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ExportConfig("password123", mockUri))
        }
        composeRule.waitForIdle()
        assertEquals("Export failed: error", currentState().message)

        composeRule.runOnIdle {
            state.eventSink(BackupRestoreScreen.Event.ClearMessage)
        }
        composeRule.waitForIdle()
        assertNull(currentState().message)
    }

    private var latestState by mutableStateOf<BackupRestoreScreen.State?>(null)

    private fun renderPresenter(): BackupRestoreScreen.State {
        val presenter =
            BackupRestorePresenter(
                navigator = navigator,
                configurationExporter = configurationExporter,
                configurationImporter = configurationImporter,
                analytics = analytics,
            )

        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides mockContext) {
                latestState = presenter.present()
            }
        }
        composeRule.waitForIdle()
        return currentState()
    }

    private fun currentState(): BackupRestoreScreen.State = requireNotNull(latestState)
}
