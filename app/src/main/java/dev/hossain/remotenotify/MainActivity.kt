package dev.hossain.remotenotify

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.remember
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuitx.gesturenavigation.GestureNavigationDecorationFactory
import dev.hossain.remotenotify.di.ActivityKey
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.ui.alertlist.AlertsListScreen
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

@ContributesIntoMap(AppScope::class, binding<Activity>())
@ActivityKey(MainActivity::class)
@Inject
class MainActivity
    constructor(
        private val circuit: Circuit,
    ) : ComponentActivity() {
        @OptIn(ExperimentalSharedTransitionApi::class)
        override fun onCreate(savedInstanceState: Bundle?) {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)

            setContent {
                ComposeAppTheme {
                    // See https://slackhq.github.io/circuit/navigation/
                    val backStack = rememberSaveableBackStack(root = AlertsListScreen)
                    val navigator = rememberCircuitNavigator(backStack)

                    // See https://slackhq.github.io/circuit/circuit-content/
                    CircuitCompositionLocals(circuit) {
                        // See https://slackhq.github.io/circuit/shared-elements/

                        ContentWithOverlays {
                            NavigableCircuitContent(
                                navigator = navigator,
                                backStack = backStack,
                                decoratorFactory =
                                    remember(navigator) {
                                        GestureNavigationDecorationFactory(onBackInvoked = navigator::pop)
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
