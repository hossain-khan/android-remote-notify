package dev.hossain.remotenotify.ui.addalert

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.effects.LaunchedImpressionEffect
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.toAlertType
import dev.hossain.remotenotify.model.toIconResId
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.utils.BatteryOptimizationHelper
import dev.hossain.remotenotify.utils.findActivity
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object AddNewRemoteAlertScreen : Screen {
    data class State(
        val showBatteryOptSheet: Boolean,
        val isBatteryOptimized: Boolean,
        val selectedAlertType: AlertType,
        val threshold: Int,
        val availableStorage: Int,
        val storageSliderMax: Int,
        val hideBatteryOptReminder: Boolean,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class SaveNotification(
            val notification: RemoteAlert,
        ) : Event()

        data object NavigateBack : Event()

        data object ShowBatteryOptimizationSheet : Event()

        data object DismissBatteryOptimizationSheet : Event()

        data object OpenBatterySettings : Event()

        data object HideBatteryOptimizationReminder : Event()

        data class UpdateAlertType(
            val alertType: AlertType,
        ) : Event()

        data class UpdateThreshold(
            val value: Int,
        ) : Event()
    }
}

class AddNewRemoteAlertPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val remoteAlertRepository: RemoteAlertRepository,
        private val storageMonitor: StorageMonitor,
        private val appPreferencesDataStore: AppPreferencesDataStore,
        private val analytics: Analytics,
    ) : Presenter<AddNewRemoteAlertScreen.State> {
        @Composable
        override fun present(): AddNewRemoteAlertScreen.State {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var showBatteryOptimizeSheet by remember { mutableStateOf(false) }
            var isBatteryOptimized by remember {
                mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
            }
            var hideBatteryOptReminder by remember { mutableStateOf(false) }

            var selectedType by remember { mutableStateOf(AlertType.BATTERY) }
            var threshold by remember { mutableIntStateOf(10) }

            val availableStorage = remember { storageMonitor.getAvailableStorageInGB().toInt() }

            /**
             * Round up to nearest 10 for slider max value.
             */
            val storageSliderMax =
                remember(availableStorage) {
                    ((availableStorage + 9) / 10) * 10
                }

            LaunchedImpressionEffect {
                analytics.logScreenView(AddNewRemoteAlertScreen::class)
            }

            // Update threshold if needed when switching types
            LaunchedEffect(selectedType) {
                if (selectedType == AlertType.STORAGE && threshold > storageSliderMax) {
                    threshold = storageSliderMax
                }
            }

            LaunchedEffect(Unit) {
                appPreferencesDataStore.hideBatteryOptReminder.collect { hidden ->
                    hideBatteryOptReminder = hidden
                }
            }

            // Battery optimization observer
            DisposableEffect(Unit) {
                val activity = context.findActivity()
                val lifecycleObserver =
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isBatteryOptimized = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                        }
                    }
                activity?.lifecycle?.addObserver(lifecycleObserver)

                onDispose {
                    activity?.lifecycle?.removeObserver(lifecycleObserver)
                }
            }

            return AddNewRemoteAlertScreen.State(
                showBatteryOptSheet = showBatteryOptimizeSheet,
                isBatteryOptimized = isBatteryOptimized,
                selectedAlertType = selectedType,
                threshold = threshold,
                availableStorage = availableStorage,
                storageSliderMax = storageSliderMax,
                hideBatteryOptReminder = hideBatteryOptReminder,
            ) { event ->
                when (event) {
                    is AddNewRemoteAlertScreen.Event.SaveNotification -> {
                        scope.launch {
                            analytics.logAlertAdded(event.notification.toAlertType())
                            remoteAlertRepository.saveRemoteAlert(event.notification)
                        }
                        navigator.pop()
                    }
                    AddNewRemoteAlertScreen.Event.NavigateBack -> {
                        navigator.pop()
                    }
                    AddNewRemoteAlertScreen.Event.ShowBatteryOptimizationSheet -> {
                        showBatteryOptimizeSheet = true
                        scope.launch {
                            analytics.logOptimizeBatteryInfoShown()
                        }
                    }
                    AddNewRemoteAlertScreen.Event.DismissBatteryOptimizationSheet -> {
                        showBatteryOptimizeSheet = false
                    }
                    AddNewRemoteAlertScreen.Event.OpenBatterySettings -> {
                        scope.launch {
                            analytics.logOptimizeBatteryGoToSettings()
                        }
                        showBatteryOptimizeSheet = false
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        context.startActivity(intent)
                    }
                    is AddNewRemoteAlertScreen.Event.UpdateAlertType -> {
                        selectedType = event.alertType
                    }
                    is AddNewRemoteAlertScreen.Event.UpdateThreshold -> {
                        threshold = event.value
                    }
                    AddNewRemoteAlertScreen.Event.HideBatteryOptimizationReminder -> {
                        scope.launch {
                            analytics.logOptimizeBatteryIgnore()
                            appPreferencesDataStore.setHideBatteryOptReminder(true)
                        }
                    }
                }
            }
        }

        @CircuitInject(AddNewRemoteAlertScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): AddNewRemoteAlertPresenter
        }
    }

@CircuitInject(screen = AddNewRemoteAlertScreen::class, scope = AppScope::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewRemoteAlertUi(
    state: AddNewRemoteAlertScreen.State,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Add New Alert") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(AddNewRemoteAlertScreen.Event.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Alert Type Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "Alert Type",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    AlertTypeSelector(
                        selectedType = state.selectedAlertType,
                        onTypeSelected = { type ->
                            state.eventSink(AddNewRemoteAlertScreen.Event.UpdateAlertType(type))
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            // Threshold Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        when (state.selectedAlertType) {
                            AlertType.BATTERY -> "Battery Level Threshold"
                            AlertType.STORAGE -> "Storage Space Threshold"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            when (state.selectedAlertType) {
                                AlertType.BATTERY -> "Alert when battery falls below ${state.threshold}%"
                                AlertType.STORAGE -> "Alert when available storage is below ${state.threshold}GB"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = state.threshold.toFloat(),
                            onValueChange = { value ->
                                state.eventSink(AddNewRemoteAlertScreen.Event.UpdateThreshold(value.toInt()))
                            },
                            valueRange =
                                when (state.selectedAlertType) {
                                    AlertType.BATTERY -> 5f..50f
                                    AlertType.STORAGE -> 1f..state.storageSliderMax.toFloat()
                                },
                            steps =
                                when (state.selectedAlertType) {
                                    AlertType.BATTERY -> 44 // Total steps: (50-5) - 1 = 44 steps
                                    AlertType.STORAGE -> (state.storageSliderMax - 1) // From 1 to max, so max-1 steps
                                },
                        )
                    }
                }
            }

            // Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            ) {
                ListItem(
                    headlineContent = {
                        Text(
                            when (state.selectedAlertType) {
                                AlertType.BATTERY -> "Battery Alert"
                                AlertType.STORAGE -> "Storage Alert"
                            },
                        )
                    },
                    supportingContent = {
                        Column {
                            Text(
                                when (state.selectedAlertType) {
                                    AlertType.BATTERY -> "Will notify when battery is below ${state.threshold}%"
                                    AlertType.STORAGE ->
                                        buildString {
                                            append("Will notify when storage is below ${state.threshold}GB")
                                            append(" (Currently: ${state.availableStorage}GB available)")
                                        }
                                },
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "NOTE: Same alert will be sent only once every 24 hours.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontStyle = FontStyle.Italic,
                            )
                        }
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(state.selectedAlertType.toIconResId()),
                            contentDescription = null,
                        )
                    },
                )
            }

            // Save Button
            Button(
                onClick = {
                    val notification =
                        when (state.selectedAlertType) {
                            AlertType.BATTERY -> RemoteAlert.BatteryAlert(batteryPercentage = state.threshold)
                            AlertType.STORAGE -> RemoteAlert.StorageAlert(storageMinSpaceGb = state.threshold)
                        }
                    state.eventSink(AddNewRemoteAlertScreen.Event.SaveNotification(notification))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.threshold > 0,
            ) {
                Text("Save Alert")
            }

            // Only show card if both conditions are met
            if (!state.isBatteryOptimized && !state.hideBatteryOptReminder) {
                Spacer(modifier = Modifier.height(24.dp))
                BatteryOptimizationCard(
                    onOptimizeClick = {
                        state.eventSink(AddNewRemoteAlertScreen.Event.ShowBatteryOptimizationSheet)
                    },
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Show bottom sheet if needed
        if (state.showBatteryOptSheet) {
            BatteryOptimizationBottomSheet(
                sheetState = sheetState,
                onSettingsClick = {
                    state.eventSink(AddNewRemoteAlertScreen.Event.OpenBatterySettings)
                },
                onDontRemindAgain = {
                    state.eventSink(AddNewRemoteAlertScreen.Event.HideBatteryOptimizationReminder)
                    state.eventSink(AddNewRemoteAlertScreen.Event.DismissBatteryOptimizationSheet)
                },
                onDismiss = {
                    state.eventSink(AddNewRemoteAlertScreen.Event.DismissBatteryOptimizationSheet)
                },
            )
        }
    }
}

@Composable
private fun AlertTypeSelector(
    selectedType: AlertType,
    onTypeSelected: (AlertType) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        AlertType.entries.forEachIndexed { index, alertType ->
            SegmentedButton(
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = AlertType.entries.size,
                    ),
                icon = {
                    SegmentedButtonDefaults.Icon(active = alertType == selectedType) {
                        Icon(
                            painter =
                                painterResource(
                                    when (alertType) {
                                        AlertType.BATTERY -> R.drawable.battery_5_bar_24dp
                                        AlertType.STORAGE -> R.drawable.hard_disk_24dp
                                    },
                                ),
                            contentDescription = null,
                        )
                    }
                },
                onClick = { onTypeSelected(alertType) },
                selected = alertType == selectedType,
            ) {
                Text(
                    when (alertType) {
                        AlertType.BATTERY -> "Battery"
                        AlertType.STORAGE -> "Storage"
                    },
                )
            }
        }
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewAddNewRemoteAlertUi() {
    ComposeAppTheme {
        AddNewRemoteAlertUi(
            state =
                AddNewRemoteAlertScreen.State(
                    showBatteryOptSheet = false,
                    isBatteryOptimized = false,
                    selectedAlertType = AlertType.BATTERY,
                    threshold = 10,
                    availableStorage = 56,
                    storageSliderMax = 96,
                    hideBatteryOptReminder = false,
                    eventSink = {},
                ),
        )
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewStorageAlertUi() {
    ComposeAppTheme {
        AddNewRemoteAlertUi(
            state =
                AddNewRemoteAlertScreen.State(
                    showBatteryOptSheet = false,
                    isBatteryOptimized = true,
                    selectedAlertType = AlertType.STORAGE,
                    threshold = 8,
                    availableStorage = 16,
                    storageSliderMax = 20,
                    hideBatteryOptReminder = false,
                    eventSink = {},
                ),
        )
    }
}
