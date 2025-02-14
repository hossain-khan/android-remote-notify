package dev.hossain.remotenotify.ui.addalertmedium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import dev.hossain.remotenotify.data.TelegramConfigDataStore
import dev.hossain.remotenotify.data.WebhookConfigDataStore
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.notifier.NotifierType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
data class ConfigureNotificationMediumScreen(
    val notifierType: NotifierType,
) : Screen {
    data class State(
        val notifierType: NotifierType,
        val isConfigured: Boolean,
        val botToken: String,
        val chatId: String,
        val webhookUrl: String,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object SaveConfig : Event()

        data class OnBotTokenUpdated(
            val botToken: String,
        ) : Event()

        data class OnChatIdUpdated(
            val chatId: String,
        ) : Event()

        data class OnWebhookUrlUpdated(
            val url: String,
        ) : Event()
    }
}

class ConfigureNotificationMediumPresenter
    @AssistedInject
    constructor(
        @Assisted private val screen: ConfigureNotificationMediumScreen,
        @Assisted private val navigator: Navigator,
        private val telegramConfigDataStore: TelegramConfigDataStore,
        private val webhookConfigDataStore: WebhookConfigDataStore,
    ) : Presenter<ConfigureNotificationMediumScreen.State> {
        @Composable
        override fun present(): ConfigureNotificationMediumScreen.State {
            val scope = rememberCoroutineScope()
            var savedBotToken by remember { mutableStateOf("") }
            var savedChatId by remember { mutableStateOf("") }
            var savedWebhookUrl by remember { mutableStateOf("") }
            var isConfigured by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                when (screen.notifierType) {
                    NotifierType.TELEGRAM -> {
                        isConfigured = telegramConfigDataStore.hasValidConfig()
                        savedBotToken = telegramConfigDataStore.botToken.first() ?: ""
                        savedChatId = telegramConfigDataStore.chatId.first() ?: ""
                    }

                    NotifierType.WEBHOOK_REST_API -> {
                        savedWebhookUrl = webhookConfigDataStore.webhookUrl.first() ?: ""
                    }
                }
            }

            return ConfigureNotificationMediumScreen.State(
                notifierType = screen.notifierType,
                isConfigured = isConfigured,
                botToken = savedBotToken,
                chatId = savedChatId,
                webhookUrl = savedWebhookUrl,
            ) { event ->
                when (event) {
                    is ConfigureNotificationMediumScreen.Event.SaveConfig -> {
                        scope.launch {
                            when (screen.notifierType) {
                                NotifierType.TELEGRAM -> {
                                    runCatching {
                                        telegramConfigDataStore.saveBotToken(savedBotToken)
                                        telegramConfigDataStore.saveChatId(savedChatId)
                                    }.onFailure {
                                        Timber.e(it, "Error saving Telegram config")
                                    }
                                }

                                NotifierType.WEBHOOK_REST_API -> {
                                    runCatching {
                                        webhookConfigDataStore.saveWebhookUrl(savedWebhookUrl)
                                    }.onFailure {
                                        Timber.e(it, "Error saving Webhook config")
                                    }
                                }
                            }
                            navigator.pop()
                        }
                    }

                    is ConfigureNotificationMediumScreen.Event.OnBotTokenUpdated -> {
                        savedBotToken = event.botToken
                    }

                    is ConfigureNotificationMediumScreen.Event.OnChatIdUpdated -> {
                        savedChatId = event.chatId
                    }

                    is ConfigureNotificationMediumScreen.Event.OnWebhookUrlUpdated -> {
                        savedWebhookUrl = event.url
                    }
                }
            }
        }

        @CircuitInject(ConfigureNotificationMediumScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(
                screen: ConfigureNotificationMediumScreen,
                navigator: Navigator,
            ): ConfigureNotificationMediumPresenter
        }
    }

@CircuitInject(screen = ConfigureNotificationMediumScreen::class, scope = AppScope::class)
@Composable
fun ConfigureNotificationMediumUi(
    state: ConfigureNotificationMediumScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            Text(
                text = "Configure ${state.notifierType.displayName}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            when (state.notifierType) {
                NotifierType.TELEGRAM -> {
                    TextField(
                        value = state.botToken,
                        onValueChange = {
                            state.eventSink(
                                ConfigureNotificationMediumScreen.Event.OnBotTokenUpdated(it),
                            )
                        },
                        label = { Text("Bot Token") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextField(
                        value = state.chatId,
                        onValueChange = {
                            state.eventSink(
                                ConfigureNotificationMediumScreen.Event.OnChatIdUpdated(it),
                            )
                        },
                        label = { Text("Chat ID") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                NotifierType.WEBHOOK_REST_API -> {
                    TextField(
                        value = state.webhookUrl,
                        onValueChange = {
                            state.eventSink(
                                ConfigureNotificationMediumScreen.Event.OnWebhookUrlUpdated(it),
                            )
                        },
                        label = { Text("Webhook URL") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Enter the URL to receive notifications") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { state.eventSink(ConfigureNotificationMediumScreen.Event.SaveConfig) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isConfigured) "Update Configuration" else "Save Configuration")
            }
        }
    }
}

@Preview
@Composable
private fun PreviewTelegramConfigurationUi() {
    MaterialTheme {
        ConfigureNotificationMediumUi(
            state =
                ConfigureNotificationMediumScreen.State(
                    notifierType = NotifierType.TELEGRAM,
                    isConfigured = false,
                    botToken = "1234567890:ABCdefGHIjklMNOpqrsTUVwxyz",
                    chatId = "123456789",
                    webhookUrl = "",
                    eventSink = {},
                ),
        )
    }
}

@Preview
@Composable
private fun PreviewWebhookConfigurationUi() {
    MaterialTheme {
        ConfigureNotificationMediumUi(
            state =
                ConfigureNotificationMediumScreen.State(
                    notifierType = NotifierType.WEBHOOK_REST_API,
                    isConfigured = true,
                    botToken = "",
                    chatId = "",
                    webhookUrl = "https://api.example.com/webhook",
                    eventSink = {},
                ),
        )
    }
}
