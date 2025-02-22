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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.toIconResId
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
    }
}

class AddNewRemoteAlertPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val remoteAlertRepository: RemoteAlertRepository,
    ) : Presenter<AddNewRemoteAlertScreen.State> {
        @Composable
        override fun present(): AddNewRemoteAlertScreen.State {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            var showBatteryOptimizeSheet by remember { mutableStateOf(false) }
            var isBatteryOptimized by remember {
                mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
            }

            // Refresh battery optimization status when screen resumes
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
            ) { event ->
                when (event) {
                    is AddNewRemoteAlertScreen.Event.SaveNotification -> {
                        scope.launch {
                            remoteAlertRepository.saveRemoteAlert(event.notification)
                        }
                        navigator.pop()
                    }
                    AddNewRemoteAlertScreen.Event.NavigateBack -> {
                        navigator.pop()
                    }
                    AddNewRemoteAlertScreen.Event.ShowBatteryOptimizationSheet -> {
                        showBatteryOptimizeSheet = true
                    }
                    AddNewRemoteAlertScreen.Event.DismissBatteryOptimizationSheet -> {
                        showBatteryOptimizeSheet = false
                    }
                    AddNewRemoteAlertScreen.Event.OpenBatterySettings -> {
                        showBatteryOptimizeSheet = false
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        context.startActivity(intent)
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
    var type by remember { mutableStateOf(AlertType.BATTERY) }
    var threshold by remember { mutableIntStateOf(10) }
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
                        selectedType = type,
                        onTypeSelected = { type = it },
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
                        when (type) {
                            AlertType.BATTERY -> "Battery Level Threshold"
                            AlertType.STORAGE -> "Storage Space Threshold"
                        },
                        style = MaterialTheme.typography.labelMedium,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            when (type) {
                                AlertType.BATTERY -> "Alert when battery falls below $threshold%"
                                AlertType.STORAGE -> "Alert when available storage is below ${threshold}GB"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Slider(
                            value = threshold.toFloat(),
                            onValueChange = { threshold = it.toInt() },
                            valueRange =
                                when (type) {
                                    AlertType.BATTERY -> 5f..50f
                                    AlertType.STORAGE -> 1f..32f
                                },
                            steps =
                                when (type) {
                                    // Creates 45 possible values: 5,6,7,...,49,50
                                    AlertType.BATTERY -> 44
                                    // Creates 31 possible values: 1,2,3,...,31,32
                                    AlertType.STORAGE -> 30
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
                            when (type) {
                                AlertType.BATTERY -> "Battery Alert"
                                AlertType.STORAGE -> "Storage Alert"
                            },
                        )
                    },
                    supportingContent = {
                        Text(
                            when (type) {
                                AlertType.BATTERY -> "Will notify when battery is below $threshold%"
                                AlertType.STORAGE -> "Will notify when storage is below ${threshold}GB"
                            },
                        )
                    },
                    leadingContent = {
                        Icon(
                            painter = painterResource(type.toIconResId()),
                            contentDescription = null,
                        )
                    },
                )
            }

            // Save Button
            Button(
                onClick = {
                    val notification =
                        when (type) {
                            AlertType.BATTERY -> RemoteAlert.BatteryAlert(batteryPercentage = threshold)
                            AlertType.STORAGE -> RemoteAlert.StorageAlert(storageMinSpaceGb = threshold)
                        }
                    state.eventSink(AddNewRemoteAlertScreen.Event.SaveNotification(notification))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = threshold > 0,
            ) {
                Text("Save Alert")
            }

            if (!state.isBatteryOptimized) {
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
                    eventSink = {},
                ),
        )
    }
}
