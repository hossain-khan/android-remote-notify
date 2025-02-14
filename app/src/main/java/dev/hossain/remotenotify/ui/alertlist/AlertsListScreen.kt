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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
import dev.hossain.remotenotify.ui.addterminus.AddNotificationMediumScreen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
data object AlertsListScreen : Screen {
    data class State(
        val notifications: List<RemoteNotification>,
        val batteryPercentage: Int,
        val availableStorage: Long,
        val totalStorage: Long,
        val isAnyNotifierConfigured: Boolean,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class DeleteNotification(
            val notification: RemoteNotification,
        ) : Event()

        data object AddNotification : Event()

        data object AddNotificationDestination : Event()
    }
}

class AlertsListPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val remoteAlertRepository: RemoteAlertRepository,
        private val batteryMonitor: BatteryMonitor,
        private val storageMonitor: StorageMonitor,
        private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
    ) : Presenter<AlertsListScreen.State> {
        @Composable
        override fun present(): AlertsListScreen.State {
            val scope = rememberCoroutineScope()
            val batteryPercentage = batteryMonitor.getBatteryLevel()
            val availableStorage = storageMonitor.getAvailableStorageInGB()
            val totalStorage = storageMonitor.getTotalStorageInGB()

            val notifications by produceState<List<RemoteNotification>>(emptyList()) {
                remoteAlertRepository
                    .getAllRemoteNotificationsFlow()
                    .collect {
                        value = it
                    }
            }

            val isAnyNotifierConfigured by produceState(false) {
                value = notifiers.any { it.hasValidConfiguration() }
            }

            return AlertsListScreen.State(
                notifications = notifications,
                batteryPercentage = batteryPercentage,
                availableStorage = availableStorage,
                totalStorage = totalStorage,
                isAnyNotifierConfigured = isAnyNotifierConfigured,
            ) { event ->
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

                    AlertsListScreen.Event.AddNotificationDestination -> {
                        navigator.goTo(AddNotificationMediumScreen)
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

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(screen = AlertsListScreen::class, scope = AppScope::class)
@Composable
fun AlertsListUi(
    state: AlertsListScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Remote Alerts") },
                actions = {
                    IconButton(onClick = {
                        state.eventSink(AlertsListScreen.Event.AddNotificationDestination)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { state.eventSink(AlertsListScreen.Event.AddNotification) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Alert Notification") },
                text = { Text("Add Alert") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
        ) {
            LazyColumn {
                // Display battery percentage at the top
                item { DeviceCurrentStateUi(state) }

                if (state.isAnyNotifierConfigured.not()) {
                    item {
                        NoNotifierConfiguredCard(
                            onConfigureClick = {
                                state.eventSink(AlertsListScreen.Event.AddNotificationDestination)
                            },
                        )
                    }
                }

                // Show all user configured alerts
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
private fun DeviceCurrentStateUi(state: AlertsListScreen.State) {
    Column {
        Text(
            text = "Battery Percentage: ${state.batteryPercentage}%",
            modifier = Modifier.padding(2.dp),
        )
        // Display storage data under battery level
        Text(
            text = "Available Storage: ${state.availableStorage} GB",
            modifier = Modifier.padding(2.dp),
        )
        Text(
            text = "Total Storage: ${state.totalStorage} GB",
            modifier = Modifier.padding(2.dp),
        )
    }
}

@Composable
private fun NoNotifierConfiguredCard(
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = "No notification medium configured. Please configure one.")
            Spacer(modifier = Modifier.size(8.dp))
            Button(onClick = onConfigureClick) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Configure")
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
    Card(
        modifier =
            Modifier
                .padding(8.dp)
                .fillMaxWidth(),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                when (notification) {
                    is RemoteNotification.BatteryNotification -> {
                        Text(text = "Battery Alert")
                    }
                    is RemoteNotification.StorageNotification -> {
                        Text(text = "Storage Alert")
                    }
                }
            },
            supportingContent = {
                when (notification) {
                    is RemoteNotification.BatteryNotification -> {
                        Text(text = "Battery Percentage: ${notification.batteryPercentage}%")
                    }
                    is RemoteNotification.StorageNotification -> {
                        Text(text = "Minimum Storage Space: ${notification.storageMinSpaceGb} GB")
                    }
                }
            },
            trailingContent = {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Alert",
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
            modifier = Modifier.padding(horizontal = 4.dp),
        )
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
                batteryPercentage = 50,
                availableStorage = 10,
                totalStorage = 100,
                isAnyNotifierConfigured = false,
                eventSink = {},
            ),
    )
}
