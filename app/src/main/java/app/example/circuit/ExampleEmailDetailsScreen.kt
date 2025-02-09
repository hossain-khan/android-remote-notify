package app.example.circuit

// -------------------------------------------------------------------------------------
//
// THIS IS AN EXAMPLE FILE WITH CIRCUIT SCREENS AND PRESENTERS
// Example content is taken from https://slackhq.github.io/circuit/tutorial/
//
// You should delete this file and create your own screens with presenters.
//
//  -------------------------------------------------------------------------------------

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.example.data.Email
import app.example.data.ExampleEmailRepository
import app.example.data.ExampleEmailValidator
import app.example.di.AppScope
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.parcelize.Parcelize

// See https://slackhq.github.io/circuit/screen/
@Parcelize
data class DetailScreen(
    val emailId: String,
) : Screen {
    data class State(
        val email: Email,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object BackClicked : Event()
    }
}

// See https://slackhq.github.io/circuit/presenter/
class DetailPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        @Assisted private val screen: DetailScreen,
        private val emailRepository: ExampleEmailRepository,
        private val exampleEmailValidator: ExampleEmailValidator,
    ) : Presenter<DetailScreen.State> {
        @Composable
        override fun present(): DetailScreen.State {
            val email = emailRepository.getEmail(screen.emailId)

            // Example usage of the validator that is injected in this presenter
            val allValidEmail = email.recipients.all { exampleEmailValidator.isValidEmail(it) }
            Log.d("DetailPresenter", "Is ${email.recipients} valid: $allValidEmail")

            return DetailScreen.State(email) { event ->
                when (event) {
                    DetailScreen.Event.BackClicked -> navigator.pop()
                }
            }
        }

        @CircuitInject(DetailScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(
                navigator: Navigator,
                screen: DetailScreen,
            ): DetailPresenter
        }
    }

@CircuitInject(DetailScreen::class, AppScope::class)
@Composable
fun EmailDetailContent(
    state: DetailScreen.State,
    modifier: Modifier = Modifier,
) {
    val email = state.email
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier.padding(innerPadding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(
                    Icons.Default.Person,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Magenta)
                            .padding(4.dp),
                    colorFilter = ColorFilter.tint(Color.White),
                    contentDescription = null,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row {
                        Text(
                            text = email.sender,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = email.timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alpha(0.5f),
                        )
                    }
                    Text(text = email.subject, style = MaterialTheme.typography.labelMedium)
                    Row {
                        Text(
                            "To: ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = email.recipients.joinToString(","),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.alpha(0.5f),
                        )
                    }
                }
            }
            @Suppress("DEPRECATION") // Deprecated in Android but not yet available in CM
            (Divider(modifier = Modifier.padding(vertical = 16.dp)))
            Text(text = email.body, style = MaterialTheme.typography.bodyMedium)

            Button(
                onClick = { state.eventSink(DetailScreen.Event.BackClicked) },
                modifier = Modifier.padding(top = 16.dp).align(Alignment.End),
            ) {
                Text("Go Back")
            }
        }
    }
}

/** A simple email item to show in a list. */
@Composable
fun EmailItem(
    email: Email,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier.clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            Icons.Default.Person,
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Magenta)
                    .padding(4.dp),
            colorFilter = ColorFilter.tint(Color.White),
            contentDescription = null,
        )
        Column {
            Row {
                Text(
                    text = email.sender,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = email.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.alpha(0.5f),
                )
            }

            Text(text = email.subject, style = MaterialTheme.typography.labelLarge)
            Text(
                text = email.body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(0.5f),
            )
        }
    }
}
