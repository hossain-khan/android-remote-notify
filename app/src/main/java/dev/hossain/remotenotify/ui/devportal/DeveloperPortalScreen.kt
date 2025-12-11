package dev.hossain.remotenotify.ui.devportal

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.hossain.remotenotify.BuildConfig
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

/**
 * Developer Portal screen for debug builds only.
 * Provides tools to test and simulate app features without waiting for actual device conditions.
 */
@Parcelize
data object DeveloperPortalScreen : Screen {
    data class State(
        val currentBatteryLevel: Int,
        val currentStorageGb: Long,
        val maxStorageGb: Long,
        val buildVersion: String,
        val isSimulating: Boolean,
        val simulationResult: String?,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object GoBack : Event()

        data class SimulateBatteryAlert(
            val simulatedLevel: Int,
        ) : Event()

        data class SimulateStorageAlert(
            val simulatedStorageGb: Int,
        ) : Event()
    }
}

@AssistedInject
class DeveloperPortalPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val analytics: Analytics,
        private val batteryMonitor: BatteryMonitor,
        private val storageMonitor: StorageMonitor,
        private val repository: RemoteAlertRepository,
        private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
    ) : Presenter<DeveloperPortalScreen.State> {
        @Composable
        override fun present(): DeveloperPortalScreen.State {
            val scope = rememberCoroutineScope()
            var isSimulating by remember { mutableStateOf(false) }
            var simulationResult by remember { mutableStateOf<String?>(null) }

            LaunchedImpressionEffect {
                analytics.logScreenView(DeveloperPortalScreen::class)
            }

            // Get current device status
            val currentBatteryLevel = batteryMonitor.getBatteryLevel()
            val currentStorageGb = storageMonitor.getAvailableStorageInGB()
            val maxStorageGb = remember { ((currentStorageGb + 9) / 10) * 10 } // Round up to nearest 10

            val buildVersion =
                buildString {
                    append("v")
                    append(BuildConfig.VERSION_NAME)
                    append(" (")
                    append(BuildConfig.GIT_COMMIT_HASH)
                    append(")")
                }

            return DeveloperPortalScreen.State(
                currentBatteryLevel = currentBatteryLevel,
                currentStorageGb = currentStorageGb,
                maxStorageGb = maxStorageGb,
                buildVersion = buildVersion,
                isSimulating = isSimulating,
                simulationResult = simulationResult,
            ) { event ->
                when (event) {
                    DeveloperPortalScreen.Event.GoBack -> {
                        navigator.pop()
                    }

                    is DeveloperPortalScreen.Event.SimulateBatteryAlert -> {
                        scope.launch {
                            isSimulating = true
                            simulationResult = null
                            try {
                                Timber.d("Simulating battery alert at ${event.simulatedLevel}%")

                                // Create test battery alert with simulated value
                                val testAlert =
                                    RemoteAlert.BatteryAlert(
                                        alertId = -1L, // Test ID
                                        batteryPercentage = 20, // Threshold
                                        currentBatteryLevel = event.simulatedLevel,
                                    )

                                // Send to all configured notifiers
                                var successCount = 0
                                var failCount = 0

                                notifiers.forEach { notifier ->
                                    if (notifier.hasValidConfig()) {
                                        val result = notifier.sendNotification(testAlert)
                                        if (result) {
                                            successCount++
                                            Timber.d("✓ Sent test battery alert via ${notifier.notifierType}")
                                        } else {
                                            failCount++
                                            Timber.e("✗ Failed to send test battery alert via ${notifier.notifierType}")
                                        }
                                    }
                                }

                                simulationResult =
                                    if (successCount > 0) {
                                        "✓ Battery alert sent! $successCount succeeded, $failCount failed"
                                    } else {
                                        "✗ No notifications sent. Configure at least one channel first."
                                    }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to simulate battery alert")
                                simulationResult = "✗ Error: ${e.message}"
                            } finally {
                                isSimulating = false
                            }
                        }
                    }

                    is DeveloperPortalScreen.Event.SimulateStorageAlert -> {
                        scope.launch {
                            isSimulating = true
                            simulationResult = null
                            try {
                                Timber.d("Simulating storage alert at ${event.simulatedStorageGb}GB")

                                // Create test storage alert with simulated value
                                val testAlert =
                                    RemoteAlert.StorageAlert(
                                        alertId = -1L, // Test ID
                                        storageMinSpaceGb = 10, // Threshold
                                        currentStorageGb = event.simulatedStorageGb.toDouble(),
                                    )

                                // Send to all configured notifiers
                                var successCount = 0
                                var failCount = 0

                                notifiers.forEach { notifier ->
                                    if (notifier.hasValidConfig()) {
                                        val result = notifier.sendNotification(testAlert)
                                        if (result) {
                                            successCount++
                                            Timber.d("✓ Sent test storage alert via ${notifier.notifierType}")
                                        } else {
                                            failCount++
                                            Timber.e("✗ Failed to send test storage alert via ${notifier.notifierType}")
                                        }
                                    }
                                }

                                simulationResult =
                                    if (successCount > 0) {
                                        "✓ Storage alert sent! $successCount succeeded, $failCount failed"
                                    } else {
                                        "✗ No notifications sent. Configure at least one channel first."
                                    }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to simulate storage alert")
                                simulationResult = "✗ Error: ${e.message}"
                            } finally {
                                isSimulating = false
                            }
                        }
                    }
                }
            }
        }

        @CircuitInject(DeveloperPortalScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): DeveloperPortalPresenter
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(DeveloperPortalScreen::class, AppScope::class)
@Composable
fun DeveloperPortalUi(
    state: DeveloperPortalScreen.State,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar when simulation result changes
    state.simulationResult?.let { result ->
        scope.launch {
            snackbarHostState.showSnackbar(result)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("🔧 Developer Portal") },
                navigationIcon = {
                    IconButton(onClick = {
                        state.eventSink(DeveloperPortalScreen.Event.GoBack)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                        )
                    }
                },
            )
        },
    ) { contentPaddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPaddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            // Warning banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Debug Mode",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = "Debug Build Only",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            text = "Test alerts without waiting for actual device conditions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            // Build info card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Build Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Version: ${state.buildVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Device Simulation Section
            DeviceSimulationCard(
                state = state,
                modifier = Modifier.fillMaxWidth(),
            )

            // Placeholder cards for upcoming features
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚙️ WorkManager Testing",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Monitor periodic health checks and trigger immediate runs",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "\ud83d\udea7 Implementation in progress",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "\ud83d\udea7 Coming Soon",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text =
                            "\u2022 Notification Channel Testing\\n" +
                                "\u2022 Alert Configuration Overview\\n" +
                                "\u2022 Log Management\\n" +
                                "\u2022 Battery Optimization Testing",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DeviceSimulationCard(
    state: DeveloperPortalScreen.State,
    modifier: Modifier = Modifier,
) {
    var batterySliderValue by remember { mutableFloatStateOf(state.currentBatteryLevel.toFloat()) }
    var storageSliderValue by remember { mutableFloatStateOf(state.currentStorageGb.toFloat()) }

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📱 Device Simulation",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Test notifications with simulated values",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Battery Simulation
            Text(
                text = "🔋 Battery Level",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Simulated: ${batterySliderValue.toInt()}% (Actual: ${state.currentBatteryLevel}%)",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = batterySliderValue,
                onValueChange = { batterySliderValue = it },
                valueRange = 0f..100f,
                enabled = !state.isSimulating,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("0%", style = MaterialTheme.typography.bodySmall)
                Text("100%", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    state.eventSink(
                        DeveloperPortalScreen.Event.SimulateBatteryAlert(batterySliderValue.toInt()),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSimulating,
            ) {
                if (state.isSimulating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Simulate Battery Alert")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Storage Simulation
            Text(
                text = "💾 Storage Space",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Simulated: ${storageSliderValue.toInt()} GB (Actual: ${state.currentStorageGb} GB)",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = storageSliderValue,
                onValueChange = { storageSliderValue = it },
                valueRange = 1f..state.maxStorageGb.toFloat(),
                enabled = !state.isSimulating,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("1 GB", style = MaterialTheme.typography.bodySmall)
                Text("${state.maxStorageGb} GB", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    state.eventSink(
                        DeveloperPortalScreen.Event.SimulateStorageAlert(storageSliderValue.toInt()),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSimulating,
            ) {
                if (state.isSimulating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Simulate Storage Alert")
                }
            }

            if (state.isSimulating) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⏳ Sending notifications...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun DeveloperPortalScreenPreview() {
    val sampleState =
        DeveloperPortalScreen.State(
            currentBatteryLevel = 75,
            currentStorageGb = 45,
            maxStorageGb = 96,
            buildVersion = "v1.17.0 (abc1234)",
            isSimulating = false,
            simulationResult = null,
            eventSink = {},
        )
    ComposeAppTheme {
        DeveloperPortalUi(state = sampleState)
    }
}
