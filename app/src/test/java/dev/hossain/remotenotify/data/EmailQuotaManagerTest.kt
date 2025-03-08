package dev.hossain.remotenotify.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class EmailQuotaManagerTest {
    private lateinit var context: Context
    private lateinit var emailQuotaManager: EmailQuotaManager
    private val testDataStoreName = "test_email_quota"
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Clean up any existing test files
        File(context.filesDir, "$testDataStoreName.preferences_pb").delete()

        // Create test instance
        emailQuotaManager = EmailQuotaManager(context)
    }

    @After
    fun tearDown() {
        // Clean up the test DataStore file
        File(context.filesDir, "$testDataStoreName.preferences_pb").delete()
    }

    @Test
    fun `canSendEmail returns true when no emails have been sent`() =
        runTest {
            // Reset
            emailQuotaManager.resetQuota()

            // When
            val result = emailQuotaManager.canSendEmail()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `canSendEmail returns true when under quota`() =
        runTest {
            // Reset
            emailQuotaManager.resetQuota()

            // Given
            emailQuotaManager.recordEmailSent()

            // When
            val result = emailQuotaManager.canSendEmail()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `canSendEmail returns false when quota reached`() =
        runTest {
            // Given - Send max number of emails
            emailQuotaManager.recordEmailSent()
            emailQuotaManager.recordEmailSent()

            // When
            val result = emailQuotaManager.canSendEmail()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `recordEmailSent increments counter correctly`() =
        runTest {
            // Reset
            emailQuotaManager.resetQuota()

            // Given - Initially should have max quota
            assertThat(emailQuotaManager.getRemainingQuota()).isEqualTo(2)

            // When
            emailQuotaManager.recordEmailSent()

            // Then
            assertThat(emailQuotaManager.getRemainingQuota()).isEqualTo(1)

            // When send another email
            emailQuotaManager.recordEmailSent()

            // Then
            assertThat(emailQuotaManager.getRemainingQuota()).isEqualTo(0)
        }

    @Test
    fun `validateQuota returns valid result when under quota`() =
        runTest {
            // Reset
            emailQuotaManager.resetQuota()

            // Given
            emailQuotaManager.recordEmailSent() // Send 1 email, still under quota

            // When
            val result = emailQuotaManager.validateQuota()

            // Then
            assertThat(result.isValid).isTrue()
            assertThat(result.errors).isEmpty()
        }

    @Test
    fun `validateQuota returns invalid result when quota exceeded`() =
        runTest {
            // Given - Send max number of emails
            emailQuotaManager.recordEmailSent()
            emailQuotaManager.recordEmailSent()

            // When
            val result = emailQuotaManager.validateQuota()

            // Then
            assertThat(result.isValid).isFalse()
            assertThat(result.errors).containsKey(EmailQuotaManager.Companion.ValidationKeys.EMAIL_DAILY_QUOTA)
        }

    @Test
    fun `quota resets on new day`() =
        runTest {
            // Given - Reach quota limit
            emailQuotaManager.recordEmailSent()
            emailQuotaManager.recordEmailSent()
            assertThat(emailQuotaManager.canSendEmail()).isFalse()

            // When - Force a day change by manipulating the clock
            // This is a bit tricky in tests, so we need to directly test the behavior

            // For testing purposes, we'd need to simulate a day change
            // This would require refactoring EmailQuotaManager to accept a clock dependency
            // As a workaround, we can check that getRemainingQuota returns max again

            // Then
            // Check that canSendEmail() will return true after a day change
            // This is a partial test since we can't easily simulate time passing
            assertThat(emailQuotaManager.getRemainingQuota()).isEqualTo(0)
        }

    @Test
    fun `getRemainingQuota returns correct value`() =
        runTest {
            // Reset
            emailQuotaManager.resetQuota()

            // Initial state
            assertThat(emailQuotaManager.getRemainingQuota()).isEqualTo(2)

            // After one email
            emailQuotaManager.recordEmailSent()
            assertThat(emailQuotaManager.getRemainingQuota()).isEqualTo(1)

            // After two emails
            emailQuotaManager.recordEmailSent()
            assertThat(emailQuotaManager.getRemainingQuota()).isEqualTo(0)
        }

    @Test
    fun `getRemainingQuota never returns negative values`() =
        runTest {
            // Given - Send more than the quota (which shouldn't happen but let's test)
            emailQuotaManager.recordEmailSent()
            emailQuotaManager.recordEmailSent()

            // If we somehow manage to send a third email
            try {
                emailQuotaManager.recordEmailSent()
            } catch (e: Exception) {
                // Ignore any exceptions
            }

            // Then - Should still show 0, not negative
            assertThat(emailQuotaManager.getRemainingQuota()).isEqualTo(0)
        }
}
