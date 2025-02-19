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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dev.hossain.remotenotify.BuildConfig
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.theme.ComposeAppTheme
import kotlinx.parcelize.Parcelize

@Parcelize
data object AboutAppScreen : Screen {
    data class State(
        val appVersion: String,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object GoBack : Event()

        data object OpenGitHubProject : Event()
    }
}

class AboutAppPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
    ) : Presenter<AboutAppScreen.State> {
        @Composable
        override fun present(): AboutAppScreen.State {
            val uriHandler = LocalUriHandler.current

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
            ) { event ->
                when (event) {
                    AboutAppScreen.Event.GoBack -> {
                        navigator.pop()
                    }

                    AboutAppScreen.Event.OpenGitHubProject -> {
                        uriHandler.openUri("https://github.com/hossain-khan/android-remote-notify")
                    }
                }
            }
        }

        @CircuitInject(AboutAppScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
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
                Text("This app monitors Android device battery and storage, alerting you to low levels so you can take action.")
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
            append("Your no-fuss, personal weather ")
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
                    },
                ),
            ) {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("alerter")
                }
            }
            append(".")
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
fun AboutAppScreenPreview() {
    val sampleState =
        AboutAppScreen.State(
            appVersion = "v1.0.0 (b135e2a)",
            eventSink = {},
        )
    ComposeAppTheme {
        AboutAppScreen(state = sampleState)
    }
}
