package dev.hossain.remotenotify.ui.alertchecklog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.utils.formatTimeDuration
import dev.hossain.remotenotify.utils.toTitleCase
import kotlinx.parcelize.Parcelize

/**
 * Screen to view all the alert check logs.
 * Currently this is only visible on debug build.
 */
@Parcelize
data object AlertCheckLogViewerScreen : Screen {
    data class State(
        val logs: List<AlertCheckLog>,
        val isLoading: Boolean,
        val checkIntervalMinutes: Long,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object NavigateBack : Event()
    }
}

class AlertCheckLogViewerPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val appPreferencesDataStore: AppPreferencesDataStore,
        private val remoteAlertRepository: RemoteAlertRepository,
    ) : Presenter<AlertCheckLogViewerScreen.State> {
        @Composable
        override fun present(): AlertCheckLogViewerScreen.State {
            var isLoading by remember { mutableStateOf(true) }

            val checkIntervalMinutes by produceState(0L) {
                appPreferencesDataStore.workerIntervalFlow.collect {
                    value = it
                }
            }

            val logs by produceState<List<AlertCheckLog>>(emptyList()) {
                remoteAlertRepository
                    .getAllAlertCheckLogs()
                    .collect {
                        value = it
                        isLoading = false
                    }
            }

            return AlertCheckLogViewerScreen.State(
                logs = logs,
                isLoading = isLoading,
                checkIntervalMinutes = checkIntervalMinutes,
                eventSink = { event ->
                    when (event) {
                        AlertCheckLogViewerScreen.Event.NavigateBack -> navigator.pop()
                    }
                },
            )
        }

        @CircuitInject(AlertCheckLogViewerScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): AlertCheckLogViewerPresenter
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(AlertCheckLogViewerScreen::class, AppScope::class)
@Composable
fun AlertCheckLogViewerUi(
    state: AlertCheckLogViewerScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Logs")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        state.eventSink(AlertCheckLogViewerScreen.Event.NavigateBack)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading logs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.logs.isEmpty()) {
                EmptyLogsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    item(key = "logs_summary") {
                        LogsSummaryInfo(
                            totalLogs = state.logs.size,
                            checkIntervalMinutes = state.checkIntervalMinutes,
                        )
                    }

                    items(
                        count = state.logs.size,
                        key = { state.logs[it].checkedOn },
                    ) { index ->
                        LogItemCard(
                            log = state.logs[index],
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemCard(log: AlertCheckLog) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (log.isAlertSent) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    when (log.alertType) {
                        AlertType.BATTERY -> "Battery Check"
                        AlertType.STORAGE -> "Storage Check"
                    },
                )
            },
            supportingContent = {
                Column {
                    // Show configured threshold and actual value
                    Text(
                        text =
                            when (log.alertType) {
                                AlertType.BATTERY ->
                                    buildString {
                                        append("${log.stateValue}% battery")
                                        append(" (Alert threshold: ${log.configBatteryPercentage}%)")
                                    }
                                AlertType.STORAGE ->
                                    buildString {
                                        append("${log.stateValue} GB storage")
                                        append(" (Alert threshold: ${log.configStorageMinSpaceGb} GB)")
                                    }
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Checked ${formatTimeDuration(log.checkedOn)}",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                if (log.isAlertSent) {
                                    "Alert triggered & sent via ${log.notifierType?.name?.toTitleCase() ?: "N/A"}"
                                } else {
                                    "Alert not triggered"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (log.isAlertSent) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            },
            leadingContent = {
                Icon(
                    painter =
                        painterResource(
                            when (log.alertType) {
                                AlertType.BATTERY -> R.drawable.battery_5_bar_24dp
                                AlertType.STORAGE -> R.drawable.hard_disk_24dp
                            },
                        ),
                    contentDescription = null,
                    tint =
                        if (log.isAlertSent) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            },
        )
    }
}

@Composable
private fun LogsSummaryInfo(
    totalLogs: Int,
    checkIntervalMinutes: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            if (checkIntervalMinutes > 0L) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.schedule_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alerts checked every $checkIntervalMinutes minutes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            if (totalLogs > 0) {
                if (checkIntervalMinutes > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.format_list_bulleted_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Total $totalLogs alert check logs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAlertCheckLogViewerUi() {
    val sampleLogs =
        listOf(
            AlertCheckLog(
                checkedOn = System.currentTimeMillis() - 3600000, // 1 hour ago
                alertType = AlertType.BATTERY,
                isAlertSent = true,
                notifierType = NotifierType.EMAIL,
                stateValue = 15,
                configId = 1,
                configBatteryPercentage = 20,
                configStorageMinSpaceGb = 0,
                configCreatedOn = System.currentTimeMillis() - 86400000 * 7, // 7 days ago
            ),
            AlertCheckLog(
                checkedOn = System.currentTimeMillis() - 7200000, // 2 hours ago
                alertType = AlertType.STORAGE,
                isAlertSent = false,
                notifierType = null,
                stateValue = 20,
                configId = 2,
                configBatteryPercentage = 0,
                configStorageMinSpaceGb = 25,
                configCreatedOn = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
            ),
            AlertCheckLog(
                checkedOn = System.currentTimeMillis() - 86400000, // 1 day ago
                alertType = AlertType.BATTERY,
                isAlertSent = false,
                notifierType = null,
                stateValue = 80,
                configId = 1,
                configBatteryPercentage = 20,
                configStorageMinSpaceGb = 0,
                configCreatedOn = System.currentTimeMillis() - 86400000 * 7, // 7 days ago
            ),
            AlertCheckLog(
                checkedOn = System.currentTimeMillis() - 9250000,
                alertType = AlertType.STORAGE,
                isAlertSent = true,
                notifierType = NotifierType.TELEGRAM,
                stateValue = 2,
                configId = 2,
                configBatteryPercentage = 0,
                configStorageMinSpaceGb = 25,
                configCreatedOn = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
            ),
        )

    ComposeAppTheme {
        AlertCheckLogViewerUi(
            state =
                AlertCheckLogViewerScreen.State(
                    logs = sampleLogs,
                    isLoading = false,
                    checkIntervalMinutes = 60,
                    eventSink = {},
                ),
        )
    }
}
