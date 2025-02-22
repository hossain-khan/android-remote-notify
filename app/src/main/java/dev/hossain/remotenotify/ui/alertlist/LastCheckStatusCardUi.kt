package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.BuildConfig
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.model.WorkerStatus
import dev.hossain.remotenotify.model.toIconResId
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.utils.formatTimeDuration
import dev.hossain.remotenotify.utils.toTitleCase

@Composable
internal fun LastCheckStatusCardUi(
    lastCheckLog: AlertCheckLog?,
    workerStatus: WorkerStatus?,
    modifier: Modifier = Modifier,
    onViewAllLogs: () -> Unit = {},
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
                        text =
                            buildString {
                                append("Worker State: ${status.state.toTitleCase()}")
                                status.nextRunTimeMs?.let {
                                    append(", Next check: ${formatTimeDuration(it)}")
                                }
                            },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (BuildConfig.DEBUG) {
                TextButton(
                    onClick = onViewAllLogs,
                    colors =
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                ) {
                    Text("View All Logs")
                }
            }
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
                            stateValue = 17,
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
                            stateValue = 12,
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
