package dev.hossain.remotenotify.ui.alertchecklog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.theme.ComposeAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun FilterBottomSheetContent(
    currentState: AlertCheckLogViewerScreen.State,
    onFilterByAlertType: (AlertType?) -> Unit,
    onFilterByNotifierType: (NotifierType?) -> Unit,
    onToggleTriggeredOnly: () -> Unit,
    onSelectDateRange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Filter Logs",
                style = MaterialTheme.typography.titleLarge,
            )

            TextButton(onClick = onClearFilters) {
                Text("Clear All")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Alert Status",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = currentState.showTriggeredOnly,
                onCheckedChange = { onToggleTriggeredOnly() },
            )
            Text("Show Triggered Alerts Only")
        }

        Text(
            text = "Alert Type",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            FilterChip(
                selected = currentState.selectedAlertType == null,
                onClick = { onFilterByAlertType(null) },
                label = { Text("All") },
            )

            FilterChip(
                selected = currentState.selectedAlertType == AlertType.BATTERY,
                onClick = { onFilterByAlertType(AlertType.BATTERY) },
                leadingIcon = {
                    if (currentState.selectedAlertType == AlertType.BATTERY) {
                        Icon(
                            painter = painterResource(R.drawable.battery_5_bar_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
                label = { Text("Battery") },
            )

            FilterChip(
                selected = currentState.selectedAlertType == AlertType.STORAGE,
                onClick = { onFilterByAlertType(AlertType.STORAGE) },
                leadingIcon = {
                    if (currentState.selectedAlertType == AlertType.STORAGE) {
                        Icon(
                            painter = painterResource(R.drawable.hard_disk_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                },
                label = { Text("Storage") },
            )
        }

        Text(
            text = "Notification Method",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        NotifierTypeFilterChips(
            currentState = currentState,
            onFilterByNotifierType = onFilterByNotifierType,
        )

        Text(
            text = "Date Range",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Start date
            OutlinedTextField(
                value =
                    if (currentState.dateRange.first != null) {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(currentState.dateRange.first!!))
                    } else {
                        ""
                    },
                onValueChange = { },
                readOnly = true,
                label = { Text("Start Date") },
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                trailingIcon = {
                    IconButton(onClick = { onSelectDateRange(true) }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Start Date")
                    }
                },
            )

            // End date
            OutlinedTextField(
                value =
                    if (currentState.dateRange.second != null) {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(Date(currentState.dateRange.second!!))
                    } else {
                        ""
                    },
                onValueChange = { },
                readOnly = true,
                label = { Text("End Date") },
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    IconButton(onClick = { onSelectDateRange(false) }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select End Date")
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onClose) {
                Text("Apply Filters")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotifierTypeFilterChips(
    currentState: AlertCheckLogViewerScreen.State,
    onFilterByNotifierType: (NotifierType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
    ) {
        FilterChip(
            selected = currentState.selectedNotifierType == null,
            onClick = { onFilterByNotifierType(null) },
            label = { Text("All") },
        )

        // Create a chip for each notifier type
        NotifierType.entries.forEach { notifierType ->
            FilterChip(
                selected = currentState.selectedNotifierType == notifierType,
                onClick = { onFilterByNotifierType(notifierType) },
                label = { Text(notifierType.displayName) },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFilterBottomSheetContent_NoFilters() {
    ComposeAppTheme {
        Surface {
            FilterBottomSheetContent(
                currentState =
                    AlertCheckLogViewerScreen.State(
                        logs = emptyList(),
                        filteredLogs = emptyList(),
                        isLoading = false,
                        checkIntervalMinutes = 60,
                        eventSink = {},
                    ),
                onFilterByAlertType = {},
                onFilterByNotifierType = {},
                onToggleTriggeredOnly = {},
                onSelectDateRange = {},
                onClearFilters = {},
                onClose = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFilterBottomSheetContent_WithFilters() {
    ComposeAppTheme {
        Surface {
            FilterBottomSheetContent(
                currentState =
                    AlertCheckLogViewerScreen.State(
                        logs = emptyList(),
                        filteredLogs = emptyList(),
                        isLoading = false,
                        checkIntervalMinutes = 60,
                        showTriggeredOnly = true,
                        selectedAlertType = AlertType.BATTERY,
                        selectedNotifierType = NotifierType.EMAIL,
                        eventSink = {},
                    ),
                onFilterByAlertType = {},
                onFilterByNotifierType = {},
                onToggleTriggeredOnly = {},
                onSelectDateRange = {},
                onClearFilters = {},
                onClose = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFilterBottomSheetContent_WithDateRange() {
    ComposeAppTheme {
        Surface {
            FilterBottomSheetContent(
                currentState =
                    AlertCheckLogViewerScreen.State(
                        logs = emptyList(),
                        filteredLogs = emptyList(),
                        isLoading = false,
                        checkIntervalMinutes = 60,
                        dateRange =
                            Pair(
                                System.currentTimeMillis() - 86400000 * 7, // 7 days ago
                                System.currentTimeMillis(),
                            ),
                        eventSink = {},
                    ),
                onFilterByAlertType = {},
                onFilterByNotifierType = {},
                onToggleTriggeredOnly = {},
                onSelectDateRange = {},
                onClearFilters = {},
                onClose = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewFilterBottomSheetContent_AllFilters() {
    ComposeAppTheme {
        Surface {
            FilterBottomSheetContent(
                currentState =
                    AlertCheckLogViewerScreen.State(
                        logs = emptyList(),
                        filteredLogs = emptyList(),
                        isLoading = false,
                        checkIntervalMinutes = 60,
                        showTriggeredOnly = true,
                        selectedAlertType = AlertType.STORAGE,
                        selectedNotifierType = NotifierType.TELEGRAM,
                        dateRange =
                            Pair(
                                System.currentTimeMillis() - 86400000 * 30, // 30 days ago
                                System.currentTimeMillis() - 86400000 * 2, // 2 days ago
                            ),
                        eventSink = {},
                    ),
                onFilterByAlertType = {},
                onFilterByNotifierType = {},
                onToggleTriggeredOnly = {},
                onSelectDateRange = {},
                onClearFilters = {},
                onClose = {},
            )
        }
    }
}
