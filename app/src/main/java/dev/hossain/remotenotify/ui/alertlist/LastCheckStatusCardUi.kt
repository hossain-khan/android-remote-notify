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
                        painter = painterResource(R.drawable.schedule_24dp),
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
                            text = "Alert checked ${formatTimeAgo(lastCheckLog.checkedOn)}",
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
                        text = "Worker State: ${status.state}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    status.nextRunTimeMs?.let {
                        Text(
                            text = ", Next check: ${formatTimeAgo(it)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> {
            val minutes = diff / 60_000
            "$minutes minute${if (minutes > 1) "s" else ""} ago"
        }
        diff < 86400_000 -> {
            val hours = diff / 3600_000
            val minutes = (diff % 3600_000) / 60_000
            buildString {
                append("$hours hour${if (hours > 1) "s" else ""}")
                if (minutes > 0) {
                    append(" $minutes minute${if (minutes > 1) "s" else ""}")
                }
                append(" ago")
            }
        }
        diff < 2592000000 -> { // 30 days
            val days = diff / 86400_000
            val hours = (diff % 86400_000) / 3600_000
            buildString {
                append("$days day${if (days > 1) "s" else ""}")
                if (hours > 0) {
                    append(" $hours hour${if (hours > 1) "s" else ""}")
                }
                append(" ago")
            }
        }
        else -> {
            val days = diff / 86400_000
            "$days days ago"
        }
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
                            nextRunTimeMs = System.currentTimeMillis() + 3600_000,
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
