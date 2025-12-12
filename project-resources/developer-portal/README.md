# Developer Portal

> **Debug-only feature** for comprehensive testing and validation of Android Remote Notify app functionality

## Overview

The Developer Portal is a specialized screen available only in debug builds that provides developers with tools to test and simulate app features without waiting for actual device conditions. It's designed to streamline development, debugging, and QA processes.

## Access

**Navigation Path:** About Screen → "Developer Portal" button (debug builds only)

The Developer Portal entry point is automatically hidden in release builds for security and user experience.

## Features

### 📱 Device Status Display

Real-time monitoring of current device conditions:
- **Battery Level**: Current battery percentage
- **Storage Available**: Free storage space in GB
- **Build Information**: Version name, version code, and build type

### 🎮 Device Simulation

Simulate device conditions to trigger notifications without waiting for real events:

#### Battery Alert Simulation
- **Interactive Slider**: Adjust simulated battery level (0-100%)
- **Current Level Display**: Shows selected percentage
- **Test Notification**: Triggers battery alert notifications to all configured channels
- **Result Feedback**: Success/failure count via Snackbar

#### Storage Alert Simulation
- **Interactive Slider**: Adjust simulated available storage (1GB to max)
- **Current Space Display**: Shows selected GB amount
- **Test Notification**: Triggers storage alert notifications to all configured channels
- **Result Feedback**: Success/failure count via Snackbar

**Use Cases:**
- Test notification delivery without draining battery
- Validate alert thresholds and notification content
- QA testing across all notification channels simultaneously
- Demo app capabilities without affecting device

### 📧 Notification Channel Testing

Individual testing controls for each notification channel:

#### Supported Channels
1. **Email** (Mailgun API)
2. **Telegram** (Bot API)
3. **Twilio SMS** (SMS API)
4. **Slack** (Workflow Webhook)
5. **Discord** (Webhook)
6. **REST Webhook** (Custom HTTP endpoint)

#### Features
- **Configuration Status**: Green checkmark (✓) if configured, gray if not
- **Individual Test Buttons**: Test each channel independently
- **Real-time Results**: Success (✓) or failure (✗) indicators per channel
- **Loading States**: Visual feedback during API calls
- **Error Handling**: Detailed error messages for troubleshooting

**Use Cases:**
- Verify notification channel configurations
- Test individual channel APIs without full simulation
- Troubleshoot specific channel failures
- Validate API credentials and endpoints

### 📊 Alert Configuration & Log Management

Comprehensive alert and log statistics with management tools:

#### Statistics Display
- **Configured Alerts**: Count of active alert configurations
- **Total Checks**: Number of health check executions
- **Triggered Alerts**: Count of alerts that were sent (color-coded red)

#### Management Actions
- **View All Logs**: Navigate to detailed alert check log list
- **Clear All Logs**: Remove all alert check history with confirmation dialog
- **Real-time Updates**: Statistics update automatically via Flow observation

**Use Cases:**
- Monitor alert system activity
- Audit alert triggers and notification history
- Clean up test data before QA runs
- Track notification success/failure rates

### 🔋 Battery Optimization Testing

Test battery optimization status and user education flow:

#### Status Display
- **Current Status**: Shows if battery optimization is enabled or disabled
- **Visual Indicators**: Green (✓ Disabled) or Red (Enabled) with warning text
- **Auto-refresh**: Status updates automatically after system settings changes

#### Testing Controls
1. **Show Reminder Sheet**: Force display the battery optimization education bottom sheet
2. **Reset Preference**: Clear "Don't remind again" flag to retest user flow
3. **Open Settings**: Direct link to system battery optimization settings (with auto-refresh on return)

**Use Cases:**
- Test battery optimization reminder flow
- Verify education content and UI
- Debug battery optimization detection
- QA user onboarding experience

### ⚙️ WorkManager Testing

Monitor and control background health check jobs:

#### Work Status Display
- **One-Time Work**: Shows status of debug test work requests
  - States: Not scheduled, ⏳ Enqueued, 🔄 Running, ✅ Succeeded, ❌ Failed, 🚫 Blocked, 🛑 Cancelled
- **Periodic Work**: Shows status of scheduled periodic health checks
  - States: Not scheduled, ⏳ Scheduled, 🔄 Running

#### Testing Controls
- **Trigger One-Time Check**: Immediately execute a health check without waiting for schedule
- **Real-time Monitoring**: Status updates via WorkManager Flow observation
- **Result Tracking**: Check Alert Logs for execution results

**Use Cases:**
- Test WorkManager job execution immediately
- Verify background work constraints and conditions
- Debug health check logic without waiting
- Validate periodic scheduling configuration

## Architecture

### Technology Stack
- **Framework**: [Circuit](https://slackhq.github.io/circuit/) - Compose-driven architecture
- **DI**: [Metro](https://zacsweers.github.io/metro/) - Kotlin-first dependency injection
- **UI**: Jetpack Compose with Material3
- **State Management**: StateFlow, produceState, LaunchedEffect
- **Background Work**: WorkManager with Flow observation

### File Structure
```
app/src/main/java/dev/hossain/remotenotify/ui/devportal/
└── DeveloperPortalScreen.kt  (Single file with ~1300 lines)
    ├── DeveloperPortalScreen (data object)
    │   ├── State (data class)
    │   └── Event (sealed class)
    ├── DeveloperPortalPresenter (class)
    └── DeveloperPortalUi (@Composable)
        ├── DeviceSimulationCard
        ├── NotificationChannelTestingCard
        ├── AlertLogManagementCard
        ├── BatteryOptimizationTestingCard
        ├── WorkManagerTestingCard
        └── Dialogs (Clear Logs, Battery Opt Sheet)
```

### State Management Pattern

#### State Definition
```kotlin
data class State(
    val currentBatteryLevel: Int,
    val currentStorageGb: Double,
    val maxStorageGb: Int,
    val buildVersion: String,
    val isSimulating: Boolean,
    val simulationResult: String?,
    val testingChannel: NotifierType?,
    val channelTestResults: Map<NotifierType, Boolean?>,
    val configuredChannels: Set<NotifierType>,
    val configuredAlertsCount: Int,
    val totalChecks: Int,
    val triggeredAlerts: Int,
    val showClearLogsDialog: Boolean,
    val isBatteryOptimizationEnabled: Boolean,
    val showBatteryOptSheet: Boolean,
    val oneTimeWorkState: String?,
    val periodicWorkState: String?,
    val eventSink: (Event) -> Unit,
) : CircuitUiState
```

#### Event Handling
```kotlin
sealed class Event : CircuitUiEvent {
    data object GoBack : Event()
    data class SimulateBatteryAlert(val simulatedLevel: Int) : Event()
    data class SimulateStorageAlert(val simulatedStorageGb: Int) : Event()
    data class TestNotificationChannel(val notifierType: NotifierType) : Event()
    data object NavigateToAlertsList : Event()
    data object ShowClearLogsDialog : Event()
    data object DismissClearLogsDialog : Event()
    data object ConfirmClearLogs : Event()
    data object ShowBatteryOptSheet : Event()
    data object DismissBatteryOptSheet : Event()
    data object ResetBatteryOptPreference : Event()
    data object OpenBatterySettings : Event()
    data object TriggerOneTimeWork : Event()
}
```

### Dependency Injection

The presenter uses Metro's assisted injection:

```kotlin
@AssistedInject
class DeveloperPortalPresenter constructor(
    @Assisted private val navigator: Navigator,
    private val analytics: Analytics,
    private val batteryMonitor: BatteryMonitor,
    private val storageMonitor: StorageMonitor,
    private val repository: RemoteAlertRepository,
    private val notifiers: Set<@JvmSuppressWildcards NotificationSender>,
    private val appPreferencesDataStore: AppPreferencesDataStore,
) : Presenter<DeveloperPortalScreen.State>
```

All dependencies are automatically provided by Metro's dependency graph.

## Testing Strategy

### Unit Testing Approach

**Test File**: `DeveloperPortalScreenTest.kt` (to be created)

**Test Coverage Areas:**
1. **State Management**
   - Initial state values
   - State updates on events
   - Async state updates (Flow/LaunchedEffect)

2. **Event Handling**
   - Navigation events
   - Simulation events with different parameters
   - Channel testing events
   - Dialog show/hide events
   - Battery optimization events
   - WorkManager trigger events

3. **Business Logic**
   - Battery simulation logic
   - Storage simulation logic
   - Notification sending success/failure handling
   - Log clearing confirmation flow

4. **Integration Points**
   - Repository interactions
   - NotificationSender invocations
   - WorkManager triggering
   - Analytics event logging

### UI Testing Approach

**Compose UI Tests**: Verify UI rendering and interactions

**Test Coverage Areas:**
1. **Screen Rendering**
   - All cards display correctly
   - Status information shows current values
   - Buttons are enabled/disabled appropriately

2. **User Interactions**
   - Slider adjustments update state
   - Button clicks trigger correct events
   - Dialog interactions work correctly

3. **Accessibility**
   - Content descriptions present
   - Touch targets meet minimum size
   - Screen reader compatibility

## Development Guidelines

### Adding New Features

1. **Define Event**: Add new event type to `Event` sealed class
2. **Update State**: Add new state fields to `State` data class
3. **Implement Handler**: Add event handler logic in presenter's `present()` function
4. **Update UI**: Create or modify composables to display new feature
5. **Wire Up**: Connect UI event triggers to `state.eventSink()`

### Code Style

- Follow Kotlin coding conventions
- Use descriptive variable and function names
- Keep composables focused and single-responsibility
- Extract complex UI logic into separate composables
- Add comments for complex business logic

### Performance Considerations

- Use `remember` for expensive calculations
- Prefer `LaunchedEffect` over `DisposableEffect` when possible
- Use `produceState` for Flow collection in Composables
- Avoid creating objects in composition
- Use proper Flow cancellation with coroutine scopes

## Common Issues & Solutions

### Issue: Notifications Not Sending
**Solution**: Check notification channel configuration in app settings. Use individual channel testing to identify specific failures.

### Issue: WorkManager Status Not Updating
**Solution**: Ensure WorkManager is properly initialized. Check device battery optimization settings (may block background work).

### Issue: Battery Optimization Sheet Not Showing
**Solution**: Verify the preference hasn't been set to "don't show again". Use Reset Preference button to clear.

### Issue: Alert Logs Not Appearing
**Solution**: Trigger at least one work request (one-time or periodic). Logs are created only after health checks execute.

## Analytics Integration

The Developer Portal logs the following analytics events:

- **Screen Views**: Every time a developer interacts with a feature
- **Test Actions**: Simulation triggers, channel tests, work triggers
- **Configuration Changes**: Battery optimization preference resets

Analytics events help track developer tool usage and identify popular testing scenarios.

## Security Considerations

### Debug-Only Access

The Developer Portal is:
- ✅ Only compiled into debug builds (`BuildConfig.DEBUG` check)
- ✅ Hidden from release builds automatically
- ✅ Not accessible in production environments
- ✅ Removes all traces in ProGuard/R8 optimization

### Data Privacy

- No production user data is accessible
- Test notifications use configured channels only
- Log data is stored locally in Room database
- No sensitive credentials are displayed in UI

## Future Enhancements

Potential features for future versions:

1. **Advanced WorkManager Controls**
   - Cancel scheduled work
   - Modify interval constraints
   - View work execution history
   - Export work logs

2. **Network Simulation**
   - Simulate network failures
   - Test retry logic
   - Monitor API call metrics

3. **Notification Preview**
   - Preview notification content before sending
   - Test notification appearance across devices
   - Validate notification channels

4. **Export Functionality**
   - Export alert logs as CSV/JSON
   - Share test results with team
   - Generate test reports

5. **Remote Control**
   - Trigger tests from web dashboard
   - Monitor multiple test devices
   - Centralized test result collection

## Contributing

When contributing to the Developer Portal:

1. Follow the existing architecture patterns (Circuit + Metro)
2. Add comprehensive tests for new features
3. Update this README with new feature documentation
4. Ensure all code passes linting and formatting checks
5. Test on multiple device configurations
6. Consider accessibility in all UI changes

## Resources

- [Circuit Documentation](https://slackhq.github.io/circuit/)
- [Metro Documentation](https://zacsweers.github.io/metro/)
- [Jetpack Compose Guidelines](https://developer.android.com/jetpack/compose)
- [WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Material3 Design System](https://m3.material.io/)

---

**Developer Portal** - Making Android development faster, easier, and more reliable.
