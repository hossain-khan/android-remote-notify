package dev.hossain.remotenotify.ui.alertmediumlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.utils.formatDuration

/**
 * Internal composable function that displays a card for configuring the worker check interval.
 *
 * This card allows the user to adjust a slider to set the frequency at which the background worker
 * checks for new alerts. The selected interval is displayed in a human-readable format (e.g., "1h 30m").
 * When the slider value changes, it triggers an event to update the worker interval in the application's state.
 *
 * @param state The current [NotificationMediumListScreen.State] which contains the `workerIntervalMinutes`
 *              and the `eventSink` for dispatching update events.
 * @param modifier Optional [Modifier] to be applied to the card.
 */
@Composable
internal fun WorkerConfigCard(
    state: NotificationMediumListScreen.State,
    modifier: Modifier = Modifier,
) {
    // Update intervalSliderValue whenever state.workerIntervalMinutes changes
    var intervalSliderValue by remember(state.workerIntervalMinutes) {
        mutableFloatStateOf(state.workerIntervalMinutes.toFloat())
    }

    val alertCheckIntervalRangeStart = 60f // 1 hour
    val alertCheckIntervalRangeEnd = 720f // 12 hours

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Check Frequency",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alert checked every ${formatDuration(intervalSliderValue.toInt())}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = intervalSliderValue,
                onValueChange = { intervalValue: Float ->
                    intervalSliderValue = intervalValue // Update UI immediately
                    state.eventSink(NotificationMediumListScreen.Event.OnWorkerIntervalUpdated(intervalValue.toLong()))
                },
                valueRange = alertCheckIntervalRangeStart..alertCheckIntervalRangeEnd,
                modifier = Modifier.fillMaxWidth(),
                colors =
                    SliderDefaults.colors(
                        // Increase contrast for the inactive track
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        // Make active part more prominent
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        thumbColor = MaterialTheme.colorScheme.primary,
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${(alertCheckIntervalRangeStart / 60).toInt()}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${(alertCheckIntervalRangeEnd / 60).toInt()}h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@PreviewLightDark
@PreviewDynamicColors
@Composable
private fun PreviewWorkerConfigCard() {
    ComposeAppTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            WorkerConfigCard(
                state =
                    NotificationMediumListScreen.State(
                        workerIntervalMinutes = 68,
                        notifiers = emptyList(),
                        eventSink = {},
                    ),
            )
        }
    }
}

@PreviewLightDark
@PreviewDynamicColors
@Composable
private fun PreviewWorkerConfigCardWithLongDuration() {
    ComposeAppTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            WorkerConfigCard(
                state =
                    NotificationMediumListScreen.State(
                        workerIntervalMinutes = 224,
                        notifiers = emptyList(),
                        eventSink = {},
                    ),
            )
        }
    }
}
