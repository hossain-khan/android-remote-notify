package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.WorkerStatus
import dev.hossain.remotenotify.model.toIconResId
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.utils.toTitleCase

@Composable
internal fun LastCheckStatusCardUi(
    lastCheckLog: AlertCheckLog?,
    workerStatus: WorkerStatus?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "Last Check Status",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (lastCheckLog == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.pending_actions_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "No checks have been performed yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(lastCheckLog.alertType.toIconResId()),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint =
                            if (lastCheckLog.isAlertSent) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        /*Text(
                            text = when (lastCheckLog.alertType) {
                                AlertType.BATTERY -> "Battery: ${lastCheckLog.alertStateValue}%"
                                AlertType.STORAGE -> "Storage: ${lastCheckLog.alertStateValue}GB"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )*/
                        Text(
                            text = "Alert checked ${formatTimeDuration(lastCheckLog.checkedOn)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (lastCheckLog.isAlertSent) {
                            Text(
                                text = "Alert sent",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            // Show worker status
            workerStatus?.let { status ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.schedule_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = buildString {
                            append("Worker State: ${status.state.toTitleCase()}")
                            status.nextRunTimeMs?.let {
                                append(", Next check: ${formatTimeDuration(it)}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Formats timestamp to human readable time in past or future.
 *
 * Examples:
 * - Past: "2 hours 30 minutes ago"
 * - Future: "in 2 hours 30 minutes"
 * - Now: "just now"
 * - Past: "5 days 2 hours ago"
 * - Future: "in 5 days 2 hours"
 */
private fun formatTimeDuration(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = timestamp - now
    val absoluteDiff = kotlin.math.abs(diff)

    // Function to handle pluralization
    fun pluralize(count: Long, unit: String) = "$count $unit${if (count > 1) "s" else ""}"

    // Build time components
    fun buildTimeString(
        primary: Pair<Long, String>,
        secondary: Pair<Long, String>? = null
    ): String = buildString {
        append(pluralize(primary.first, primary.second))
        secondary?.let { (value, unit) ->
            if (value > 0) {
                append(" ${pluralize(value, unit)}")
            }
        }
    }

    val timeString = when {
        absoluteDiff < 60_000 -> "just now"
        absoluteDiff < 3600_000 -> {
            val minutes = absoluteDiff / 60_000
            pluralize(minutes, "minute")
        }
        absoluteDiff < 86400_000 -> {
            val hours = absoluteDiff / 3600_000
            val minutes = (absoluteDiff % 3600_000) / 60_000
            buildTimeString(hours to "hour", minutes to "minute")
        }
        absoluteDiff < 2592000000 -> { // 30 days
            val days = absoluteDiff / 86400_000
            val hours = (absoluteDiff % 86400_000) / 3600_000
            buildTimeString(days to "day", hours to "hour")
        }
        else -> {
            val days = absoluteDiff / 86400_000
            pluralize(days, "day")
        }
    }

    return when {
        diff > 0 -> "in $timeString" // Future
        diff < 0 -> "$timeString ago" // Past
        else -> timeString // Now
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewLastCheckStatusCard() {
    ComposeAppTheme {
        Surface {
            Column {
                LastCheckStatusCardUi(
                    lastCheckLog =
                        AlertCheckLog(
                            checkedOn = System.currentTimeMillis(),
                            alertType = AlertType.BATTERY,
                            isAlertSent = false,
                        ),
                    workerStatus =
                        WorkerStatus(
                            state = "RUNNING",
                            nextRunTimeMs = System.currentTimeMillis() + 3990_000,
                            lastRunTimeMs = System.currentTimeMillis(),
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                LastCheckStatusCardUi(
                    lastCheckLog =
                        AlertCheckLog(
                            checkedOn = System.currentTimeMillis() - 10_00_000,
                            alertType = AlertType.STORAGE,
                            isAlertSent = false,
                        ),
                    workerStatus =
                        WorkerStatus(
                            state = "RUNNING",
                            nextRunTimeMs = System.currentTimeMillis() + 3600_000,
                            lastRunTimeMs = System.currentTimeMillis() - 60_00_000,
                        ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                LastCheckStatusCardUi(
                    lastCheckLog = null,
                    workerStatus = null,
                )
            }
        }
    }
}
