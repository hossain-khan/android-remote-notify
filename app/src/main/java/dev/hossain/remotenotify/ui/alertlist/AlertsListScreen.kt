package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.RemoteNotification
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
data object AlertsListScreen : Screen {
    data class State(
        val notifications: List<RemoteNotification>,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class DeleteNotification(
            val notification: RemoteNotification,
        ) : Event()

        data object AddNotification : Event()
    }
}

class AlertsListPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val remoteAlertRepository: RemoteAlertRepository,
    ) : Presenter<AlertsListScreen.State> {
        @Composable
        override fun present(): AlertsListScreen.State {
            val scope = rememberCoroutineScope()

            val notifications by produceState<List<RemoteNotification>>(emptyList()) {
                remoteAlertRepository
                    .getAllRemoteNotificationsFlow()
                    .collect {
                        value = it
                    }
            }

            return AlertsListScreen.State(notifications) { event ->
                when (event) {
                    is AlertsListScreen.Event.DeleteNotification -> {
                        Timber.d("Deleting notification: $event")
                        scope.launch {
                            remoteAlertRepository.deleteRemoteNotification(event.notification)
                        }
                    }
                    AlertsListScreen.Event.AddNotification -> {
                        navigator.goTo(AddNewRemoteAlertScreen)
                    }
                }
            }
        }

        @CircuitInject(AlertsListScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): AlertsListPresenter
        }
    }

@CircuitInject(screen = AlertsListScreen::class, scope = AppScope::class)
@Composable
fun AlertsListUi(
    state: AlertsListScreen.State,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val batteryMonitor = BatteryMonitor(context)
    val storageMonitor = StorageMonitor(context)
    val batteryPercentage = batteryMonitor.getBatteryLevel()
    val availableStorage = storageMonitor.getAvailableStorageInGB()
    val totalStorage = storageMonitor.getTotalStorageInGB()

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { state.eventSink(AlertsListScreen.Event.AddNotification) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Notification")
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Display battery percentage at the top
            Text(
                text = "Battery Percentage: $batteryPercentage%",
                modifier = Modifier.padding(2.dp),
            )
            // Display storage data under battery level
            Text(
                text = "Available Storage: $availableStorage GB",
                modifier = Modifier.padding(2.dp),
            )
            Text(
                text = "Total Storage: $totalStorage GB",
                modifier = Modifier.padding(2.dp),
            )
            LazyColumn {
                items(state.notifications) { notification ->
                    NotificationItem(notification = notification, onDelete = {
                        state.eventSink(AlertsListScreen.Event.DeleteNotification(notification))
                    })
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: RemoteNotification,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (notification) {
                is RemoteNotification.BatteryNotification -> {
                    Text(text = "Battery Alert")
                    Text(text = "Battery Percentage: ${notification.batteryPercentage}%")
                }
                is RemoteNotification.StorageNotification -> {
                    Text(text = "Storage Alert")
                    Text(text = "Minimum Storage Space: ${notification.storageMinSpaceGb} GB")
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Row {
                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onDelete) {
                    Text(text = "Delete")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAlertsListUi() {
    val sampleNotifications =
        listOf(
            RemoteNotification.BatteryNotification(batteryPercentage = 50),
            RemoteNotification.StorageNotification(storageMinSpaceGb = 10),
        )
    AlertsListUi(
        state =
            AlertsListScreen.State(
                notifications = sampleNotifications,
                eventSink = {},
            ),
    )
}
