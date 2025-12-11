# GitHub Copilot Instructions for Android Remote Notify

> **Note**: These instructions provide context and guidelines to help GitHub Copilot assist with code generation, suggestions, and understanding of this Android project's architecture, conventions, and best practices.

## Project Overview

**Android Remote Notify** is a specialized Android application that monitors battery and storage levels on remote Android devices and sends notifications when thresholds are exceeded. The app supports multiple notification channels including Email, Twilio SMS, Slack webhooks, Telegram, and REST webhooks.

### Key Features
- 🔋 Battery level monitoring with customizable thresholds
- 💾 Storage space monitoring with configurable alerts
- 📧 Multi-channel notifications (Email, SMS, Slack, Telegram, Webhooks)
- ⏰ Periodic background monitoring using WorkManager
- 📊 Alert history and logging with filtering capabilities
- 🎨 Modern Material3 UI with dark/light theme support

### Architecture Philosophy
- Clean Architecture with separation of concerns
- Reactive programming with Kotlin Coroutines and Flow
- Single Activity architecture with Compose navigation
- Repository pattern for data management
- Dependency injection for loose coupling
- Test-driven development approach

## Technology Stack & Architecture

### Core Technologies
- **Language**: Kotlin 2.2.0 with Java 17 compatibility
- **UI Framework**: Jetpack Compose with Material3 design system
- **Minimum SDK**: 30 (Android 11)
- **Target SDK**: 35 (Android 15)

### Architecture Components
- **Navigation & State Management**: [Slack Circuit](https://slackhq.github.io/circuit/) - Compose-driven architecture
- **Dependency Injection**: [Metro](https://zacsweers.github.io/metro/) - Kotlin-first dependency injection framework
- **Database**: Room 2.7.1 with SQLite backend
- **Networking**: Retrofit 3.0.0 + OkHttp 4.12.0 + Moshi 1.15.2
- **Background Processing**: WorkManager 2.10.1 for periodic health checks
- **Data Storage**: DataStore 1.1.6 for preferences and configuration
- **Asynchronous Operations**: Kotlin Coroutines with Flow for reactive streams

### Supporting Libraries
- **Logging**: Timber 5.0.1 with Firebase Crashlytics integration
- **JSON Processing**: Moshi with KSP code generation
- **Error Handling**: [EitherNet](https://github.com/slackhq/EitherNet) for network error modeling
- **Firebase**: Crashlytics and Analytics
- **Testing**: JUnit 4, MockK, Robolectric, Truth assertions, Espresso

## Code Style & Conventions

### Kotlin Style
- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use **ktlint** formatting rules (enforced via Kotlinter plugin)
- Prefer Kotlin coroutines over callbacks
- Use data classes for immutable models
- Leverage Kotlin's null-safety features

### Compose Guidelines
- Follow [Compose API Guidelines](https://github.com/androidx/androidx/blob/androidx-main/compose/docs/compose-api-guidelines.md)
- Use `@Composable` functions with proper naming (PascalCase)
- Prefer stateless composables with state hoisting
- Use `remember` for expensive computations
- Implement proper `@Preview` functions for UI components

### Architecture Patterns
- **Circuit Pattern**: Use Presenter/UI separation for screens
- **Repository Pattern**: Centralize data access logic
- **Use Case Pattern**: Encapsulate business logic
- **Observer Pattern**: Use Flow for reactive data streams

### Naming Conventions
```kotlin
// Classes: PascalCase
class RemoteAlertRepository

// Functions and variables: camelCase
fun sendNotification()
val batteryLevel: Int

// Constants: UPPER_SNAKE_CASE
const val DEFAULT_BATTERY_THRESHOLD = 20

// Compose UI: PascalCase with descriptive names
@Composable
fun AlertConfigurationScreen()

// Test functions: backticks with descriptive names
@Test
fun `saveBotToken saves token and can be retrieved`()
```

## Project Structure

```
app/src/main/java/dev/hossain/remotenotify/
├── MainActivity.kt                    # Single activity host
├── RemoteAlertApp.kt                 # Application class
├── analytics/                        # Analytics and tracking
├── data/                            # Data layer (repositories, data sources)
│   ├── *Repository.kt               # Repository implementations
│   ├── *DataStore.kt               # DataStore preferences
│   └── remote/                     # Network data sources
├── db/                             # Room database components
│   ├── entities/                   # Database entities
│   ├── dao/                       # Data Access Objects
│   └── AppDatabase.kt             # Database configuration
├── di/                            # Dependency injection modules
│   ├── AppGraph.kt                # Main Metro dependency graph
│   ├── CircuitProviders.kt        # Circuit framework providers
│   └── *Bindings.kt               # Metro binding containers
├── model/                         # Domain models and data classes
├── monitor/                       # Device monitoring logic
├── notifier/                      # Notification channel implementations
├── theme/                         # Compose theme and styling
├── ui/                           # UI layer (screens and components)
│   ├── */                        # Feature-based screen packages
│   │   ├── *Screen.kt            # Circuit screen, presenter, and UI in combined files
│   │   └── *ConfigInputUi.kt     # Feature-specific UI components
│   └── components/               # Reusable UI components
├── utils/                        # Utility classes and extensions
└── worker/                       # Background work implementations
```

### Test Structure
```
app/src/test/java/                    # Unit tests
app/src/androidTest/java/             # Instrumentation tests
```

## Development Guidelines

### Circuit Framework Usage
When creating new screens, follow the Circuit pattern where Screen, Presenter, and UI are defined in the same file:

```kotlin
// All-in-one file: SettingsScreen.kt

// Screen definition
@Parcelize
data object SettingsScreen : Screen {
    data class State(
        val settings: Settings,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed class Event : CircuitUiEvent {
        data object NavigateBack : Event()
        // ... other events
    }
}

// Presenter with Metro assisted injection
@Inject
class SettingsPresenter(
    @Assisted private val navigator: Navigator,
    // Inject other dependencies
) : Presenter<SettingsScreen.State> {
    
    @Composable
    override fun present(): SettingsScreen.State {
        // State management logic
        return SettingsScreen.State(
            settings = currentSettings,
            eventSink = { event ->
                when (event) {
                    SettingsScreen.Event.NavigateBack -> navigator.pop()
                    // Handle other events
                }
            }
        )
    }
    
    @CircuitInject(SettingsScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator): SettingsPresenter
    }
}

// UI in the same file
@CircuitInject(SettingsScreen::class, AppScope::class)
@Composable
fun SettingsUi(state: SettingsScreen.State, modifier: Modifier = Modifier) {
    // UI composition
}
```

Note: The project uses `@CircuitInject` annotations on presenter factories and UI functions for automatic registration. Circuit uses this information to automatically create presenter and UI factories that are collected via `CircuitProviders.kt`.

### Dependency Injection Patterns
Use Metro for Kotlin-first dependency injection:

```kotlin
// Binding containers for organizing related providers
@BindingContainer
object NetworkBindings {
    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        // Implementation
    }
}

// Circuit providers use @ContributesTo for interface-based modules
@ContributesTo(AppScope::class)
interface CircuitProviders {
    @Multibinds fun presenterFactories(): Set<Presenter.Factory>
    @Multibinds fun viewFactories(): Set<Ui.Factory>
}

// Constructor injection with contributions
@ContributesBinding(AppScope::class)
@Inject
class RepositoryImpl(
    private val dataSource: DataSource
) : Repository

// Multibinding contributions
@ContributesIntoSet(AppScope::class)
@Inject
class NotificationSenderImpl(
    private val httpClient: OkHttpClient
) : NotificationSender

// Dependency graph configuration
@DependencyGraph(
    AppScope::class,
    bindingContainers = [
        AnalyticsBindings::class,
        AppBindings::class,
        DatabaseBindings::class,
        NetworkBindings::class,
        NotificationSenderMultibindings::class,
        WorkerBindings::class,
    ],
)
interface AppGraph {
    val workManager: WorkManager
    val workerFactory: WorkerFactory
    
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: Application): AppGraph
    }
}
```

### Data Layer Patterns
```kotlin
// Repository pattern with Flow
interface RemoteAlertRepository {
    fun getAlerts(): Flow<List<RemoteAlert>>
    suspend fun saveAlert(alert: RemoteAlert): Result<Unit>
}

// DataStore usage with Metro injection
@Inject
class ConfigurationDataStore(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore
    
    val configuration = dataStore.data.map { preferences ->
        // Map preferences to domain model
    }
    
    suspend fun saveConfiguration(config: Configuration) {
        dataStore.edit { preferences ->
            // Save configuration
        }
    }
}
```

### Background Work
Use WorkManager for background tasks:

```kotlin
@AssistedFactory
interface HealthWorkerFactory {
    fun create(appContext: Context, params: WorkerParameters): HealthWorker
}

@Inject
class HealthWorker(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: Repository
) : CoroutineWorker(appContext, params) {
    
    override suspend fun doWork(): Result {
        // Background work implementation
    }
}
```

### Error Handling
Use EitherNet for network error handling:

```kotlin
interface ApiService {
    @GET("endpoint")
    suspend fun getData(): ApiResult<DataResponse, ApiError>
}

// In repository
suspend fun fetchData(): Flow<Result<Data>> = flow {
    apiService.getData().fold(
        onSuccess = { response -> emit(Result.success(response.data)) },
        onFailure = { error -> emit(Result.failure(error.toException())) }
    )
}
```

## Testing Guidelines

### Unit Testing
- Use JUnit 4 for test framework
- MockK for mocking dependencies
- Truth for fluent assertions
- Robolectric for Android component testing

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RepositoryTest {
    
    @Test
    fun `saveConfiguration saves data and can be retrieved`() = runTest {
        // Given
        val testConfig = Configuration(...)
        
        // When
        repository.saveConfiguration(testConfig)
        
        // Then
        val result = repository.getConfiguration().first()
        assertThat(result).isEqualTo(testConfig)
    }
}
```

### Compose Testing
```kotlin
@Test
fun myComposableTest() {
    composeTestRule.setContent {
        MyComposable(state = testState)
    }
    
    composeTestRule
        .onNodeWithText("Expected Text")
        .assertExists()
}
```

### Test Naming
Use descriptive test names with backticks:
```kotlin
@Test
fun `when battery level is below threshold then notification is sent`()

@Test
fun `given invalid configuration when saving then error is returned`()
```

## Build & Development Tools

### Gradle Tasks
```bash
# Build and testing
./gradlew clean build
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest   # Instrumentation tests

# Code quality
./gradlew lintDebug              # Android Lint
./gradlew formatKotlin           # Format code with ktlint
./gradlew lintKotlin            # Lint Kotlin code
./gradlew koverHtmlReport        # Generate code coverage

# Compose tooling
./gradlew compileDebugKotlin     # Check Compose compiler
```

### Code Quality Tools
- **Kotlinter**: Enforces ktlint rules for consistent formatting
- **Android Lint**: Static analysis for Android-specific issues
- **Compose Lint**: Compose-specific lint checks
- **Kover**: Kotlin code coverage reporting

### Required Configuration
Create `local.properties` file with:
```properties
EMAIL_API_KEY=your_email_api_key_here
```

## Firebase Integration

### Crashlytics Setup
- Debug builds use Timber.DebugTree for logging
- Release builds use CrashlyticsTree for crash reporting
- Custom crash reporting for specific error scenarios

### Analytics
- Track user engagement and app usage patterns
- Monitor notification success rates
- Performance metrics for background work

## Common Patterns & Best Practices

### State Management
```kotlin
// Use sealed classes for screen states
sealed interface ScreenState {
    data object Loading : ScreenState
    data class Success(val data: Data) : ScreenState
    data class Error(val message: String) : ScreenState
}

// Compose state handling
@Composable
fun MyScreen(state: ScreenState) {
    when (state) {
        is Loading -> LoadingIndicator()
        is Success -> SuccessContent(state.data)
        is Error -> ErrorMessage(state.message)
    }
}
```

### Resource Management
```kotlin
// Use sealed classes for string resources
sealed class StringResource {
    data object AppName : StringResource()
    data class Formatted(val resId: Int, vararg val args: Any) : StringResource()
}

// Extension for easy string resolution
@Composable
fun StringResource.resolve(): String = when (this) {
    is AppName -> stringResource(R.string.app_name)
    is Formatted -> stringResource(resId, *args)
}
```

### Network Requests
```kotlin
// Repository pattern with error handling
class NetworkRepository @Inject constructor(
    private val apiService: ApiService,
    private val errorHandler: ErrorHandler
) : Repository {
    
    override suspend fun fetchData(): Flow<Result<Data>> = flow {
        emit(Result.Loading)
        try {
            val response = apiService.getData()
            emit(Result.Success(response))
        } catch (exception: Exception) {
            emit(Result.Error(errorHandler.handle(exception)))
        }
    }.flowOn(Dispatchers.IO)
}
```

## Performance Considerations

### Compose Performance
- Use `remember` for expensive calculations
- Prefer `LazyColumn`/`LazyRow` for large lists
- Use `derivedStateOf` for computed states
- Avoid creating objects in Composition

### Background Work Optimization
- Use WorkManager constraints appropriately
- Respect Android's battery optimization settings
- Implement exponential backoff for failed work
- Use PeriodicWorkRequest with minimum 15-minute intervals

### Memory Management
- Use `StateFlow` instead of `LiveData` for better performance
- Properly scope coroutines to avoid leaks
- Use `collectAsState()` in Compose for Flow observation

## Security Considerations

### API Keys & Secrets
- Store sensitive data in `local.properties` (not in VCS)
- Use BuildConfig for compile-time constants
- Consider using Android Keystore for runtime secrets

### Network Security
- Implement certificate pinning for production APIs
- Use HTTPS for all network communications
- Validate server responses before processing

## Deployment & Release

### Release Process
**IMPORTANT**: All release changes must be made on a dedicated release branch, NOT on `main`.

1. **Create Release Branch**: `git checkout -b release/vX.Y.Z` (e.g., `release/v1.17`)
2. **Update Version**: Increment `versionCode` and `versionName` in `app/build.gradle.kts`
3. **Update Release Notes**: Add new release section in `project-resources/google-play/release-notes.md`
4. **Build & Test**: Run `./gradlew clean assembleRelease bundleRelease`
5. **Commit Changes**: Commit version bump and release notes on the release branch
6. **Create PR**: Open pull request from release branch to `main`
7. **Merge & Tag**: After PR approval, merge to `main` and create git tag `vX.Y.Z`
8. **Upload to Play Store**: Upload AAB to Google Play Console

### Version Management
- Update `versionCode` (increment by 1) and `versionName` in `app/build.gradle.kts`
- Update release notes in `project-resources/google-play/release-notes.md`
- Follow semantic versioning (MAJOR.MINOR.PATCH)
- Create git tag matching the version (e.g., `v1.17`)

### Release Checklist
- [ ] Create release branch (e.g., `release/v1.17`)
- [ ] Update version code and name in `app/build.gradle.kts`
- [ ] Update `release-notes.md` with new release section
- [ ] Check `local.properties` for correct keystore path and password
- [ ] Build release: `./gradlew clean assembleRelease bundleRelease`
- [ ] Test release build locally
- [ ] Run full test suite: `./gradlew test`
- [ ] Check ProGuard/R8 obfuscation
- [ ] Verify Firebase integration
- [ ] Commit and push release branch
- [ ] Create pull request to `main`
- [ ] After merge, create git tag: `git tag -a vX.Y.Z -m "Release vX.Y.Z"`
- [ ] Push tag: `git push origin vX.Y.Z`
- [ ] Upload AAB to Firebase Test Lab
- [ ] Upload AAB to Google Play Console
- [ ] Create GitHub release from tag with release notes

## Troubleshooting

### Common Issues
1. **Build Failures**: Ensure `local.properties` contains required API keys
2. **WorkManager Issues**: Check device battery optimization settings
3. **Firebase Issues**: Verify `google-services.json` is present and valid
4. **Compose Issues**: Clean and rebuild project, check Compose compiler version

### Debugging Tools
- Use `Timber.d()` for debug logging
- Firebase Crashlytics for production crashes
- Android Studio's Database Inspector for Room debugging
- WorkManager Inspector for background job debugging

## Contributing Guidelines

When contributing to this project:

1. **Follow the established architecture patterns**
2. **Write comprehensive tests for new features**
3. **Update documentation for significant changes**
4. **Use descriptive commit messages**
5. **Ensure code passes all quality checks**
6. **Test on multiple device configurations**

### Code Review Checklist
- [ ] Follows Kotlin coding conventions
- [ ] Includes appropriate tests
- [ ] Handles error cases properly
- [ ] Uses proper dependency injection
- [ ] Implements accessibility features
- [ ] Considers performance implications
- [ ] Updates relevant documentation

---

This document provides comprehensive guidance for working with the Android Remote Notify codebase. For questions or clarifications, refer to the existing code patterns and architecture decisions implemented throughout the project.