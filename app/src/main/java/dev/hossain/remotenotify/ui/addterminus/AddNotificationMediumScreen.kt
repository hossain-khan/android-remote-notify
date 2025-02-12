package dev.hossain.remotenotify.ui.addterminus

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.notifier.NotifierType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object AddNotificationMediumScreen : Screen {
    data class State(
        val botToken: String,
        val chatId: String,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class SaveTelegramConfig(
            val botToken: String,
            val chatId: String,
        ) : Event()
    }
}

class AddNotificationMediumPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val telegramConfigDataStore: TelegramConfigDataStore,
    ) : Presenter<AddNotificationMediumScreen.State> {
        @Composable
        override fun present(): AddNotificationMediumScreen.State {
            val scope = rememberCoroutineScope()
            var savedBotToken by remember { mutableStateOf("") }
            var savedChatId by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                savedBotToken = telegramConfigDataStore.botToken.first() ?: ""
                savedChatId = telegramConfigDataStore.chatId.first() ?: ""
            }

            return AddNotificationMediumScreen.State(savedBotToken, savedChatId) { event ->
                when (event) {
                    is AddNotificationMediumScreen.Event.SaveTelegramConfig -> {
                        scope.launch {
                            telegramConfigDataStore.saveBotToken(event.botToken)
                            telegramConfigDataStore.saveChatId(event.chatId)
                        }
                        navigator.pop()
                    }
                }
            }
        }

        @CircuitInject(AddNotificationMediumScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): AddNotificationMediumPresenter
        }
    }

@CircuitInject(screen = AddNotificationMediumScreen::class, scope = AppScope::class)
@Composable
fun AddNotificationMediumUi(
    state: AddNotificationMediumScreen.State,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(NotifierType.TELEGRAM) }
    var botToken by remember { mutableStateOf(state.botToken) }
    var chatId by remember { mutableStateOf(state.chatId) }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            // UI for selecting notification medium
            Text("Select Notification Medium")
            Row {
                NotifierType.entries.forEach { type ->
                    RadioButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                    )
                    Text(type.name)
                }
            }

            // UI for Telegram configuration
            if (selectedType == NotifierType.TELEGRAM) {
                Text("Enter Telegram Bot Token")
                // Add TextField for botToken input
                TextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    label = { Text("Bot Token") },
                )

                Text("Enter Telegram Chat ID")
                // Add TextField for chatId input
                TextField(
                    value = chatId,
                    onValueChange = { chatId = it },
                    label = { Text("Chat ID") },
                )
            }

            // Save button
            Button(onClick = {
                if (selectedType == NotifierType.TELEGRAM) {
                    state.eventSink(
                        AddNotificationMediumScreen.Event.SaveTelegramConfig(
                            botToken = botToken,
                            chatId = chatId,
                        ),
                    )
                }
            }) {
                Text("Save")
            }
        }
    }
}
