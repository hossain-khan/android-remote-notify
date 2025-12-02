package dev.hossain.remotenotify.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.effects.LaunchedImpressionEffect
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.export.AppConfiguration
import dev.hossain.remotenotify.data.export.ConfigOperationResult
import dev.hossain.remotenotify.data.export.ConfigurationExporter
import dev.hossain.remotenotify.data.export.ConfigurationImporter
import dev.hossain.remotenotify.data.export.ImportValidationResult
import dev.hossain.remotenotify.data.export.getConfiguredNotifierTypes
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
data object BackupRestoreScreen : Screen {
    /**
     * Minimum password length required for export encryption.
     */
    @IgnoredOnParcel
    const val MIN_PASSWORD_LENGTH = 6

    data class State(
        val isLoading: Boolean = false,
        val message: String? = null,
        val showExportDialog: Boolean = false,
        val showImportPasswordDialog: Boolean = false,
        val showImportConfirmDialog: Boolean = false,
        val pendingImportConfig: AppConfiguration? = null,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object GoBack : Event()

        data object ShowExportDialog : Event()

        data object DismissExportDialog : Event()

        data class ExportConfig(
            val password: String,
            val uri: Uri,
        ) : Event()

        data class ImportFileSelected(
            val uri: Uri,
        ) : Event()

        data class PasswordEntered(
            val password: String,
        ) : Event()

        data object DismissImportPasswordDialog : Event()

        data object ConfirmImport : Event()

        data object DismissImportConfirmDialog : Event()

        data object ClearMessage : Event()
    }
}

@AssistedInject
class BackupRestorePresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val configurationExporter: ConfigurationExporter,
        private val configurationImporter: ConfigurationImporter,
        private val analytics: Analytics,
    ) : Presenter<BackupRestoreScreen.State> {
        @Composable
        override fun present(): BackupRestoreScreen.State {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()

            var isLoading by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf<String?>(null) }
            var showExportDialog by remember { mutableStateOf(false) }
            var showImportPasswordDialog by remember { mutableStateOf(false) }
            var showImportConfirmDialog by remember { mutableStateOf(false) }
            var pendingImportConfig by remember { mutableStateOf<AppConfiguration?>(null) }
            var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
            var pendingPassword by remember { mutableStateOf("") }

            LaunchedImpressionEffect {
                analytics.logScreenView(BackupRestoreScreen::class)
            }

            return BackupRestoreScreen.State(
                isLoading = isLoading,
                message = message,
                showExportDialog = showExportDialog,
                showImportPasswordDialog = showImportPasswordDialog,
                showImportConfirmDialog = showImportConfirmDialog,
                pendingImportConfig = pendingImportConfig,
            ) { event ->
                when (event) {
                    BackupRestoreScreen.Event.GoBack -> {
                        navigator.pop()
                    }

                    BackupRestoreScreen.Event.ShowExportDialog -> {
                        showExportDialog = true
                    }

                    BackupRestoreScreen.Event.DismissExportDialog -> {
                        showExportDialog = false
                    }

                    is BackupRestoreScreen.Event.ExportConfig -> {
                        showExportDialog = false
                        isLoading = true
                        scope.launch {
                            val result = configurationExporter.exportConfiguration(event.password)
                            result.fold(
                                onSuccess = { json ->
                                    try {
                                        context.contentResolver.openOutputStream(event.uri)?.use { outputStream ->
                                            outputStream.write(json.toByteArray(Charsets.UTF_8))
                                        }
                                        message = "Configuration exported successfully"
                                        analytics.logConfigExport(success = true)
                                    } catch (e: Exception) {
                                        Timber.e(e, "Failed to write export file")
                                        message = "Failed to save file: ${e.message}"
                                        analytics.logConfigExport(success = false)
                                    }
                                },
                                onFailure = { e ->
                                    message = "Export failed: ${e.message}"
                                    analytics.logConfigExport(success = false)
                                },
                            )
                            isLoading = false
                        }
                    }

                    is BackupRestoreScreen.Event.ImportFileSelected -> {
                        isLoading = true
                        scope.launch {
                            try {
                                val json =
                                    context.contentResolver.openInputStream(event.uri)?.use { inputStream ->
                                        inputStream.bufferedReader().readText()
                                    }
                                if (json == null) {
                                    message = "Failed to read file"
                                    isLoading = false
                                    return@launch
                                }

                                when (val validationResult = configurationImporter.parseAndValidate(json)) {
                                    is ImportValidationResult.Valid -> {
                                        pendingImportConfig = validationResult.configuration
                                        pendingImportUri = event.uri
                                        showImportPasswordDialog = true
                                    }
                                    is ImportValidationResult.Invalid -> {
                                        message = "Invalid configuration: ${validationResult.errors.joinToString(separator = "; ")}"
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to read import file")
                                message = "Failed to read file: ${e.message}"
                            }
                            isLoading = false
                        }
                    }

                    is BackupRestoreScreen.Event.PasswordEntered -> {
                        pendingPassword = event.password
                        val config = pendingImportConfig
                        if (config != null) {
                            if (configurationImporter.testPassword(config, event.password)) {
                                showImportPasswordDialog = false
                                showImportConfirmDialog = true
                            } else {
                                message = "Invalid password. Please try again."
                            }
                        }
                    }

                    BackupRestoreScreen.Event.DismissImportPasswordDialog -> {
                        showImportPasswordDialog = false
                        pendingImportConfig = null
                        pendingImportUri = null
                        pendingPassword = ""
                    }

                    BackupRestoreScreen.Event.ConfirmImport -> {
                        showImportConfirmDialog = false
                        val config = pendingImportConfig
                        if (config != null) {
                            isLoading = true
                            scope.launch {
                                val result =
                                    configurationImporter.importConfiguration(
                                        configuration = config,
                                        password = pendingPassword,
                                        replaceExisting = true,
                                    )
                                when (result) {
                                    is ConfigOperationResult.Success -> {
                                        message = "Configuration imported successfully"
                                        val notifierCount = config.notifiers.getConfiguredNotifierTypes().size
                                        analytics.logConfigImport(
                                            success = true,
                                            alertsCount = config.alerts.size,
                                            notifiersCount = notifierCount,
                                        )
                                    }
                                    is ConfigOperationResult.Error -> {
                                        message = "Import failed: ${result.message}"
                                        analytics.logConfigImport(success = false)
                                    }
                                }
                                pendingImportConfig = null
                                pendingImportUri = null
                                pendingPassword = ""
                                isLoading = false
                            }
                        }
                    }

                    BackupRestoreScreen.Event.DismissImportConfirmDialog -> {
                        showImportConfirmDialog = false
                        pendingImportConfig = null
                        pendingImportUri = null
                        pendingPassword = ""
                    }

                    BackupRestoreScreen.Event.ClearMessage -> {
                        message = null
                    }
                }
            }
        }

        @CircuitInject(BackupRestoreScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): BackupRestorePresenter
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(BackupRestoreScreen::class, AppScope::class)
@Composable
fun BackupRestoreScreenUi(
    state: BackupRestoreScreen.State,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }

    // File picker for export
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri != null && exportPassword.isNotBlank()) {
                state.eventSink(BackupRestoreScreen.Event.ExportConfig(exportPassword, uri))
                exportPassword = ""
            }
        }

    // File picker for import
    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                state.eventSink(BackupRestoreScreen.Event.ImportFileSelected(uri))
            }
        }

    // Show snackbar when message is set
    LaunchedEffect(state.message) {
        state.message?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
                state.eventSink(BackupRestoreScreen.Event.ClearMessage)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = {
                        state.eventSink(BackupRestoreScreen.Event.GoBack)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPaddingValues ->
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(contentPaddingValues),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Export Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Export Configuration",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            text =
                                "Export your alerts and notification channel settings to a file. " +
                                    "Sensitive data like API keys will be encrypted with your password.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { state.eventSink(BackupRestoreScreen.Event.ShowExportDialog) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading,
                        ) {
                            Text("Export to File")
                        }
                    }
                }

                // Import Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Import Configuration",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            text =
                                "Import configuration from a previously exported file. " +
                                    "This will replace your current settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isLoading,
                        ) {
                            Text("Import from File")
                        }
                    }
                }

                // Security Warning
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Column {
                            Text(
                                text = "Security Notice",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text =
                                    "Keep your export file secure. While sensitive data is encrypted, " +
                                        "the file contains your configuration. Use a strong password.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            // Loading overlay
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Export Password Dialog
    if (state.showExportDialog) {
        AlertDialog(
            onDismissRequest = { state.eventSink(BackupRestoreScreen.Event.DismissExportDialog) },
            title = { Text("Enter Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a password to encrypt sensitive data in the export file.")
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val timestamp =
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                                .format(Date())
                        exportLauncher.launch("remote_notify_backup_$timestamp.json")
                    },
                    enabled = exportPassword.length >= BackupRestoreScreen.MIN_PASSWORD_LENGTH,
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    exportPassword = ""
                    state.eventSink(BackupRestoreScreen.Event.DismissExportDialog)
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Import Password Dialog
    if (state.showImportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { state.eventSink(BackupRestoreScreen.Event.DismissImportPasswordDialog) },
            title = { Text("Enter Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the password used when exporting this configuration.")
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        state.eventSink(BackupRestoreScreen.Event.PasswordEntered(importPassword))
                    },
                    enabled = importPassword.isNotBlank(),
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    importPassword = ""
                    state.eventSink(BackupRestoreScreen.Event.DismissImportPasswordDialog)
                }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Import Confirmation Dialog
    if (state.showImportConfirmDialog && state.pendingImportConfig != null) {
        val config = state.pendingImportConfig
        AlertDialog(
            onDismissRequest = { state.eventSink(BackupRestoreScreen.Event.DismissImportConfirmDialog) },
            title = { Text("Confirm Import") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will replace your current configuration with:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• ${config.alerts.size} alert(s)")
                    Text("• ${config.notifiers.getConfiguredNotifierTypes().size} notification channel(s)")
                    config.preferences.workerIntervalMinutes?.let {
                        Text("• Check interval: $it minutes")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Warning: This will delete your existing alerts and notification settings.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        importPassword = ""
                        state.eventSink(BackupRestoreScreen.Event.ConfirmImport)
                    },
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    importPassword = ""
                    state.eventSink(BackupRestoreScreen.Event.DismissImportConfirmDialog)
                }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun BackupRestoreScreenPreview() {
    val sampleState =
        BackupRestoreScreen.State(
            isLoading = false,
            message = null,
            showExportDialog = false,
            showImportPasswordDialog = false,
            showImportConfirmDialog = false,
            pendingImportConfig = null,
            eventSink = {},
        )
    ComposeAppTheme {
        BackupRestoreScreenUi(state = sampleState)
    }
}

@Composable
@PreviewLightDark
private fun BackupRestoreScreenLoadingPreview() {
    val sampleState =
        BackupRestoreScreen.State(
            isLoading = true,
            message = null,
            showExportDialog = false,
            showImportPasswordDialog = false,
            showImportConfirmDialog = false,
            pendingImportConfig = null,
            eventSink = {},
        )
    ComposeAppTheme {
        BackupRestoreScreenUi(state = sampleState)
    }
}
