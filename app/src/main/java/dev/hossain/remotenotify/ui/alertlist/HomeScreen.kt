package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.hossain.remotenotify.db.NotificationDao
import dev.hossain.remotenotify.db.NotificationEntity
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.ui.addalert.AddNotificationScreen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object HomeScreen : Screen {
    data class State(
        val notifications: List<NotificationEntity>,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class DeleteNotification(
            val notification: NotificationEntity,
        ) : Event()

        data object AddNotification : Event()
    }
}

class HomePresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val notificationDao: NotificationDao,
    ) : Presenter<HomeScreen.State> {
        @Composable
        override fun present(): HomeScreen.State {
            val scope = rememberCoroutineScope()
            var notifications: List<NotificationEntity> by remember { mutableStateOf(emptyList()) }

            LaunchedEffect(Unit) {
                notifications = notificationDao.getAll()
            }

            return HomeScreen.State(notifications) { event ->
                when (event) {
                    is HomeScreen.Event.DeleteNotification -> {
                        scope.launch {
                            notificationDao.delete(event.notification)
                        }
                    }
                    HomeScreen.Event.AddNotification -> {
                        navigator.goTo(AddNotificationScreen)
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
    notification: NotificationEntity,
    onDelete: () -> Unit,
) {
    // Implement the UI for each notification item
}
