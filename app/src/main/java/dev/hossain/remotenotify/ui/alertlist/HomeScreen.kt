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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import dev.hossain.remotenotify.ui.addalert.AddNewRemoteAlertScreen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object HomeScreen : Screen {
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

class HomePresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val remoteAlertRepository: RemoteAlertRepository,
    ) : Presenter<HomeScreen.State> {
        @Composable
        override fun present(): HomeScreen.State {
            val scope = rememberCoroutineScope()
            var notifications: List<RemoteNotification> by remember { mutableStateOf(emptyList()) }

            LaunchedEffect(Unit) {
                notifications = remoteAlertRepository.getAllRemoteNotifications()
            }

            return HomeScreen.State(notifications) { event ->
                when (event) {
                    is HomeScreen.Event.DeleteNotification -> {
                        scope.launch {
                            remoteAlertRepository.deleteRemoteNotification(event.notification)
                        }
                    }
                    HomeScreen.Event.AddNotification -> {
                        navigator.goTo(AddNewRemoteAlertScreen)
                    }
                }
            }
        }

        @CircuitInject(HomeScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): HomePresenter
        }
    }

@CircuitInject(screen = HomeScreen::class, scope = AppScope::class)
@Composable
fun Home(
    state: HomeScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { state.eventSink(HomeScreen.Event.AddNotification) }) {
                Icon(Icons.Default.Add, contentDescription = "Add Notification")
            }
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            items(state.notifications) { notification ->
                NotificationItem(notification = notification, onDelete = {
                    state.eventSink(HomeScreen.Event.DeleteNotification(notification))
                })
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
