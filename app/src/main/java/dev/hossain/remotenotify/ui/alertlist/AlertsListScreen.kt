package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
import dev.hossain.remotenotify.ui.alertmediumlist.NotificationMediumListScreen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
data object AlertsListScreen : Screen {
    data class State(
        val remoteAlertConfigs: List<RemoteAlert>,
        val batteryPercentage: Int,
        val availableStorage: Long,
        val totalStorage: Long,
        val isAnyNotifierConfigured: Boolean,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class DeleteNotification(
            val notification: RemoteAlert,
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
            val deviceBatteryLevelPercentage = remember { batteryMonitor.getBatteryLevel() }
            val deviceAvailableStorage = remember { storageMonitor.getAvailableStorageInGB() }
            val deviceTotalStorage = remember { storageMonitor.getTotalStorageInGB() }

            val notifications by produceState<List<RemoteAlert>>(emptyList()) {
                remoteAlertRepository
                    .getAllRemoteAlertFlow()
                    .collect {
                        value = it
                    }
            }

            val isAnyNotifierConfigured by produceState(false) {
                value = notifiers.any { it.hasValidConfig() }
            }

            return AlertsListScreen.State(
                remoteAlertConfigs = notifications,
                batteryPercentage = deviceBatteryLevelPercentage,
                availableStorage = deviceAvailableStorage,
                totalStorage = deviceTotalStorage,
                isAnyNotifierConfigured = isAnyNotifierConfigured,
            ) { event ->
                when (event) {
                    is AlertsListScreen.Event.DeleteNotification -> {
                        Timber.d("Deleting notification: $event")
                        scope.launch {
                            remoteAlertRepository.deleteRemoteAlert(event.notification)
                        }
                    }

                    AlertsListScreen.Event.AddNotification -> {
                        navigator.goTo(AddNewRemoteAlertScreen)
                    }

                    AlertsListScreen.Event.AddNotificationDestination -> {
                        navigator.goTo(NotificationMediumListScreen)
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
                    .padding(horizontal = 8.dp),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Display battery percentage at the top
                item(key = "device_state_info") { DeviceCurrentStateUi(state) }

                if (state.isAnyNotifierConfigured.not()) {
                    item(key = "no_notifier_configured") {
                        NoNotifierConfiguredCard(
                            onConfigureClick = {
                                state.eventSink(AlertsListScreen.Event.AddNotificationDestination)
                            },
                        )
                    }
                }

                // Show empty state or user configured alerts
                if (state.remoteAlertConfigs.isEmpty()) {
                    item(key = "alert_empty_view") { EmptyNotificationsState() }
                } else {
                    item(key = "alerts_header") {
                        Text(
                            text = "Your Alerts",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    itemsIndexed(
                        items = state.remoteAlertConfigs,
                        key = { _, remoteAlert -> remoteAlert.alertId },
                    ) { _: Int, remoteAlert: RemoteAlert ->
                        NotificationItem(
                            remoteAlert = remoteAlert,
                            onDelete = {
                                state.eventSink(AlertsListScreen.Event.DeleteNotification(remoteAlert))
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCurrentStateUi(state: AlertsListScreen.State) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // Battery Status
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.battery_5_bar_24dp),
                        contentDescription = "Battery Status",
                        tint =
                            if (state.batteryPercentage > 20) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Battery",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${state.batteryPercentage}%",
                        style = MaterialTheme.typography.titleMedium,
                        color =
                            if (state.batteryPercentage > 20) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.batteryPercentage / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color =
                        if (state.batteryPercentage > 20) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Status
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.hard_disk_24dp),
                        contentDescription = "Storage Status",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${state.availableStorage}/${state.totalStorage} GB",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (state.availableStorage.toFloat() / state.totalStorage.toFloat()) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
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
    remoteAlert: RemoteAlert,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        shadowElevation = 2.dp,
        leadingContent = {
            Icon(
                painter =
                    when (remoteAlert) {
                        is RemoteAlert.BatteryAlert ->
                            painterResource(id = R.drawable.battery_3_bar_24dp)
                        is RemoteAlert.StorageAlert ->
                            painterResource(id = R.drawable.hard_disk_24dp)
                    },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        },
        headlineContent = {
            Text(
                text =
                    when (remoteAlert) {
                        is RemoteAlert.BatteryAlert -> "Battery Alert"
                        is RemoteAlert.StorageAlert -> "Storage Alert"
                    },
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                when (remoteAlert) {
                    is RemoteAlert.BatteryAlert -> {
                        LinearProgressIndicator(
                            progress = { remoteAlert.batteryPercentage / 100f },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(4.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${remoteAlert.batteryPercentage}%",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    is RemoteAlert.StorageAlert -> {
                        Text(
                            text = "Min Storage: ${remoteAlert.storageMinSpaceGb} GB",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        trailingContent = {
            IconButton(
                onClick = onDelete,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Alert",
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

@Composable
private fun EmptyNotificationsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No alerts configured",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAlertsListUi() {
    val sampleNotifications =
        listOf(
            RemoteAlert.BatteryAlert(batteryPercentage = 50),
            RemoteAlert.StorageAlert(storageMinSpaceGb = 10),
        )
    AlertsListUi(
        state =
            AlertsListScreen.State(
                remoteAlertConfigs = sampleNotifications,
                batteryPercentage = 50,
                availableStorage = 10,
                totalStorage = 100,
                isAnyNotifierConfigured = false,
                eventSink = {},
            ),
    )
}
