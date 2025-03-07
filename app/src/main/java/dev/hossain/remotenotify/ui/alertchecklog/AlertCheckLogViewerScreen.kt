package dev.hossain.remotenotify.ui.alertchecklog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
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
import dev.hossain.remotenotify.data.AppPreferencesDataStore
import dev.hossain.remotenotify.data.RemoteAlertRepository
import dev.hossain.remotenotify.di.AppScope
import dev.hossain.remotenotify.model.AlertCheckLog
import dev.hossain.remotenotify.model.AlertType
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.utils.formatDuration
import dev.hossain.remotenotify.utils.formatTimeDuration
import dev.hossain.remotenotify.utils.toTitleCase
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Screen to view all the alert check logs with filtering capabilities.
 */
@Parcelize
data object AlertCheckLogViewerScreen : Screen {
    data class State(
        val logs: List<AlertCheckLog>,
        val filteredLogs: List<AlertCheckLog>,
        val isLoading: Boolean,
        val checkIntervalMinutes: Long,
        val showTriggeredOnly: Boolean = false,
        val selectedAlertType: AlertType? = null,
        val selectedNotifierType: NotifierType? = null,
        val dateRange: Pair<Long?, Long?> = Pair(null, null),
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object NavigateBack : Event()

        data object ToggleTriggeredOnly : Event()

        data class FilterByAlertType(
            val alertType: AlertType?,
        ) : Event()

        data class FilterByNotifierType(
            val notifierType: NotifierType?,
        ) : Event()

        data class FilterByDateRange(
            val startDate: Long?,
            val endDate: Long?,
        ) : Event()

        data object ClearFilters : Event()

        data class ExportLogs(
            val logs: List<AlertCheckLog>,
        ) : Event()
    }
}

class AlertCheckLogViewerPresenter
    @AssistedInject
    constructor(
        @Assisted private val navigator: Navigator,
        private val appPreferencesDataStore: AppPreferencesDataStore,
        private val remoteAlertRepository: RemoteAlertRepository,
    ) : Presenter<AlertCheckLogViewerScreen.State> {
        @Composable
        override fun present(): AlertCheckLogViewerScreen.State {
            var isLoading by remember { mutableStateOf(true) }
            var showTriggeredOnly by rememberSaveable { mutableStateOf(false) }
            var selectedAlertType by rememberSaveable { mutableStateOf<AlertType?>(null) }
            var selectedNotifierType by rememberSaveable { mutableStateOf<NotifierType?>(null) }
            var startDate by rememberSaveable { mutableStateOf<Long?>(null) }
            var endDate by rememberSaveable { mutableStateOf<Long?>(null) }

            val checkIntervalMinutes by produceState(0L) {
                appPreferencesDataStore.workerIntervalFlow.collect {
                    value = it
                }
            }

            val allLogs by produceState<List<AlertCheckLog>>(emptyList()) {
                remoteAlertRepository
                    .getAllAlertCheckLogs()
                    .collect {
                        value = it
                        isLoading = false
                    }
            }

            // Apply all filters
            val filteredLogs =
                allLogs.filter { log ->
                    val startDate = startDate
                    val endDate = endDate
                    val matchesTriggeredFilter = !showTriggeredOnly || log.isAlertSent
                    val matchesAlertTypeFilter = selectedAlertType == null || log.alertType == selectedAlertType
                    val matchesNotifierTypeFilter = selectedNotifierType == null || log.notifierType == selectedNotifierType
                    val matchesDateRange =
                        (startDate == null || log.checkedOn >= startDate) &&
                            (endDate == null || log.checkedOn <= endDate)

                    matchesTriggeredFilter &&
                        matchesAlertTypeFilter &&
                        matchesNotifierTypeFilter &&
                        matchesDateRange
                }

            return AlertCheckLogViewerScreen.State(
                logs = allLogs,
                filteredLogs = filteredLogs,
                isLoading = isLoading,
                checkIntervalMinutes = checkIntervalMinutes,
                showTriggeredOnly = showTriggeredOnly,
                selectedAlertType = selectedAlertType,
                selectedNotifierType = selectedNotifierType,
                dateRange = Pair(startDate, endDate),
                eventSink = { event ->
                    when (event) {
                        AlertCheckLogViewerScreen.Event.NavigateBack -> navigator.pop()
                        AlertCheckLogViewerScreen.Event.ToggleTriggeredOnly -> {
                            showTriggeredOnly = !showTriggeredOnly
                        }
                        is AlertCheckLogViewerScreen.Event.FilterByAlertType -> {
                            selectedAlertType = event.alertType
                        }
                        is AlertCheckLogViewerScreen.Event.FilterByNotifierType -> {
                            selectedNotifierType = event.notifierType
                        }
                        is AlertCheckLogViewerScreen.Event.FilterByDateRange -> {
                            startDate = event.startDate
                            endDate = event.endDate
                        }
                        AlertCheckLogViewerScreen.Event.ClearFilters -> {
                            showTriggeredOnly = false
                            selectedAlertType = null
                            selectedNotifierType = null
                            startDate = null
                            endDate = null
                        }
                        is AlertCheckLogViewerScreen.Event.ExportLogs -> {
                            // Export functionality would be implemented here
                        }
                    }
                },
            )
        }

        @CircuitInject(AlertCheckLogViewerScreen::class, AppScope::class)
        @AssistedFactory
        fun interface Factory {
            fun create(navigator: Navigator): AlertCheckLogViewerPresenter
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(AlertCheckLogViewerScreen::class, AppScope::class)
@Composable
fun AlertCheckLogViewerUi(
    state: AlertCheckLogViewerScreen.State,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var datePickerMode by remember { mutableIntStateOf(0) } // 0 for start date, 1 for end date

    var expandedLogId by remember { mutableLongStateOf(-1L) }

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val hasActiveFilters =
        state.showTriggeredOnly ||
            state.selectedAlertType != null ||
            state.selectedNotifierType != null ||
            state.dateRange.first != null ||
            state.dateRange.second != null

    val datePickerState = rememberDatePickerState()

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Logs History")
                        if (state.filteredLogs.size != state.logs.size) {
                            Text(
                                "Showing ${state.filteredLogs.size} of ${state.logs.size}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        state.eventSink(AlertCheckLogViewerScreen.Event.NavigateBack)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                        )
                    }
                },
                actions = {
                    // Filter button with badge if filters are active
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (hasActiveFilters) {
                                    Badge { }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.filter_alt_24dp),
                                contentDescription = "Filter",
                                tint =
                                    if (hasActiveFilters) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    }

                    // Options menu
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                            )
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            offset = DpOffset(x = (-16).dp, y = 0.dp),
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export Logs") },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    state.eventSink(AlertCheckLogViewerScreen.Event.ExportLogs(state.filteredLogs))
                                    coroutineScope.launch {
                                        val result =
                                            snackbarHostState.showSnackbar(
                                                message = "Export feature coming soon!",
                                                actionLabel = "OK",
                                            )
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Clear All Filters") },
                                leadingIcon = {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                },
                                enabled = hasActiveFilters,
                                onClick = {
                                    showOptionsMenu = false
                                    state.eventSink(AlertCheckLogViewerScreen.Event.ClearFilters)
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (state.isLoading) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Loading logs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (state.logs.isEmpty()) {
                EmptyLogsState()
            } else if (state.filteredLogs.isEmpty()) {
                NoMatchingLogsState(
                    onClearFilters = { state.eventSink(AlertCheckLogViewerScreen.Event.ClearFilters) },
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp),
                ) {
                    item(key = "logs_summary") {
                        LogsSummaryInfo(
                            totalLogs = state.logs.size,
                            filteredLogs = state.filteredLogs.size,
                            checkIntervalMinutes = state.checkIntervalMinutes,
                            hasActiveFilters = hasActiveFilters,
                            onClearFilters = {
                                if (hasActiveFilters) {
                                    state.eventSink(AlertCheckLogViewerScreen.Event.ClearFilters)
                                }
                            },
                        )
                    }

                    // Active filters display
                    if (hasActiveFilters) {
                        item(key = "active_filters") {
                            ActiveFiltersSection(
                                showTriggeredOnly = state.showTriggeredOnly,
                                alertType = state.selectedAlertType,
                                notifierType = state.selectedNotifierType,
                                dateRange = state.dateRange,
                                onClearFilter = { filter ->
                                    when (filter) {
                                        "triggered" -> state.eventSink(AlertCheckLogViewerScreen.Event.ToggleTriggeredOnly)
                                        "alertType" -> state.eventSink(AlertCheckLogViewerScreen.Event.FilterByAlertType(null))
                                        "notifierType" -> state.eventSink(AlertCheckLogViewerScreen.Event.FilterByNotifierType(null))
                                        "dateRange" -> state.eventSink(AlertCheckLogViewerScreen.Event.FilterByDateRange(null, null))
                                    }
                                },
                            )
                        }
                    }

                    items(
                        count = state.filteredLogs.size,
                        key = { index -> state.filteredLogs[index].checkedOn },
                    ) { index ->
                        val log = state.filteredLogs[index]
                        val isExpanded = expandedLogId == log.checkedOn

                        LogItemCard(
                            log = log,
                            isExpanded = isExpanded,
                            onClick = {
                                expandedLogId = if (isExpanded) -1L else log.checkedOn
                            },
                            onCopyDetails = {
                                val logDetails =
                                    buildString {
                                        append("Type: ${log.alertType.name}\n")
                                        append("Time: ${formatDateTime(log.checkedOn)}\n")
                                        append("Status: ${if (log.isAlertSent) "Alert Sent" else "No Alert"}\n")

                                        if (log.alertType == AlertType.BATTERY) {
                                            append("Battery Level: ${log.stateValue}% (Threshold: ${log.configBatteryPercentage}%)\n")
                                        } else {
                                            append("Free Storage: ${log.stateValue}GB (Threshold: ${log.configStorageMinSpaceGb}GB)\n")
                                        }

                                        if (log.isAlertSent && log.notifierType != null) {
                                            append("Notification sent via: ${log.notifierType.displayName}\n")
                                        }
                                    }
                                clipboardManager.setText(AnnotatedString(logDetails))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Log details copied to clipboard")
                                }
                            },
                        )
                    }
                }
            }

            // Show date picker dialog for date range filtering
            if (showDatePickerDialog) {
                val confirmEnabled = datePickerState.selectedDateMillis != null
                val dialogTitle = if (datePickerMode == 0) "Select Start Date" else "Select End Date"

                DatePickerDialog(
                    onDismissRequest = { showDatePickerDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (datePickerMode == 0) {
                                    // Start date
                                    val startDate = datePickerState.selectedDateMillis
                                    state.eventSink(
                                        AlertCheckLogViewerScreen.Event.FilterByDateRange(
                                            startDate,
                                            state.dateRange.second,
                                        ),
                                    )
                                } else {
                                    // End date - set to end of day
                                    var endDate = datePickerState.selectedDateMillis
                                    if (endDate != null) {
                                        val calendar = Calendar.getInstance()
                                        calendar.timeInMillis = endDate
                                        calendar.set(Calendar.HOUR_OF_DAY, 23)
                                        calendar.set(Calendar.MINUTE, 59)
                                        calendar.set(Calendar.SECOND, 59)
                                        endDate = calendar.timeInMillis
                                    }
                                    state.eventSink(
                                        AlertCheckLogViewerScreen.Event.FilterByDateRange(
                                            state.dateRange.first,
                                            endDate,
                                        ),
                                    )
                                }
                                showDatePickerDialog = false
                            },
                            enabled = confirmEnabled,
                        ) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerDialog = false }) {
                            Text("Cancel")
                        }
                    },
                    tonalElevation = 4.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = dialogTitle,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                        DatePicker(
                            state = datePickerState,
                            showModeToggle = false,
                        )
                    }
                }
            }

            // Filter bottom sheet
            if (showFilterSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showFilterSheet = false },
                    sheetState = filterSheetState,
                ) {
                    FilterBottomSheetContent(
                        currentState = state,
                        onFilterByAlertType = { alertType ->
                            state.eventSink(AlertCheckLogViewerScreen.Event.FilterByAlertType(alertType))
                        },
                        onFilterByNotifierType = { notifierType ->
                            state.eventSink(AlertCheckLogViewerScreen.Event.FilterByNotifierType(notifierType))
                        },
                        onToggleTriggeredOnly = {
                            state.eventSink(AlertCheckLogViewerScreen.Event.ToggleTriggeredOnly)
                        },
                        onSelectDateRange = { isStartDate ->
                            datePickerMode = if (isStartDate) 0 else 1
                            showDatePickerDialog = true
                        },
                        onClearFilters = {
                            state.eventSink(AlertCheckLogViewerScreen.Event.ClearFilters)
                            showFilterSheet = false
                        },
                        onClose = {
                            showFilterSheet = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterBottomSheetContent(
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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier =
                Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
        ) {
            FilterChip(
                selected = currentState.selectedNotifierType == null,
                onClick = { onFilterByNotifierType(null) },
                label = { Text("All") },
            )

            // Create a chip for each notifier type
            NotifierType.values().forEach { notifierType ->
                FilterChip(
                    selected = currentState.selectedNotifierType == notifierType,
                    onClick = { onFilterByNotifierType(notifierType) },
                    label = { Text(notifierType.displayName) },
                )
            }
        }

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

@Composable
private fun ActiveFiltersSection(
    showTriggeredOnly: Boolean,
    alertType: AlertType?,
    notifierType: NotifierType?,
    dateRange: Pair<Long?, Long?>,
    onClearFilter: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
    ) {
        Text(
            text = "Active Filters:",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            if (showTriggeredOnly) {
                FilterChip(
                    selected = true,
                    onClick = { onClearFilter("triggered") },
                    label = { Text("Triggered Only") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Filter",
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            if (alertType != null) {
                FilterChip(
                    selected = true,
                    onClick = { onClearFilter("alertType") },
                    label = {
                        Text(
                            when (alertType) {
                                AlertType.BATTERY -> "Battery"
                                AlertType.STORAGE -> "Storage"
                            },
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Filter",
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            if (notifierType != null) {
                FilterChip(
                    selected = true,
                    onClick = { onClearFilter("notifierType") },
                    label = { Text(notifierType.displayName) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Filter",
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }

            if (dateRange.first != null || dateRange.second != null) {
                FilterChip(
                    selected = true,
                    onClick = { onClearFilter("dateRange") },
                    label = {
                        val dateText =
                            when {
                                dateRange.first != null && dateRange.second != null -> "Date Range"
                                dateRange.first != null ->
                                    "From ${
                                        SimpleDateFormat("MMM dd", Locale.getDefault())
                                            .format(Date(dateRange.first!!))
                                    }"
                                else ->
                                    "Until ${
                                        SimpleDateFormat("MMM dd", Locale.getDefault())
                                            .format(Date(dateRange.second!!))
                                    }"
                            }
                        Text(dateText)
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Filter",
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun LogItemCard(
    log: AlertCheckLog,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onCopyDetails: () -> Unit,
) {
    val cardColor by animateColorAsState(
        if (log.isAlertSent) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "cardColor",
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    when (log.alertType) {
                        AlertType.BATTERY -> "Battery Check"
                        AlertType.STORAGE -> "Storage Check"
                    },
                )
            },
            supportingContent = {
                Column {
                    // Show configured threshold and actual value
                    Text(
                        text =
                            when (log.alertType) {
                                AlertType.BATTERY ->
                                    buildString {
                                        append("${log.stateValue}% battery")
                                        append(" (Alert threshold: ${log.configBatteryPercentage}%)")
                                    }
                                AlertType.STORAGE ->
                                    buildString {
                                        append("${log.stateValue} GB storage")
                                        append(" (Alert threshold: ${log.configStorageMinSpaceGb} GB)")
                                    }
                            },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Checked ${formatTimeDuration(log.checkedOn)}",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text =
                                if (log.isAlertSent) {
                                    "Alert triggered & sent via ${log.notifierType?.name?.toTitleCase() ?: "N/A"}"
                                } else {
                                    "Alert not triggered"
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (log.isAlertSent) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                            )

                            Text(
                                text = "Details:",
                                style = MaterialTheme.typography.labelMedium,
                            )

                            Text(
                                text = "Full date: ${formatDateTime(log.checkedOn)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )

                            Text(
                                text = "Config ID: ${log.configId}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp),
                            )

                            Text(
                                text = "Config created: ${formatDateTime(log.configCreatedOn)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp),
                            )

                            TextButton(
                                onClick = onCopyDetails,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text("Copy Details")
                            }
                        }
                    }
                }
            },
            leadingContent = {
                Icon(
                    painter =
                        painterResource(
                            when (log.alertType) {
                                AlertType.BATTERY -> R.drawable.battery_5_bar_24dp
                                AlertType.STORAGE -> R.drawable.hard_disk_24dp
                            },
                        ),
                    contentDescription = null,
                    tint =
                        if (log.isAlertSent) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            },
        )
    }
}

@Composable
private fun LogsSummaryInfo(
    totalLogs: Int,
    filteredLogs: Int,
    checkIntervalMinutes: Long,
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            if (checkIntervalMinutes > 0L) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.schedule_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Alerts checked every ${formatDuration(checkIntervalMinutes.toInt())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            if (totalLogs > 0) {
                if (checkIntervalMinutes > 0L) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.format_list_bulleted_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (filteredLogs < totalLogs) {
                            Text(
                                text = "Showing $filteredLogs of $totalLogs logs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            Text(
                                text = "Total $totalLogs logs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    if (hasActiveFilters) {
                        TextButton(
                            onClick = onClearFilters,
                            modifier = Modifier.padding(start = 8.dp),
                        ) {
                            Text("Clear Filters")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoMatchingLogsState(onClearFilters: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.filter_alt_24dp),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Matching Logs",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No logs match your current filters. Try adjusting your filter criteria to see more results.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onClearFilters) {
            Text("Clear All Filters")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewAlertCheckLogViewerUi() {
    val sampleLogs =
        listOf(
            AlertCheckLog(
                checkedOn = System.currentTimeMillis() - 3600000, // 1 hour ago
                alertType = AlertType.BATTERY,
                isAlertSent = true,
                notifierType = NotifierType.EMAIL,
                stateValue = 15,
                configId = 1,
                configBatteryPercentage = 20,
                configStorageMinSpaceGb = 0,
                configCreatedOn = System.currentTimeMillis() - 86400000 * 7, // 7 days ago
            ),
            AlertCheckLog(
                checkedOn = System.currentTimeMillis() - 7200000, // 2 hours ago
                alertType = AlertType.STORAGE,
                isAlertSent = false,
                notifierType = null,
                stateValue = 20,
                configId = 2,
                configBatteryPercentage = 0,
                configStorageMinSpaceGb = 25,
                configCreatedOn = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
            ),
            AlertCheckLog(
                checkedOn = System.currentTimeMillis() - 86400000, // 1 day ago
                alertType = AlertType.BATTERY,
                isAlertSent = false,
                notifierType = null,
                stateValue = 80,
                configId = 1,
                configBatteryPercentage = 20,
                configStorageMinSpaceGb = 0,
                configCreatedOn = System.currentTimeMillis() - 86400000 * 7, // 7 days ago
            ),
            AlertCheckLog(
                checkedOn = System.currentTimeMillis() - 9250000,
                alertType = AlertType.STORAGE,
                isAlertSent = true,
                notifierType = NotifierType.TELEGRAM,
                stateValue = 2,
                configId = 2,
                configBatteryPercentage = 0,
                configStorageMinSpaceGb = 25,
                configCreatedOn = System.currentTimeMillis() - 86400000 * 5, // 5 days ago
            ),
        )

    ComposeAppTheme {
        AlertCheckLogViewerUi(
            state =
                AlertCheckLogViewerScreen.State(
                    logs = sampleLogs,
                    filteredLogs = sampleLogs,
                    isLoading = false,
                    checkIntervalMinutes = 60,
                    eventSink = {},
                ),
        )
    }
}

// Make sure we have a formatDateTime utility function
private fun formatDateTime(timestamp: Long): String = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
