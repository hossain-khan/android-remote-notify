# UI/Presenter Tests

This directory contains comprehensive unit tests for UI/Presenter layer using Circuit testing utilities.

## Current Status

**Tests Compile:** ✅ All 45+ test cases compile successfully  
**Tests Run:** ❌ Tests fail at runtime due to missing Compose context (LocalContext)

### Issue

Circuit presenters use `LocalContext.current` which requires a proper Compose runtime environment. Unit tests with Robolectric need additional setup to provide this context.

### Root Cause

The `presenter.test()` method from Circuit testing utilities internally calls the Composable `present()` method, which accesses `LocalContext.current`. In unit tests, this composition local is not available by default.

### Error Message

```
app.cash.turbine.TurbineAssertionError: Expected item but found Error(IllegalStateException)
Caused by: java.lang.IllegalStateException: CompositionLocal LocalContext not present
```

### Solution Approaches Investigated

1. **Using ComposeTestRule** - Doesn't work in unit tests with suspend functions
2. **Using runComposeUiTest** - API incompatibility with test structure  
3. **Using @Config annotation** - Not sufficient to provide LocalContext
4. **Mocking LocalContext** - Complex due to Compose internals

### Recommended Solution

**Option A (Recommended)**: Refactor the presenters to inject `Context` as a constructor dependency instead of using `LocalContext.current`. This would:
- Make presenters more testable
- Remove Compose-specific requirements from unit tests
- Allow standard Robolectric testing without Compose environment
- Follow dependency injection best practices

**Option B**: Convert to instrumentation tests
- Move tests to `androidTest` directory  
- Use full Compose environment
- Slower execution but guaranteed Compose support

**Option C**: Deep Compose mocking
- Mock CompositionLocalProvider
- Provide test Context through composition
- Complex and fragile solution

## Test Coverage

### Implemented Tests (45+ Total)

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

### Run All UI Tests (Currently Failing)

```bash
./gradlew testDebugUnitTest --tests "dev.hossain.remotenotify.ui.*"
```

### Expected Failure

All tests will fail with `CompositionLocal LocalContext not present` error.

## Test Structure

All tests follow this pattern:

```kotlin
@Config(sdk = [34])
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
- **androidx.ui.test.junit4**: Compose UI testing (added but insufficient alone)

## Next Steps

To make these tests functional:

1. **Option A (Recommended)**: Refactor presenters to inject Context as dependency
   ```kotlin
   // Before
   @Composable
   override fun present(): State {
       val context = LocalContext.current
       // use context...
   }
   
   // After
   class SomePresenter(
       private val context: Context,
       // other dependencies...
   ) : Presenter<State> {
       @Composable
       override fun present(): State {
           // use context directly...
       }
   }
   ```

2. **Option B**: Convert to instrumentation tests
   - Move tests from `test/` to `androidTest/`
   - Use `@get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()`
   - Full Compose environment available

3. **Option C**: Investigate Circuit-specific test utilities
   - Check if Circuit provides test context providers
   - Review Circuit testing documentation for LocalContext handling

## Contributing

When fixing or adding presenter tests:

1. Follow the established naming convention using backticks
2. Use descriptive test names that explain the scenario
3. Mock all external dependencies
4. Test both success and failure scenarios
5. Verify state transitions and navigation events
6. Use Truth assertions for readable test output

## Technical Notes

- All tests compile successfully with proper imports and annotations
- Test structure and assertions are correct
- Only runtime Compose context initialization prevents execution
- This is a known limitation of testing Composables in unit tests
- Consider this technical debt to be addressed in future refactoring

## References

- [Circuit Testing Documentation](https://slackhq.github.io/circuit/testing/)
- [Compose Testing Guide](https://developer.android.com/jetpack/compose/testing)
- [Robolectric Documentation](http://robolectric.org/)
