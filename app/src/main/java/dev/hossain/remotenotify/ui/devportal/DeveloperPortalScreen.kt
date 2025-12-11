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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.parcelize.Parcelize

/**
 * Developer Portal screen for debug builds only.
 * Provides tools to test and simulate app features without waiting for actual device conditions.
 */
@Parcelize
data object DeveloperPortalScreen : Screen {
    data class State(
        val currentBatteryLevel: Int,
        val currentStorageGb: Long,
        val buildVersion: String,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object GoBack : Event()
    }
}

@AssistedInject
class DeveloperPortalPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val analytics: Analytics,
        private val batteryMonitor: BatteryMonitor,
        private val storageMonitor: StorageMonitor,
    ) : Presenter<DeveloperPortalScreen.State> {
        @Composable
        override fun present(): DeveloperPortalScreen.State {
            LaunchedImpressionEffect {
                analytics.logScreenView(DeveloperPortalScreen::class)
            }

            // Get current device status
            val currentBatteryLevel = batteryMonitor.getBatteryLevel()
            val currentStorageGb = storageMonitor.getAvailableStorageInGB()

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
                buildVersion = buildVersion,
            ) { event ->
                when (event) {
                    DeveloperPortalScreen.Event.GoBack -> {
                        navigator.pop()
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
    Scaffold(
        modifier = modifier,
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
                            text = "This screen is not available in release builds",
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

            // Current device status card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 Current Device Status",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔋 Battery: ${state.currentBatteryLevel}%",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "💾 Storage: ${state.currentStorageGb} GB available",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Placeholder cards for upcoming features
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚧 Coming Soon",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Device Simulation Tools\n• WorkManager Testing\n• Notification Channel Testing\n• Log Management",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
            buildVersion = "v1.17.0 (abc1234)",
            eventSink = {},
        )
    ComposeAppTheme {
        DeveloperPortalUi(state = sampleState)
    }
}
