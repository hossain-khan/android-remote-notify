package dev.hossain.remotenotify.ui.alertmediumlist

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.foundation.rememberAnsweringNavigator
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.configPreviewText
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.ui.alertmediumconfig.ConfigureNotificationMediumScreen
import dev.hossain.remotenotify.worker.DEFAULT_PERIODIC_INTERVAL_MINUTES
import dev.hossain.remotenotify.worker.sendPeriodicWorkRequest
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber

/**
 * Screen to list all notification mediums that allows user to configure, edit, and reset.
 * This also allows user to configure worker interval using [WorkerConfigCard].
 */
@Parcelize
data object NotificationMediumListScreen : Screen {
    data class State(
        val workerIntervalMinutes: Long,
        val notifiers: List<NotifierMediumInfo>,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    data class NotifierMediumInfo(
        val notifierType: NotifierType,
        val name: String,
        val isConfigured: Boolean,
        val configPreviewText: String? = null,
    )

    sealed class Event : CircuitUiEvent {
        data class EditMediumConfig(
            val notifierType: NotifierType,
        ) : Event()

        data class ResetMediumConfig(
            val notifierType: NotifierType,
        ) : Event()

        data class OnWorkerIntervalUpdated(
            val minutes: Long,
        ) : Event()

        data object NavigateBack : Event()
    }
}

class NotificationMediumListPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val appPreferencesDataStore: AppPreferencesDataStore,
        private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
    ) : Presenter<NotificationMediumListScreen.State> {
        private val workerIntervalFlow = MutableStateFlow(DEFAULT_PERIODIC_INTERVAL_MINUTES)

        @OptIn(FlowPreview::class)
        @Composable
        override fun present(): NotificationMediumListScreen.State {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            // Use remember and mutableStateOf to make the list observable
            var notifierMediumInfoList by remember { mutableStateOf(emptyList<NotificationMediumListScreen.NotifierMediumInfo>()) }

            // Collect worker interval directly from DataStore preferences
            val workerIntervalMinutes by appPreferencesDataStore.workerIntervalFlow
                .collectAsState(initial = DEFAULT_PERIODIC_INTERVAL_MINUTES)

            val configureMediumNavigator =
                rememberAnsweringNavigator<ConfigureNotificationMediumScreen.ConfigurationResult>(navigator) { result ->
                    Timber.d("ConfigureNotificationMediumScreen result received: $result")
                    when (result) {
                        is ConfigureNotificationMediumScreen.ConfigurationResult.Configured -> {
                            scope.launch {
                                // Get the last saved interval
                                val intervalMinutes = appPreferencesDataStore.workerIntervalFlow.first()

                                Timber.d(
                                    "Notifier configured: ${result.notifierType}, initializing worker with interval: $intervalMinutes minutes",
                                )

                                // Initialize/update the worker
                                sendPeriodicWorkRequest(context, intervalMinutes)
                            }
                        }
                        ConfigureNotificationMediumScreen.ConfigurationResult.NotConfigured -> {
                            // Do nothing
                        }
                    }
                }

            // Helper function to update the list
            suspend fun updateNotifierList() {
                notifierMediumInfoList =
                    notifiers
                        .sortedBy { it.notifierType.displayName }
                        .map { sender ->
                            NotificationMediumListScreen.NotifierMediumInfo(
                                notifierType = sender.notifierType,
                                name = sender.notifierType.displayName,
                                isConfigured = sender.hasValidConfig(),
                                configPreviewText = if (sender.hasValidConfig()) sender.getConfig().configPreviewText() else null,
                            )
                        }
            }

            // Load initial state
            LaunchedEffect(Unit) {
                updateNotifierList()
            }

            LaunchedEffect(Unit) {
                // Set up debounced updates to avoid frequent worker setup
                workerIntervalFlow
                    // Wait for 1 second of inactivity
                    .debounce(1_000L)
                    .filter {
                        it != DEFAULT_PERIODIC_INTERVAL_MINUTES
                    }.collect { minutes ->
                        Timber.d("Worker interval updated: $minutes minutes")
                        appPreferencesDataStore.saveWorkerInterval(minutes)
                        // Also setup the worker interval
                        sendPeriodicWorkRequest(context = context, repeatIntervalMinutes = minutes)
                    }
            }

            return NotificationMediumListScreen.State(
                workerIntervalMinutes = workerIntervalMinutes,
                notifiers = notifierMediumInfoList,
            ) { event ->
                when (event) {
                    is NotificationMediumListScreen.Event.EditMediumConfig -> {
                        configureMediumNavigator.goTo(ConfigureNotificationMediumScreen(event.notifierType))
                    }
                    is NotificationMediumListScreen.Event.ResetMediumConfig -> {
                        scope.launch {
                            notifiers.find { it.notifierType == event.notifierType }?.clearConfig()
                            // Update the list after clearing config
                            updateNotifierList()
                        }
                    }
                    NotificationMediumListScreen.Event.NavigateBack -> {
                        navigator.pop()
                    }

                    is NotificationMediumListScreen.Event.OnWorkerIntervalUpdated -> {
                        workerIntervalFlow.value = event.minutes // Trigger debounced update
                    }
                }
            }
        }

        @CircuitInject(NotificationMediumListScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): NotificationMediumListPresenter
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(NotificationMediumListScreen::class, AppScope::class)
@Composable
fun NotificationMediumListUi(
    state: NotificationMediumListScreen.State,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Notification Mediums") },
                navigationIcon = {
                    IconButton(onClick = {
                        state.eventSink(NotificationMediumListScreen.Event.NavigateBack)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = state.notifiers,
                key = { it.notifierType },
            ) { notifier ->
                NotifierCard(
                    notifier = notifier,
                    onEditConfiguration = {
                        state.eventSink(
                            NotificationMediumListScreen.Event.EditMediumConfig(notifier.notifierType),
                        )
                    },
                    onResetConfiguration = {
                        state.eventSink(
                            NotificationMediumListScreen.Event.ResetMediumConfig(notifier.notifierType),
                        )
                    },
                )
            }
            // Add this item right after your existing notifiers items
            item(key = "worker-config") {
                WorkerConfigCard(
                    state = state,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                )
            }
            item(key = "bottom-feedback") {
                FeedbackAndRequestMediumUi()
            }
        }
    }
}

@Composable
private fun NotifierCard(
    notifier: NotificationMediumListScreen.NotifierMediumInfo,
    onEditConfiguration: () -> Unit,
    onResetConfiguration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        ListItem(
            headlineContent = {
                Text(text = notifier.name, style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = {
                if (notifier.isConfigured) {
                    notifier.configPreviewText?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } ?: Text(
                        text = "Configured",
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = "Not Configured",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = notifier.notifierType.iconResId()),
                    contentDescription = "Notification Icon",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp),
                )
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onEditConfiguration) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Edit",
                        )
                    }
                    AnimatedVisibility(
                        visible = notifier.isConfigured,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally(),
                    ) {
                        IconButton(onClick = onResetConfiguration) {
                            Icon(
                                painter = painterResource(id = R.drawable.reset_settings_24dp),
                                contentDescription = "Delete",
                            )
                        }
                    }
                }
            },
        )
    }
}

@DrawableRes
private fun NotifierType.iconResId(): Int =
    when (this) {
        NotifierType.EMAIL -> R.drawable.mail_24dp
        NotifierType.TELEGRAM -> R.drawable.telegram_logo_outline
        NotifierType.TWILIO -> R.drawable.twilio_logo_outline
        NotifierType.WEBHOOK_REST_API -> R.drawable.webhook_24dp
    }

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewNotificationMediumListUi() {
    ComposeAppTheme {
        NotificationMediumListUi(
            state =
                NotificationMediumListScreen.State(
                    workerIntervalMinutes = 60,
                    notifiers =
                        listOf(
                            NotificationMediumListScreen.NotifierMediumInfo(
                                notifierType = NotifierType.EMAIL,
                                name = "Email",
                                isConfigured = true,
                                configPreviewText = "user@example.com",
                            ),
                            NotificationMediumListScreen.NotifierMediumInfo(
                                notifierType = NotifierType.TELEGRAM,
                                name = "Telegram",
                                isConfigured = false,
                            ),
                            NotificationMediumListScreen.NotifierMediumInfo(
                                notifierType = NotifierType.TWILIO,
                                name = "Twilio SMS",
                                isConfigured = true,
                                configPreviewText = "+1234...7890",
                            ),
                            NotificationMediumListScreen.NotifierMediumInfo(
                                notifierType = NotifierType.WEBHOOK_REST_API,
                                name = "Webhook",
                                isConfigured = false,
                            ),
                        ),
                    eventSink = {},
                ),
        )
    }
}
