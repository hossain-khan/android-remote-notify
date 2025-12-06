# UI/Presenter Tests

This directory contains comprehensive unit tests for UI/Presenter layer using Circuit testing utilities.

## Test Coverage

### Implemented Tests

1. **AlertsListPresenterTest** (10 test cases)
   - State initialization and device info
   - Delete notification event
   - Edit remote alert navigation
   - Add notification navigation
   - Add notification destination navigation
   - Education sheet show/dismiss events
   - View all logs navigation
   - Repository alerts loading
   - Notifier configuration state

2. **AddNewRemoteAlertPresenterTest** (12 test cases)
   - State initialization for new alert
   - State initialization for edit mode
   - Save notification for new alert
   - Save notification for edit mode
   - Navigate back event
   - Update alert type event
   - Update threshold event
   - Battery optimization sheet events
   - Storage alert editing

3. **NotificationMediumListPresenterTest** (9 test cases)
   - State initialization with sorted notifiers
   - Notifier configuration status
   - Edit medium config navigation
   - Reset medium config event
   - Worker interval update event
   - Navigate back event
   - Multiple notifiers display

4. **AlertCheckLogViewerPresenterTest** (12 test cases)
   - State initialization
   - Toggle triggered only filter
   - Filter by alert type
   - Filter by notifier type
   - Filter by date range
   - Clear filters event
   - Multiple filters combination
   - Export logs event
   - Empty state handling

5. **ConfigureNotificationMediumPresenterTest** (2 test cases)
   - State initialization
   - isConfigured state validation

## Running Tests

### Prerequisites

- JDK 17 or higher
- Android SDK installed
- `local.properties` file with required API keys

### Create `local.properties`

```properties
EMAIL_API_KEY=test_api_key_for_unit_tests
```

### Run All UI Tests

```bash
./gradlew testDebugUnitTest --tests "dev.hossain.remotenotify.ui.*"
```

### Run Specific Test Class

```bash
./gradlew testDebugUnitTest --tests "dev.hossain.remotenotify.ui.alertlist.AlertsListPresenterTest"
```

### Run Single Test

```bash
./gradlew testDebugUnitTest --tests "dev.hossain.remotenotify.ui.alertlist.AlertsListPresenterTest.when presenter is initialized then state contains device info"
```

## Known Issues

### LocalContext Requirement

The current tests encounter `CompositionLocal LocalContext not present` errors because the presenters use `LocalContext.current` to access Android context. This requires additional setup:

#### Solution Options

1. **Use Robolectric Application Context** (Recommended)
   - Provide a proper Android context through Robolectric
   - May require wrapping presenter.test() with Compose test environment

2. **Mock Context Dependencies**
   - Refactor presenters to inject Context as a dependency instead of using LocalContext
   - This would be a larger refactor of the presenter architecture

3. **Use Circuit Test Utilities**
   - Circuit may provide test helpers for this scenario
   - Check Circuit documentation for testing best practices

### Current Status

- ✅ All test files compile successfully
- ✅ Test structure follows existing patterns
- ✅ Uses MockK for mocking
- ✅ Uses Truth for assertions
- ⚠️ Tests fail at runtime due to missing Compose context
- 🔄 Requires additional setup to provide LocalContext in tests

## Test Structure

All tests follow this pattern:

```kotlin
@RunWith(RobolectricTestRunner::class)
class SomePresenterTest {
    private lateinit var presenter: SomePresenter
    private val mockNavigator = FakeNavigator(SomeScreen)
    private val mockDependency = mockk<Dependency>()

    @Before
    fun setup() {
        // Setup mocks
        presenter = SomePresenter(...)
    }

    @Test
    fun `when event happens then expected behavior occurs`() = runTest {
        presenter.test {
            val state = awaitItem()
            state.eventSink(SomeEvent)
            // Verify behavior
        }
    }
}
```

## Dependencies

- **circuit-test**: Circuit testing utilities
- **mockk**: Kotlin mocking framework
- **truth**: Fluent assertions
- **robolectric**: Android unit testing framework
- **kotlinx-coroutines-test**: Coroutine testing utilities

## Next Steps

1. Add proper Compose test context setup
2. Add remaining test cases for ConfigureNotificationMediumPresenter
3. Increase coverage to >80% for all presenter classes
4. Add integration tests for complete user flows

## Contributing

When adding new presenter tests:

1. Follow the established naming convention using backticks
2. Use descriptive test names that explain the scenario
3. Mock all external dependencies
4. Test both success and failure scenarios
5. Verify state transitions and navigation events
6. Use Truth assertions for readable test output
