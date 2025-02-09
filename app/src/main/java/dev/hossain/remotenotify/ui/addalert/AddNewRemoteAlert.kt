package dev.hossain.remotenotify.ui.addalert

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import dev.hossain.remotenotify.db.NotificationDao
import dev.hossain.remotenotify.db.NotificationEntity
import dev.hossain.remotenotify.db.NotificationType
import dev.hossain.remotenotify.di.AppScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object AddNotificationScreen : Screen {
    data class State(
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class SaveNotification(
            val notification: NotificationEntity,
        ) : Event()
    }
}

class AddNotificationPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val notificationDao: NotificationDao,
    ) : Presenter<AddNotificationScreen.State> {
        @Composable
        override fun present(): AddNotificationScreen.State {
            val scope = rememberCoroutineScope()
            return AddNotificationScreen.State { event ->
                when (event) {
                    is AddNotificationScreen.Event.SaveNotification -> {
                        scope.launch {
                            notificationDao.insert(event.notification)
                        }
                        navigator.pop()
                    }
                }
            }
        }

        @CircuitInject(AddNotificationScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): AddNotificationPresenter
        }
    }

@CircuitInject(screen = AddNotificationScreen::class, scope = AppScope::class)
@Composable
fun AddNotification(
    state: AddNotificationScreen.State,
    modifier: Modifier = Modifier,
) {
    var type by remember { mutableStateOf(NotificationType.BATTERY) }
    var threshold by remember { mutableStateOf(0) }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            // UI for selecting type and threshold
            Button(onClick = {
                state.eventSink(
                    AddNotificationScreen.Event.SaveNotification(
                        NotificationEntity(type = type, batteryPercentage = threshold, storageMinSpaceGb = threshold),
                    ),
                )
            }) {
                Text("Save")
            }
        }
    }
}
