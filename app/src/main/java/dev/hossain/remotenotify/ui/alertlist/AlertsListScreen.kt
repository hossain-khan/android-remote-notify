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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.effects.LaunchedImpressionEffect
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.WorkerStatus
import dev.hossain.remotenotify.monitor.BatteryMonitor
import dev.hossain.remotenotify.monitor.StorageMonitor
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.ui.about.AboutAppScreen
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
import dev.hossain.remotenotify.ui.alertchecklog.AlertCheckLogViewerScreen
import dev.hossain.remotenotify.ui.alertmediumlist.NotificationMediumListScreen
import dev.hossain.remotenotify.worker.DEVICE_VITALS_CHECKER_WORKER_ID
import dev.hossain.remotenotify.worker.ObserveDeviceHealthWorker.Companion.WORK_DATA_KEY_LAST_RUN_TIMESTAMP_MS
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.tatarka.inject.annotations.Inject
import timber.log.Timber

@Parcelize
data object AlertsListScreen : Screen {
    data class State(
        val remoteAlertConfigs: List<RemoteAlert>,
        val batteryPercentage: Int,
        val availableStorage: Long,
        val totalStorage: Long,
        val isAnyNotifierConfigured: Boolean,
        val latestAlertCheckLog: AlertCheckLog?,
        val workerStatus: WorkerStatus?,
        val showEducationSheet: Boolean,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class DeleteNotification(
            val notification: RemoteAlert,
        ) : Event()

        data object AddNotification : Event()

        data object AddNotificationDestination : Event()

        data object NavigateToAbout : Event()

        data object ShowEducationSheet : Event()

        data object DismissEducationSheet : Event()

        data object ViewAllLogs : Event()
    }
}

class AlertsListPresenter
    @Inject
    constructor(
        private val navigator: Navigator,
        private val remoteAlertRepository: RemoteAlertRepository,
        private val batteryMonitor: BatteryMonitor,
        private val storageMonitor: StorageMonitor,
        private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
        private val appPreferencesDataStore: AppPreferencesDataStore,
        private val analytics: Analytics,
    ) : Presenter<AlertsListScreen.State> {
        @Composable
        override fun present(): AlertsListScreen.State {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val deviceBatteryLevelPercentage = remember { batteryMonitor.getBatteryLevel() }
            val deviceAvailableStorage = remember { storageMonitor.getAvailableStorageInGB() }
            val deviceTotalStorage = remember { storageMonitor.getTotalStorageInGB() }
            var showEducationSheet by remember { mutableStateOf(false) }

            LaunchedImpressionEffect {
                analytics.logScreenView(AlertsListScreen::class)
            }

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

            val workerStatus by produceState<WorkerStatus?>(null) {
                WorkManager
                    .getInstance(context)
                    .getWorkInfosByTagFlow(DEVICE_VITALS_CHECKER_WORKER_ID)
                    .collect { workInfos ->
                        val workInfo = workInfos.firstOrNull()
                        value =
                            workInfo?.let {
                                Timber.d("Got worker status: ${it.state}")
                                WorkerStatus(
                                    state = it.state.name,
                                    nextRunTimeMs = it.nextScheduleTimeMillis,
                                    lastRunTimeMs = it.progress.getLong(WORK_DATA_KEY_LAST_RUN_TIMESTAMP_MS, 0),
                                )
                            }
                    }
            }

            val lastCheckLog by produceState<AlertCheckLog?>(null) {
                remoteAlertRepository
                    .getLatestCheckLog()
                    .collect { value = it }
            }

            return AlertsListScreen.State(
                remoteAlertConfigs = notifications,
                batteryPercentage = deviceBatteryLevelPercentage,
                availableStorage = deviceAvailableStorage,
                totalStorage = deviceTotalStorage,
                isAnyNotifierConfigured = isAnyNotifierConfigured,
                latestAlertCheckLog = lastCheckLog,
                workerStatus = workerStatus,
                showEducationSheet = showEducationSheet,
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

                    AlertsListScreen.Event.NavigateToAbout -> {
                        navigator.goTo(AboutAppScreen)
                    }

                    AlertsListScreen.Event.DismissEducationSheet -> {
                        Timber.d("Dismiss the first time user education dialog shown")
                        scope.launch {
                            showEducationSheet = false
                            analytics.logViewTutorial(isComplete = true)
                            appPreferencesDataStore.markEducationDialogShown()
                        }
                    }

                    AlertsListScreen.Event.ShowEducationSheet -> {
                        Timber.d("Showing the first time user education dialog shown")
                        scope.launch {
                            showEducationSheet = true
                            analytics.logViewTutorial(isComplete = false)
                            appPreferencesDataStore.markEducationDialogShown()
                        }
                    }

                    AlertsListScreen.Event.ViewAllLogs -> {
                        navigator.goTo(AlertCheckLogViewerScreen)
                    }
                }
            }
        }

        @CircuitInject(AlertsListScreen::class, AppScope::class)
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
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Remote Alerts") },
                actions = {
                    IconButton(onClick = {
                        state.eventSink(AlertsListScreen.Event.NavigateToAbout)
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About App",
                        )
                    }
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
                item(key = "device_info_header") {
                    SectionHeader("Device Current Status")
                }
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
                } else {
                    item(key = "last_check_status") {
                        LastCheckStatusCardUi(
                            lastCheckLog = state.latestAlertCheckLog,
                            workerStatus = state.workerStatus,
                            onViewAllLogs = {
                                state.eventSink(AlertsListScreen.Event.ViewAllLogs)
                            },
                        )
                    }
                }

                // Show empty state or user configured alerts
                if (state.remoteAlertConfigs.isEmpty()) {
                    item(key = "alert_empty_view") {
                        EmptyNotificationsState(
                            onLearnMoreClick = {
                                state.eventSink(AlertsListScreen.Event.ShowEducationSheet)
                            },
                        )
                    }
                } else {
                    item(key = "alerts_header") {
                        SectionHeader("Your Configured Alerts")
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
        if (state.showEducationSheet) {
            AppUsageEducationSheetUi(sheetState = sheetState) {
                state.eventSink(AlertsListScreen.Event.DismissEducationSheet)
            }
        }
    }
}

@Composable
private fun DeviceCurrentStateUi(state: AlertsListScreen.State) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
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
                modifier = Modifier.size(36.dp),
            )
        },
        headlineContent = {
            Text(
                text =
                    when (remoteAlert) {
                        is RemoteAlert.BatteryAlert -> "Battery Alert"
                        is RemoteAlert.StorageAlert -> "Storage Alert"
                    },
                style = MaterialTheme.typography.titleSmall,
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewAlertsListUi() {
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
                latestAlertCheckLog = null,
                workerStatus = null,
                showEducationSheet = false,
                eventSink = {},
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun PreviewAlertsListUiWithLastCheck() {
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
                isAnyNotifierConfigured = true,
                latestAlertCheckLog =
                    AlertCheckLog(
                        checkedOn = System.currentTimeMillis() - 300_000, // 5 minutes ago
                        alertType = AlertType.BATTERY,
                        isAlertSent = true,
                        notifierType = NotifierType.TELEGRAM,
                        stateValue = 12,
                        configId = 1L,
                        configBatteryPercentage = 20,
                        configStorageMinSpaceGb = 0,
                        configCreatedOn = System.currentTimeMillis() - 86400000, // 1 day ago
                    ),
                workerStatus = WorkerStatus("RUNNING", 0, 0),
                showEducationSheet = false,
                eventSink = {},
            ),
    )
}
