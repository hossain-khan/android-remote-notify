package dev.hossain.remotenotify.ui.alertmediumconfig

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.PopResult
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.EmailQuotaManager
import dev.hossain.remotenotify.data.EmailQuotaManager.Companion.ValidationKeys.EMAIL_DAILY_QUOTA
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.DeviceAlert
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.notifier.of
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.ui.alertmediumconfig.ConfigureNotificationMediumScreen.ConfigurationResult
import dev.hossain.remotenotify.utils.PreformattedCodeBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.intellij.lang.annotations.Language
import timber.log.Timber

@Parcelize
data class ConfigureNotificationMediumScreen constructor(
    val notifierType: NotifierType,
) : Screen {
    data class State(
        val notifierType: NotifierType,
        val isConfigured: Boolean,
        val configValidationResult: ConfigValidationResult,
        val alertMediumConfig: AlertMediumConfig?,
        val showValidationError: Boolean,
        /**
         * Sample JSON payload preview, only shown for webhook notifier.
         */
        val sampleJsonPayload: String,
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

        data object NavigateBack : Event()
    }

    @Parcelize
    sealed class ConfigurationResult : PopResult {
        @Parcelize
        data class Configured(
            val notifierType: NotifierType,
        ) : ConfigurationResult()

        @Parcelize
        data object NotConfigured : ConfigurationResult()
    }
}

class ConfigureNotificationMediumPresenter
    @AssistedInject
    constructor(
        @Assisted private val screen: ConfigureNotificationMediumScreen,
        @Assisted private val navigator: Navigator,
        private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
        private val emailQuotaManager: EmailQuotaManager,
        private val alertFormatter: AlertFormatter,
    ) : Presenter<ConfigureNotificationMediumScreen.State> {
        @Composable
        override fun present(): ConfigureNotificationMediumScreen.State {
            val scope = rememberCoroutineScope()
            var alertMediumConfig by remember { mutableStateOf<AlertMediumConfig?>(null) }
            var snackbarMessage by remember { mutableStateOf<String?>(null) }
            var isConfigured by remember { mutableStateOf(false) }
            var shouldShowValidationError by remember { mutableStateOf(false) }
            val sampleJsonPayload by remember {
                mutableStateOf(
                    alertFormatter.format(
                        RemoteAlert.StorageAlert(
                            storageMinSpaceGb = 5,
                        ),
                        DeviceAlert.FormatType.JSON,
                    ),
                )
            }

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
                sampleJsonPayload = sampleJsonPayload,
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
                                    navigator.pop(ConfigurationResult.Configured(screen.notifierType))
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
                                // Save the config first, which will be used to test out config
                                val config = alertMediumConfig!!
                                notificationSender.saveConfig(config)

                                // For email notifier, validate quota before sending test
                                if (screen.notifierType == NotifierType.EMAIL) {
                                    val emailValidationResult = emailQuotaManager.validateQuota()
                                    if (!emailValidationResult.isValid) {
                                        // Show the quota exceeded message if present
                                        emailValidationResult.errors[EMAIL_DAILY_QUOTA]?.let { error ->
                                            snackbarMessage = error
                                            return@launch
                                        }
                                    }
                                }

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

                    ConfigureNotificationMediumScreen.Event.NavigateBack -> {
                        if (isConfigured) {
                            navigator.pop(ConfigurationResult.Configured(screen.notifierType))
                        } else {
                            navigator.pop(ConfigurationResult.NotConfigured)
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(screen = ConfigureNotificationMediumScreen::class, scope = AppScope::class)
@Composable
fun ConfigureNotificationMediumUi(
    state: ConfigureNotificationMediumScreen.State,
    modifier: Modifier = Modifier,
) {
    SideEffect { Timber.d("ConfigureNotificationMediumUi: ${state.alertMediumConfig}") }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Configure ${state.notifierType.displayName}") },
                navigationIcon = {
                    IconButton(onClick = {
                        state.eventSink(ConfigureNotificationMediumScreen.Event.NavigateBack)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
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
            NotifierConfigInputUi(state)

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

            // Additional optional UI show based on notifier type
            NotifierConfigSuffixUi(state)
        }
    }
}

@Composable
private fun NotifierConfigInputUi(state: ConfigureNotificationMediumScreen.State) {
    val onConfigUpdate: (AlertMediumConfig?) -> Unit = {
        state.eventSink(ConfigureNotificationMediumScreen.Event.UpdateConfigValue(it))
    }

    val configInputUi: @Composable () -> Unit = {
        when (state.notifierType) {
            NotifierType.EMAIL ->
                EmailConfigInputUi(
                    state.alertMediumConfig,
                    state.configValidationResult,
                    state.showValidationError,
                    onConfigUpdate,
                )

            NotifierType.WEBHOOK_SLACK_WORKFLOW ->
                SlackWebhookConfigInputUi(
                    state.alertMediumConfig,
                    state.configValidationResult,
                    state.showValidationError,
                    onConfigUpdate,
                )

            NotifierType.TELEGRAM ->
                TelegramConfigInputUi(
                    state.alertMediumConfig,
                    state.configValidationResult,
                    state.showValidationError,
                    onConfigUpdate,
                )

            NotifierType.WEBHOOK_REST_API ->
                WebhookConfigInputUi(
                    state.alertMediumConfig,
                    state.configValidationResult,
                    state.showValidationError,
                    onConfigUpdate,
                )

            NotifierType.TWILIO ->
                TwilioConfigInputUi(
                    state.alertMediumConfig,
                    state.configValidationResult,
                    state.showValidationError,
                    onConfigUpdate,
                )
        }
    }
    configInputUi()
}

/**
 * Additional UI shown after the configure button and test configuration button.
 */
@Composable
private fun NotifierConfigSuffixUi(
    state: ConfigureNotificationMediumScreen.State,
    modifier: Modifier = Modifier,
) {
    when (state.notifierType) {
        NotifierType.WEBHOOK_REST_API -> {
            // For webhook, show preview of JSON payload
            Column(modifier = modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.height(32.dp))

                Text("Sample JSON Payload", style = MaterialTheme.typography.titleSmall)
                PreformattedCodeBlock(codeBlock = state.sampleJsonPayload, modifier = Modifier.fillMaxWidth())
            }
        }
        else -> {
            // No suffix for other notifiers
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
                    sampleJsonPayload = "",
                    eventSink = {},
                ),
        )
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewWebhookConfigurationUi() {
    ComposeAppTheme {
        ConfigureNotificationMediumUi(
            state =
                ConfigureNotificationMediumScreen.State(
                    notifierType = NotifierType.WEBHOOK_REST_API,
                    isConfigured = true,
                    configValidationResult = ConfigValidationResult(true, emptyMap()),
                    alertMediumConfig = AlertMediumConfig.WebhookConfig("https://example.com"),
                    showValidationError = false,
                    snackbarMessage = null,
                    sampleJsonPayload = SAMPLE_JSON,
                    eventSink = {},
                ),
        )
    }
}

@Language("JSON")
private const val SAMPLE_JSON = """
{
  "alertType": "STORAGE",
  "deviceModel": "Samsung SM-S911W",
  "androidVersion": "14",
  "availableStorageGb": 2,
  "isoDateTime": "2025-02-19T18:47:27.899442"
}
"""
