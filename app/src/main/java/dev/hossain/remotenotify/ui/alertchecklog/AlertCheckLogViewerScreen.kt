package dev.hossain.remotenotify.ui.alertchecklog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import dev.hossain.remotenotify.db.AlertCheckLogDao
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.utils.formatTimeDuration
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize

@Parcelize
data object AlertCheckLogViewerScreen : Screen {
    data class State(
        val logs: List<AlertCheckLog>,
        val isLoading: Boolean,
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
        private val alertCheckLogDao: AlertCheckLogDao,
    ) : Presenter<AlertCheckLogViewerScreen.State> {
        @Composable
        override fun present(): AlertCheckLogViewerScreen.State {
            var isLoading by remember { mutableStateOf(true) }

            val logs by produceState<List<AlertCheckLog>>(emptyList()) {
                alertCheckLogDao
                    .getAllCheckLogs()
                    .map { entities ->
                        entities.map { entity ->
                            AlertCheckLog(
                                checkedOn = entity.checkedAt,
                                alertType = entity.alertType,
                                isAlertSent = entity.alertTriggered,
                                stateValue = entity.alertStateValue,
                            )
                        }
                    }.collect {
                        value = it
                        isLoading = false
                    }
            }

            return AlertCheckLogViewerScreen.State(
                logs = logs,
                isLoading = isLoading,
            ) { event ->
                when (event) {
                    AlertCheckLogViewerScreen.Event.NavigateBack -> navigator.pop()
                }
            }
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
                title = { Text("Logs") },
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
        if (state.isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(count = state.logs.size, key = { state.logs[it].checkedOn }) { index ->
                    LogItemCard(state.logs[index])
                }
            }
        }
    }
}

@Composable
private fun LogItemCard(log: AlertCheckLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = formatTimeDuration(log.checkedOn),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text =
                            when (log.alertType) {
                                AlertType.BATTERY -> "${log.stateValue}% Battery"
                                AlertType.STORAGE -> "${log.stateValue}GB Storage"
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text =
                            if (log.isAlertSent) {
                                "Alert triggered & sent notifications"
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
