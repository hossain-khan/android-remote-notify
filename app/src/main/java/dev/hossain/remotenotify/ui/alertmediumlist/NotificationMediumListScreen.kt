package dev.hossain.remotenotify.ui.alertmediumlist

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.ui.addalertmedium.ConfigureNotificationMediumScreen
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object NotificationMediumListScreen : Screen {
    data class State(
        val notifiers: List<NotifierInfo>,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    data class NotifierInfo(
        val id: NotifierType,
        val name: String,
        val isConfigured: Boolean,
    )

    sealed class Event : CircuitUiEvent {
        data class EditMedium(
            val id: NotifierType,
        ) : Event()

        data class DeleteMedium(
            val id: NotifierType,
        ) : Event()

        data object NavigateBack : Event()
    }
}

class NotificationMediumListPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
    ) : Presenter<NotificationMediumListScreen.State> {
        @Composable
        override fun present(): NotificationMediumListScreen.State {
            val scope = rememberCoroutineScope()
            // Use remember and mutableStateOf to make the list observable
            var notifierInfoList by remember { mutableStateOf(emptyList<NotificationMediumListScreen.NotifierInfo>()) }

            // Helper function to update the list
            suspend fun updateNotifierList() {
                notifierInfoList =
                    notifiers
                        .sortedBy { it.notifierType.displayName }
                        .map { sender ->
                            NotificationMediumListScreen.NotifierInfo(
                                id = sender.notifierType,
                                name = sender.notifierType.displayName,
                                isConfigured = sender.hasValidConfig(),
                            )
                        }
            }

            // Load initial state
            LaunchedEffect(Unit) {
                updateNotifierList()
            }

            return NotificationMediumListScreen.State(
                notifiers = notifierInfoList,
            ) { event ->
                when (event) {
                    is NotificationMediumListScreen.Event.EditMedium -> {
                        navigator.goTo(ConfigureNotificationMediumScreen(event.id))
                    }
                    is NotificationMediumListScreen.Event.DeleteMedium -> {
                        scope.launch {
                            notifiers.find { it.notifierType == event.id }?.clearConfig()
                            // Update the list after clearing config
                            updateNotifierList()
                        }
                    }
                    NotificationMediumListScreen.Event.NavigateBack -> {
                        navigator.pop()
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
        if (state.notifiers.isEmpty()) {
            EmptyMediumState(
                modifier =
                    Modifier
                        .padding(padding)
                        .fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.notifiers) { notifier ->
                    NotifierCard(
                        notifier = notifier,
                        onEditConfiguration = { state.eventSink(NotificationMediumListScreen.Event.EditMedium(notifier.id)) },
                        onResetConfiguration = { state.eventSink(NotificationMediumListScreen.Event.DeleteMedium(notifier.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotifierCard(
    notifier: NotificationMediumListScreen.NotifierInfo,
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
                Text(
                    text = if (notifier.isConfigured) "Configured" else "Not Configured",
                    color =
                        if (notifier.isConfigured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = notifier.id.iconResId()),
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

@Composable
private fun EmptyMediumState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No notification mediums configured",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@DrawableRes
private fun NotifierType.iconResId(): Int =
    when (this) {
        NotifierType.TELEGRAM -> R.drawable.telegram_logo_outline
        NotifierType.WEBHOOK_REST_API -> R.drawable.webhook_24dp
    }
