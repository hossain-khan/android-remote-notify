package dev.hossain.remotenotify.ui.addalertmedium

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
import timber.log.Timber

@Parcelize
data object ConfigureNotificationMediumScreen : Screen {
    data class State(
        val botToken: String,
        val chatId: String,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object SaveTelegramConfig : Event()

        data class OnBotTokenUpdated(
            val botToken: String,
        ) : Event()

        data class OnChatIdUpdated(
            val chatId: String,
        ) : Event()
    }
}

class ConfigureNotificationMediumPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val telegramConfigDataStore: TelegramConfigDataStore,
    ) : Presenter<ConfigureNotificationMediumScreen.State> {
        @Composable
        override fun present(): ConfigureNotificationMediumScreen.State {
            val scope = rememberCoroutineScope()
            var savedBotToken by remember { mutableStateOf("") }
            var savedChatId by remember { mutableStateOf("") }

            LaunchedEffect(Unit) {
                savedBotToken = telegramConfigDataStore.botToken.first() ?: ""
                savedChatId = telegramConfigDataStore.chatId.first() ?: ""
            }

            return ConfigureNotificationMediumScreen.State(savedBotToken, savedChatId) { event ->
                when (event) {
                    is ConfigureNotificationMediumScreen.Event.SaveTelegramConfig -> {
                        scope.launch {
                            runCatching {
                                telegramConfigDataStore.saveBotToken(savedBotToken)
                            }.onFailure { Timber.e(it, "Got error saving bot") }

                            runCatching {
                                telegramConfigDataStore.saveChatId(savedChatId)
                            }.onFailure { Timber.e(it, "Got error chat id") }
                            navigator.pop()
                        }
                    }

                    is ConfigureNotificationMediumScreen.Event.OnBotTokenUpdated -> {
                        savedBotToken = event.botToken
                    }
                    is ConfigureNotificationMediumScreen.Event.OnChatIdUpdated -> {
                        savedChatId = event.chatId
                    }
                }
            }
        }

        @CircuitInject(ConfigureNotificationMediumScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): ConfigureNotificationMediumPresenter
        }
    }

@CircuitInject(screen = ConfigureNotificationMediumScreen::class, scope = AppScope::class)
@Composable
fun ConfigureNotificationMediumUi(
    state: ConfigureNotificationMediumScreen.State,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(NotifierType.TELEGRAM) }

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
                    value = state.botToken,
                    onValueChange = {
                        state.eventSink(
                            ConfigureNotificationMediumScreen.Event.OnBotTokenUpdated(it),
                        )
                    },
                    label = { Text("Bot Token") },
                )

                Text("Enter Telegram Chat ID")
                // Add TextField for chatId input
                TextField(
                    value = state.chatId,
                    onValueChange = {
                        state.eventSink(
                            ConfigureNotificationMediumScreen.Event.OnChatIdUpdated(it),
                        )
                    },
                    label = { Text("Chat ID") },
                )
            }

            // Save button
            Button(onClick = {
                if (selectedType == NotifierType.TELEGRAM) {
                    state.eventSink(ConfigureNotificationMediumScreen.Event.SaveTelegramConfig)
                }
            }) {
                Text("Save")
            }
        }
    }
}
