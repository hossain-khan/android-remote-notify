package dev.hossain.remotenotify.ui.addalertmedium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.notifier.of
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import timber.log.Timber

@Parcelize
data class ConfigureNotificationMediumScreen(
    val notifierType: NotifierType,
) : Screen {
    data class State(
        val notifierType: NotifierType,
        val isConfigured: Boolean,
        val configValidationResult: ConfigValidationResult,
        val alertMediumConfig: AlertMediumConfig?,
        val showValidationError: Boolean,
        val snackbarMessage: String?,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data class UpdateConfigValue(
            val alertMediumConfig: AlertMediumConfig?,
        ) : Event()

        data object SaveConfig : Event()

        data object TestConfig : Event()

        data object DismissSnackbar : Event()
    }
}

class ConfigureNotificationMediumPresenter
    @AssistedInject
    constructor(
        @Assisted private val screen: ConfigureNotificationMediumScreen,
        @Assisted private val navigator: Navigator,
        private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
    ) : Presenter<ConfigureNotificationMediumScreen.State> {
        @Composable
        override fun present(): ConfigureNotificationMediumScreen.State {
            val scope = rememberCoroutineScope()
            var alertMediumConfig by remember { mutableStateOf<AlertMediumConfig?>(null) }
            var snackbarMessage by remember { mutableStateOf<String?>(null) }
            var isConfigured by remember { mutableStateOf(false) }
            var shouldShowValidationError by remember { mutableStateOf(false) }

            val notificationSender = notifiers.of(senderNotifierType = screen.notifierType)

            val validationResult by produceState(ConfigValidationResult(false, emptyMap()), alertMediumConfig, shouldShowValidationError) {
                val config = alertMediumConfig
                value =
                    if (config == null) {
                        ConfigValidationResult(false, emptyMap())
                    } else {
                        notificationSender.validateConfig(config)
                    }
            }

            LaunchedEffect(Unit) {
                isConfigured = notificationSender.hasValidConfig()
                alertMediumConfig = notificationSender.getConfig()
            }

            return ConfigureNotificationMediumScreen.State(
                notifierType = screen.notifierType,
                isConfigured = isConfigured,
                configValidationResult = validationResult,
                alertMediumConfig = alertMediumConfig,
                showValidationError = shouldShowValidationError,
                snackbarMessage = snackbarMessage,
            ) { event ->
                when (event) {
                    is ConfigureNotificationMediumScreen.Event.SaveConfig -> {
                        scope.launch {
                            shouldShowValidationError = true // Show validation on save attempt
                            if (validationResult.isValid.not()) {
                                return@launch
                            }
                            alertMediumConfig?.let { config ->
                                runCatching {
                                    notificationSender.saveConfig(config)
                                    navigator.pop()
                                }.onFailure {
                                    Timber.e(it, "Error saving config: $it")
                                    snackbarMessage = "Unable to save configuration: ${it.message}"
                                }
                            }
                        }
                    }
                    is ConfigureNotificationMediumScreen.Event.TestConfig -> {
                        scope.launch {
                            try {
                                val success =
                                    withContext(Dispatchers.IO) {
                                        val testNotification = RemoteAlert.BatteryAlert(batteryPercentage = 5)
                                        notificationSender.sendNotification(testNotification)
                                    }
                                snackbarMessage =
                                    if (success) {
                                        "Test notification sent successfully!"
                                    } else {
                                        "Failed to send test notification"
                                    }
                            } catch (e: Exception) {
                                Timber.e(e, "Error sending test notification")
                                snackbarMessage = "Error: ${e.message ?: e.toString()}"
                            }
                        }
                    }

                    ConfigureNotificationMediumScreen.Event.DismissSnackbar -> {
                        snackbarMessage = null
                    }

                    is ConfigureNotificationMediumScreen.Event.UpdateConfigValue -> {
                        shouldShowValidationError = false // Reset validation on config change
                        alertMediumConfig = event.alertMediumConfig
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
    SideEffect { Timber.d("ConfigureNotificationMediumUi: ${state.alertMediumConfig}") }
    Scaffold(
        modifier = modifier,
        snackbarHost = {
            state.snackbarMessage?.let { message ->
                Snackbar(
                    action = {
                        TextButton(onClick = {
                            state.eventSink(ConfigureNotificationMediumScreen.Event.DismissSnackbar)
                        }) {
                            Text("Dismiss")
                        }
                    },
                ) {
                    Text(message)
                }
            }
        },
    ) { innerPadding ->
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

            val onConfigUpdate: (AlertMediumConfig?) -> Unit = {
                state.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(it))
            }

            when (state.notifierType) {
                NotifierType.TELEGRAM -> {
                    TelegramConfigInputUi(
                        alertMediumConfig = state.alertMediumConfig,
                        configValidationResult = state.configValidationResult,
                        shouldShowValidationError = state.showValidationError,
                        onConfigUpdate = onConfigUpdate,
                    )
                }
                NotifierType.WEBHOOK_REST_API -> {
                    WebhookConfigInputUi(
                        alertMediumConfig = state.alertMediumConfig,
                        configValidationResult = state.configValidationResult,
                        shouldShowValidationError = state.showValidationError,
                        onConfigUpdate = onConfigUpdate,
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

            Spacer(modifier = Modifier.height(8.dp))

            // Only show test button if configuration exists
            AnimatedVisibility(
                visible = state.configValidationResult.isValid,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                OutlinedButton(
                    onClick = { state.eventSink(ConfigureNotificationMediumScreen.Event.TestConfig) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Test Configuration")
                }
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
                    configValidationResult = ConfigValidationResult(true, emptyMap()),
                    alertMediumConfig = AlertMediumConfig.TelegramConfig("bot-token", "chat-id"),
                    showValidationError = false,
                    snackbarMessage = null,
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
                    configValidationResult = ConfigValidationResult(true, emptyMap()),
                    alertMediumConfig = AlertMediumConfig.WebhookConfig("https://example.com"),
                    showValidationError = false,
                    snackbarMessage = null,
                    eventSink = {},
                ),
        )
    }
}
