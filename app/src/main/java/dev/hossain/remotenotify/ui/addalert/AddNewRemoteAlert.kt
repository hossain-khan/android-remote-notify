package dev.hossain.remotenotify.ui.addalert

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import dev.hossain.remotenotify.model.NotificationType
import dev.hossain.remotenotify.model.RemoteNotification
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object AddNewRemoteAlertScreen : Screen {
    data class State(
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class SaveNotification(
            val notification: RemoteNotification,
        ) : Event()
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
                            remoteAlertRepository.saveRemoteNotification(event.notification)
                        }
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
@Composable
fun AddNewRemoteAlertUi(
    state: AddNewRemoteAlertScreen.State,
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
            // UI for selecting type
            Text("Select Alert Type")
            Row {
                RadioButton(
                    selected = type == NotificationType.BATTERY,
                    onClick = { type = NotificationType.BATTERY },
                )
                Text("Battery")
                RadioButton(
                    selected = type == NotificationType.STORAGE,
                    onClick = { type = NotificationType.STORAGE },
                )
                Text("Storage")
            }

            // UI for setting threshold
            Text("Set Threshold")
            Slider(
                value = threshold.toFloat(),
                onValueChange = { threshold = it.toInt() },
                valueRange = 0f..100f,
                steps = 100,
            )
            Text("Threshold: $threshold")

            // Save button
            Button(onClick = {
                val notification =
                    when (type) {
                        NotificationType.BATTERY ->
                            RemoteNotification.BatteryNotification(
                                batteryPercentage = threshold,
                            )
                        NotificationType.STORAGE ->
                            RemoteNotification.StorageNotification(
                                storageMinSpaceGb = threshold,
                            )
                    }
                state.eventSink(
                    AddNewRemoteAlertScreen.Event.SaveNotification(notification),
                )
            }) {
                Text("Save")
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
