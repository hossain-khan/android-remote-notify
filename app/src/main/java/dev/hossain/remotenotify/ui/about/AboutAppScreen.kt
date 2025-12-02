package dev.hossain.remotenotify.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuitx.effects.LaunchedImpressionEffect
import dev.hossain.remotenotify.BuildConfig
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.analytics.Analytics
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.ui.alertlist.AppUsageEducationSheetUi
import dev.hossain.remotenotify.ui.backup.BackupRestoreScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data object AboutAppScreen : Screen {
    data class State(
        val appVersion: String,
        val showEducationSheet: Boolean,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object GoBack : Event()

        data object OpenGitHubProject : Event()

        data object OpenLearnMoreSheet : Event()

        data object DismissLearnMoreSheet : Event()

        data object OpenBackupRestore : Event()
    }
}

@AssistedInject
class AboutAppPresenter
    constructor(
        @Assisted private val navigator: Navigator,
        private val analytics: Analytics,
    ) : Presenter<AboutAppScreen.State> {
        @Composable
        override fun present(): AboutAppScreen.State {
            val uriHandler = LocalUriHandler.current
            val scope = rememberCoroutineScope()
            var showEducationSheet by remember { mutableStateOf(false) }

            LaunchedImpressionEffect {
                analytics.logScreenView(AboutAppScreen::class)
            }

            val appVersion =
                buildString {
                    append("v")
                    append(BuildConfig.VERSION_NAME)
                    append(" (")
                    append(BuildConfig.GIT_COMMIT_HASH)
                    append(")")
                }

            return AboutAppScreen.State(
                appVersion,
                showEducationSheet = showEducationSheet,
            ) { event ->
                when (event) {
                    AboutAppScreen.Event.GoBack -> {
                        navigator.pop()
                    }

                    AboutAppScreen.Event.OpenGitHubProject -> {
                        uriHandler.openUri("https://github.com/hossain-khan/android-remote-notify")
                    }

                    AboutAppScreen.Event.OpenLearnMoreSheet -> {
                        showEducationSheet = true
                        scope.launch {
                            analytics.logViewTutorial(isComplete = false)
                        }
                    }

                    AboutAppScreen.Event.DismissLearnMoreSheet -> {
                        showEducationSheet = false
                        scope.launch {
                            analytics.logViewTutorial(isComplete = true)
                        }
                    }

                    AboutAppScreen.Event.OpenBackupRestore -> {
                        navigator.goTo(BackupRestoreScreen)
                    }
                }
            }
        }

        @CircuitInject(AboutAppScreen::class, AppScope::class)
        @AssistedFactory
        interface Factory {
            fun create(navigator: Navigator): AboutAppPresenter
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(AboutAppScreen::class, AppScope::class)
@Composable
fun AboutAppScreen(
    state: AboutAppScreen.State,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About App") },
                navigationIcon = {
                    IconButton(onClick = {
                        state.eventSink(AboutAppScreen.Event.GoBack)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                        )
                    }
                },
            )
        },
    ) { contentPaddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(contentPaddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_v3),
                    contentDescription = "App icon",
                    modifier =
                        Modifier
                            .size(96.dp)
                            .padding(top = 16.dp)
                            .align(Alignment.CenterHorizontally),
                )
                AppTagLineWithLinkedText(state.eventSink)
                Spacer(modifier = Modifier.height(32.dp))
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.github_logo_outline),
                    contentDescription = "GitHub Logo Icon",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                    modifier =
                        Modifier
                            .size(84.dp)
                            .align(Alignment.CenterHorizontally),
                )
                Text(
                    text = "Proudly open-source on GitHub",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                TextButton(onClick = {
                    state.eventSink(AboutAppScreen.Event.OpenGitHubProject)
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("View Source") }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    state.eventSink(AboutAppScreen.Event.OpenBackupRestore)
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Backup & Restore") }
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                if (BuildConfig.DEBUG) {
                    // For fun, show Kodee in debug build
                    Image(
                        painter = painterResource(id = R.drawable.kodee_sharing_love),
                        contentDescription = "Kotlin Kodee Mascot",
                        modifier =
                            Modifier
                                .align(Alignment.CenterHorizontally),
                    )
                }
                Text(
                    text = "Version: ${state.appVersion}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp),
                )
            }
        }

        // Add the education sheet
        if (state.showEducationSheet) {
            AppUsageEducationSheetUi(
                sheetState = sheetState,
            ) { state.eventSink(AboutAppScreen.Event.DismissLearnMoreSheet) }
        }
    }
}

@Composable
private fun AppTagLineWithLinkedText(
    eventSink: (AboutAppScreen.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Tag line: Your no-fuss, personal weather alerter.
    val annotatedLinkString =
        buildAnnotatedString {
            append("App that monitors this device's battery and storage levels and ")
            withLink(
                LinkAnnotation.Url(
                    // Dummy URL, not used for this use case.
                    url = "https://hossain.dev",
                    styles =
                        TextLinkStyles(
                            style = SpanStyle(color = MaterialTheme.colorScheme.primary),
                            hoveredStyle = SpanStyle(color = MaterialTheme.colorScheme.secondary),
                        ),
                    linkInteractionListener = {
                        eventSink(AboutAppScreen.Event.OpenLearnMoreSheet)
                    },
                ),
            ) {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("alerts you")
                }
            }
            append(" when the levels are lower than what you've set.")
        }
    Text(
        text = annotatedLinkString,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun AboutAppScreenPreview() {
    val sampleState =
        AboutAppScreen.State(
            appVersion = "v1.8.1 (letzg00)",
            showEducationSheet = false,
            eventSink = {},
        )
    ComposeAppTheme {
        AboutAppScreen(state = sampleState)
    }
}
