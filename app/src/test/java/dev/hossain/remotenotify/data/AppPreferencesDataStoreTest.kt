package dev.hossain.remotenotify.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.worker.DEFAULT_PERIODIC_INTERVAL_MINUTES
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppPreferencesDataStoreTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())
    private lateinit var context: Context
    private lateinit var appPreferencesDataStore: AppPreferencesDataStore
    private val testDataStoreName = "test_app_preferences"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any existing test files
        File(context.filesDir, "$testDataStoreName.preferences").delete()

        // Create test instance
        appPreferencesDataStore = AppPreferencesDataStore(context)
    }

    @After
    fun tearDown() {
        // Clean up the test DataStore file
        File(context.filesDir, "$testDataStoreName.preferences").delete()
    }

    @Test
    fun `workerIntervalFlow defaults to DEFAULT_PERIODIC_INTERVAL_MINUTES when not set`() =
        runTest {
            // When
            val result = appPreferencesDataStore.workerIntervalFlow.first()

            // Then
            assertThat(result).isEqualTo(DEFAULT_PERIODIC_INTERVAL_MINUTES)
        }

    @Test
    fun `lastReviewRequestFlow defaults to 0 when not set`() =
        runTest {
            appPreferencesDataStore.resetAll()

            // When
            val result = appPreferencesDataStore.lastReviewRequestFlow.first()

            // Then
            assertThat(result).isEqualTo(0)
        }

    @Test
    fun `isFirstTimeDialogShown defaults to false when not set`() =
        runTest {
            // When
            val result = appPreferencesDataStore.isFirstTimeDialogShown.first()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `saveWorkerInterval updates interval value`() =
        runTest {
            // Given
            val initialValue = appPreferencesDataStore.workerIntervalFlow.first()
            assertThat(initialValue).isEqualTo(DEFAULT_PERIODIC_INTERVAL_MINUTES)

            // When
            val newInterval = 60L
            appPreferencesDataStore.saveWorkerInterval(newInterval)

            // Then
            val updatedValue = appPreferencesDataStore.workerIntervalFlow.first()
            assertThat(updatedValue).isEqualTo(newInterval)
        }

    @Test
    fun `saveLastReviewRequestTime updates timestamp value`() =
        runTest {
            // Given
            val initialValue = appPreferencesDataStore.lastReviewRequestFlow.first()
            assertThat(initialValue).isEqualTo(0)

            // When
            val timestamp = System.currentTimeMillis()
            appPreferencesDataStore.saveLastReviewRequestTime(timestamp)

            // Then
            val updatedValue = appPreferencesDataStore.lastReviewRequestFlow.first()
            assertThat(updatedValue).isEqualTo(timestamp)
        }

    @Test
    fun `saveFirstTimeDialogShown updates dialog shown status`() =
        runTest {
            // Given
            val initialValue = appPreferencesDataStore.isFirstTimeDialogShown.first()
            assertThat(initialValue).isFalse()

            // When
            appPreferencesDataStore.markEducationDialogShown()

            // Then
            val updatedValue = appPreferencesDataStore.isFirstTimeDialogShown.first()
            assertThat(updatedValue).isTrue()
        }

    @Test
    fun `saveWorkerInterval can update value multiple times`() =
        runTest {
            appPreferencesDataStore.resetAll()

            // Initial state
            assertThat(appPreferencesDataStore.workerIntervalFlow.first()).isEqualTo(DEFAULT_PERIODIC_INTERVAL_MINUTES)

            // First update
            appPreferencesDataStore.saveWorkerInterval(30L)
            assertThat(appPreferencesDataStore.workerIntervalFlow.first()).isEqualTo(30L)

            // Second update
            appPreferencesDataStore.saveWorkerInterval(60L)
            assertThat(appPreferencesDataStore.workerIntervalFlow.first()).isEqualTo(60L)

            // Third update
            appPreferencesDataStore.saveWorkerInterval(15L)
            assertThat(appPreferencesDataStore.workerIntervalFlow.first()).isEqualTo(15L)
        }

    @Test
    fun `values persist across DataStore instances`() =
        runTest {
            // Given
            val timestamp = System.currentTimeMillis()
            appPreferencesDataStore.saveWorkerInterval(45L)
            appPreferencesDataStore.saveLastReviewRequestTime(timestamp)
            appPreferencesDataStore.markEducationDialogShown()

            // When - recreate the DataStore instance
            val newInstance = AppPreferencesDataStore(context)

            // Then
            assertThat(newInstance.workerIntervalFlow.first()).isEqualTo(45L)
            assertThat(newInstance.lastReviewRequestFlow.first()).isEqualTo(timestamp)
            assertThat(newInstance.isFirstTimeDialogShown.first()).isTrue()
        }
}
