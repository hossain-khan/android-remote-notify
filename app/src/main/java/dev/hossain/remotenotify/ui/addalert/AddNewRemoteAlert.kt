package dev.hossain.remotenotify.ui.addalert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object AddNewRemoteAlertScreen : Screen {
    data class State(
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class SaveNotification(
            val notification: RemoteAlert,
        ) : Event()

        data object NavigateBack : Event()
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
            return AddNewRemoteAlertScreen.State { event ->
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
    var threshold by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Add New Alert") },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(AddNewRemoteAlertScreen.Event.NavigateBack) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FilterChip(
                            selected = type == AlertType.BATTERY,
                            onClick = { type = AlertType.BATTERY },
                            label = { Text("Battery") },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.battery_5_bar_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                        FilterChip(
                            selected = type == AlertType.STORAGE,
                            onClick = { type = AlertType.STORAGE },
                            label = { Text("Storage") },
                            leadingIcon = {
                                Icon(
                                    painterResource(R.drawable.hard_disk_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
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
                        style = MaterialTheme.typography.titleMedium,
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
                                    AlertType.BATTERY -> 0f..100f
                                    AlertType.STORAGE -> 0f..64f // Assuming max storage threshold
                                },
                            steps =
                                when (type) {
                                    AlertType.BATTERY -> 100
                                    AlertType.STORAGE -> 64
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
                            when (type) {
                                AlertType.BATTERY -> painterResource(R.drawable.battery_3_bar_24dp)
                                AlertType.STORAGE -> painterResource(R.drawable.hard_disk_24dp)
                            },
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
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAddNewRemoteAlertUi() {
    AddNewRemoteAlertUi(
        state =
            AddNewRemoteAlertScreen.State(
                eventSink = {},
            ),
    )
}
