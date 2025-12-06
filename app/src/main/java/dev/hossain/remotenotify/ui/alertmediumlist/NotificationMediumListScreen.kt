package dev.hossain.remotenotify.ui.alertmediumlist

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.slack.circuitx.effects.LaunchedImpressionEffect
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.model.configPreviewText
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.ui.alertmediumconfig.ConfigureNotificationMediumScreen
import dev.hossain.remotenotify.worker.DEFAULT_PERIODIC_INTERVAL_MINUTES
import dev.hossain.remotenotify.worker.sendPeriodicWorkRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
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
    /**
     * Represents the state of the [NotificationMediumListScreen].
     */
    data class State(
        /** The interval in minutes for the background worker. */
        val workerIntervalMinutes: Long,
        /** A list of [NotifierMediumInfo] objects representing the available notification mediums. */
        val notifiers: List<NotifierMediumInfo>,
        /** A function to send events from the UI to the presenter. */
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    /**
     * Represents information about a notification medium.
     */
    data class NotifierMediumInfo(
        /** The type of the notifier (e.g., Email, Slack). */
        val notifierType: NotifierType,
        /** The display name of the notification medium. */
        val name: String,
        /** A boolean indicating whether the notification medium is configured. */
        val isConfigured: Boolean,
        /** A preview text of the configuration, if available. */
        val configPreviewText: String? = null,
    )

    /**
     * Represents events that can occur on the [NotificationMediumListScreen].
     */
    sealed class Event : CircuitUiEvent {
        /**
         * Event triggered when the user wants to edit the configuration of a notification medium.
         * @property notifierType The type of the notifier to edit.
         */
        data class EditMediumConfig(
            val notifierType: NotifierType,
        ) : Event()

        /**
         * Event triggered when the user wants to reset the configuration of a notification medium.
         * @property notifierType The type of the notifier to reset.
         */
        data class ResetMediumConfig(
            val notifierType: NotifierType,
        ) : Event()

        /**
         * Event triggered when the worker interval is updated.
         * @property minutes The new worker interval in minutes.
         */
        data class OnWorkerIntervalUpdated(
            val minutes: Long,
        ) : Event()

        /**
         * Event triggered when the user wants to share feedback.
         */
        data object ShareFeedback : Event()

        /**
         * Event triggered when the user wants to navigate back.
         */
        data object NavigateBack : Event()
    }
}

/**
 * Presenter for the [NotificationMediumListScreen].
 *
 * @param navigator The navigator for navigating to other screens.
 * @param appPreferencesDataStore The data store for application preferences.
 * @param notifiers A set of available [NotificationSender] implementations.
 * @param analytics The analytics tracker.
 */
@AssistedInject
class NotificationMediumListPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val appPreferencesDataStore: AppPreferencesDataStore,
        private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
        private val analytics: Analytics,
    ) : Presenter<NotificationMediumListScreen.State> {
        private val workerIntervalFlow = MutableStateFlow(DEFAULT_PERIODIC_INTERVAL_MINUTES)

        @OptIn(FlowPreview::class)
        @Composable
        override fun present(): NotificationMediumListScreen.State {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            // Use remember and mutableStateOf to make the list observable
            var notifierMediumInfoList by remember { mutableStateOf(emptyList<NotificationMediumListScreen.NotifierMediumInfo>()) }

            LaunchedImpressionEffect {
                analytics.logScreenView(NotificationMediumListScreen::class)
            }

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

                    NotificationMediumListScreen.Event.ShareFeedback -> {
                        scope.launch {
                            analytics.logSendFeedback()
                        }
                    }
                }
            }
        }

        /**
         * Factory for creating instances of [NotificationMediumListPresenter].
         */
        @CircuitInject(NotificationMediumListScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            /**
             * Creates an instance of [NotificationMediumListPresenter].
             * @param navigator The navigator for navigating to other screens.
             * @return An instance of [NotificationMediumListPresenter].
             */
            fun create(navigator: Navigator): NotificationMediumListPresenter
        }
    }

/**
 * Composable function for the notification medium list UI.
 *
 * @param state The current state of the UI.
 * @param modifier The modifier to apply to the UI.
 */
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
                            contentDescription = "Navigate back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Add a focus requester to the first item
            // This is a workaround to focus on the first item when screen is loaded
            // Otherwise the check frequency slider and share feedback was staling focus, resulting in scrolling to bottom
            item(key = "top-focus") {
                val focusRequester = remember { FocusRequester() }
                Box(
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .focusable()
                            .height(1.dp),
                ) {}

                LaunchedEffect(Unit) {
                    delay(100)
                    focusRequester.requestFocus()
                }
            }
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
                FeedbackAndRequestMediumUi {
                    state.eventSink(NotificationMediumListScreen.Event.ShareFeedback)
                }
            }
        }
    }
}

/**
 * Private composable function to display a card for a notifier medium.
 *
 * @param notifier The [NotificationMediumListScreen.NotifierMediumInfo] to display.
 * @param onEditConfiguration Callback invoked when the edit configuration action is triggered.
 * @param onResetConfiguration Callback invoked when the reset configuration action is triggered.
 * @param modifier The modifier to apply to the card.
 */
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
                    contentDescription = "${notifier.name} notification medium",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp),
                )
            },
            trailingContent = {
                Row {
                    IconButton(onClick = onEditConfiguration) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configure ${notifier.name}",
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
                                contentDescription = "Reset ${notifier.name} configuration",
                            )
                        }
                    }
                }
            },
        )
    }
}

/**
 * Private extension function to get the icon resource ID for a [NotifierType].
 *
 * @return The drawable resource ID for the icon.
 */
@DrawableRes
private fun NotifierType.iconResId(): Int =
    when (this) {
        NotifierType.EMAIL -> R.drawable.mail_24dp
        NotifierType.TELEGRAM -> R.drawable.telegram_logo_outline
        NotifierType.TWILIO -> R.drawable.twilio_logo_outline
        NotifierType.WEBHOOK_DISCORD -> R.drawable.discord_logo_outline
        NotifierType.WEBHOOK_REST_API -> R.drawable.webhook_24dp
        NotifierType.WEBHOOK_SLACK_WORKFLOW -> R.drawable.slack_logo_outline
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
                                notifierType = NotifierType.WEBHOOK_SLACK_WORKFLOW,
                                name = "Slack",
                                isConfigured = false,
                            ),
                            NotificationMediumListScreen.NotifierMediumInfo(
                                notifierType = NotifierType.WEBHOOK_DISCORD,
                                name = "Discord",
                                isConfigured = true,
                                configPreviewText = "https://discord.com/api/webhooks/...",
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
