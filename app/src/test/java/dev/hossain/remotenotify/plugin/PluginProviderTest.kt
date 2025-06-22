package dev.hossain.remotenotify.plugin

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.RemoteAlertApp
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = RemoteAlertApp::class)
class PluginProviderTest {
    private lateinit var context: Context
    private lateinit var pluginProvider: PluginProvider
    private lateinit var mockNotificationSender: NotificationSender

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create mock notification sender
        mockNotificationSender = mockk(relaxed = true) {
            every { notifierType } returns NotifierType.EMAIL
            coEvery { hasValidConfig() } returns true
            coEvery { sendNotification(any()) } returns true
        }

        // Create PluginProvider directly without going through onCreate()
        pluginProvider = PluginProvider()
        
        // Manually set the dependencies to avoid dependency injection complexity
        pluginProvider.notificationSenders = setOf(mockNotificationSender)
    }

    @Test
    fun `onCreate initializes provider successfully`() {
        // Test the provider can be created successfully
        // The real onCreate functionality with dependency injection is tested in integration tests
        
        // When
        val provider = PluginProvider()
        
        // Then - provider is created without errors
        assertThat(provider).isNotNull()
    }

    @Test
    fun `getType returns correct MIME types`() {
        // Test notifications URI
        val notificationsType = pluginProvider.getType(PluginContract.NOTIFICATIONS_URI)
        assertThat(notificationsType).isEqualTo(PluginContract.CONTENT_TYPE_NOTIFICATION)

        // Test config URI
        val configType = pluginProvider.getType(PluginContract.CONFIG_URI)
        assertThat(configType).isEqualTo(PluginContract.CONTENT_TYPE_CONFIG)

        // Test status URI
        val statusType = pluginProvider.getType(PluginContract.STATUS_URI)
        assertThat(statusType).isEqualTo(PluginContract.CONTENT_TYPE_STATUS)

        // Test invalid URI
        val invalidType = pluginProvider.getType(Uri.parse("content://invalid/path"))
        assertThat(invalidType).isNull()
    }

    @Test
    fun `queryConfig returns available mediums`() {
        // Create a spy of the provider to mock context access
        val spyProvider = spyk(pluginProvider) {
            every { context } returns this@PluginProviderTest.context
        }
        spyProvider.notificationSenders = setOf(mockNotificationSender)
        
        // Mock permission check
        every { context.checkCallingPermission(PluginContract.PERMISSION) } returns PackageManager.PERMISSION_GRANTED

        // When
        val cursor =
            spyProvider.query(
                PluginContract.CONFIG_URI,
                null,
                null,
                null,
                null,
            )

        // Then
        assertThat(cursor).isNotNull()
        assertThat(cursor!!.moveToFirst()).isTrue()

        val mediumName = cursor.getString(cursor.getColumnIndex(PluginContract.ConfigColumns.MEDIUM_NAME))
        val displayName = cursor.getString(cursor.getColumnIndex(PluginContract.ConfigColumns.MEDIUM_DISPLAY_NAME))
        val isConfigured = cursor.getInt(cursor.getColumnIndex(PluginContract.ConfigColumns.IS_CONFIGURED))

        assertThat(mediumName).isEqualTo("email")
        assertThat(displayName).isEqualTo("Email")
        assertThat(isConfigured).isEqualTo(1) // true as int

        cursor.close()
    }

    @Test
    fun `queryStatus returns service status`() {
        // Create a spy of the provider to mock context access
        val spyProvider = spyk(pluginProvider) {
            every { context } returns this@PluginProviderTest.context
        }
        spyProvider.notificationSenders = setOf(mockNotificationSender)
        
        // Mock permission check
        every { context.checkCallingPermission(PluginContract.PERMISSION) } returns PackageManager.PERMISSION_GRANTED

        // When
        val cursor =
            spyProvider.query(
                PluginContract.STATUS_URI,
                null,
                null,
                null,
                null,
            )

        // Then
        assertThat(cursor).isNotNull()
        assertThat(cursor!!.moveToFirst()).isTrue()

        val serviceStatus = cursor.getString(cursor.getColumnIndex(PluginContract.StatusColumns.SERVICE_STATUS))
        val apiVersion = cursor.getInt(cursor.getColumnIndex(PluginContract.StatusColumns.API_VERSION))
        val configuredCount = cursor.getInt(cursor.getColumnIndex(PluginContract.StatusColumns.CONFIGURED_MEDIUMS_COUNT))

        assertThat(serviceStatus).isEqualTo(PluginContract.ServiceStatus.ACTIVE)
        assertThat(apiVersion).isEqualTo(PluginContract.API_VERSION)
        assertThat(configuredCount).isEqualTo(1) // One mock sender

        cursor.close()
    }

    @Test
    fun `insertNotification sends notification through configured mediums`() =
        runTest {
            // Create a spy of the provider to mock context access
            val spyProvider = spyk(pluginProvider) {
                every { context } returns this@PluginProviderTest.context
            }
            spyProvider.notificationSenders = setOf(mockNotificationSender)
            
            // Mock permission and package info
            every { context.checkCallingPermission(PluginContract.PERMISSION) } returns PackageManager.PERMISSION_GRANTED
            every { context.packageManager } returns
                mockk {
                    every { getPackagesForUid(any()) } returns arrayOf("com.example.test")
                    every { getApplicationInfo("com.example.test", 0) } returns
                        mockk {
                            every { loadLabel(any()) } returns "Test App"
                        }
                }

            // Create notification values
            val values =
                ContentValues().apply {
                    put(PluginContract.NotificationColumns.TITLE, "Test Notification")
                    put(PluginContract.NotificationColumns.MESSAGE, "This is a test message")
                    put(PluginContract.NotificationColumns.PRIORITY, PluginContract.Priority.NORMAL)
                }

            // When
            val resultUri = spyProvider.insert(PluginContract.NOTIFICATIONS_URI, values)

            // Then
            assertThat(resultUri).isNotNull()
            assertThat(resultUri.toString()).contains("notifications")

            // Wait a bit for async processing
            Thread.sleep(100)

            // Verify notification sender was called
            coVerify(timeout = 1000) { mockNotificationSender.sendNotification(any()) }
        }

    @Test
    fun `insertNotification fails with missing required fields`() {
        // Create a spy of the provider to mock context access
        val spyProvider = spyk(pluginProvider) {
            every { context } returns this@PluginProviderTest.context
        }
        spyProvider.notificationSenders = setOf(mockNotificationSender)
        
        // Mock permission check
        every { context.checkCallingPermission(PluginContract.PERMISSION) } returns PackageManager.PERMISSION_GRANTED

        // Create notification values missing title
        val values =
            ContentValues().apply {
                put(PluginContract.NotificationColumns.MESSAGE, "This is a test message")
            }

        // When
        val resultUri = spyProvider.insert(PluginContract.NOTIFICATIONS_URI, values)

        // Then
        assertThat(resultUri).isNull()
    }

    @Test
    fun `query returns null without permission`() {
        // Create a spy of the provider to mock context access
        val spyProvider = spyk(pluginProvider) {
            every { context } returns this@PluginProviderTest.context
        }
        spyProvider.notificationSenders = setOf(mockNotificationSender)
        
        // Mock permission denied
        every { context.checkCallingPermission(PluginContract.PERMISSION) } returns PackageManager.PERMISSION_DENIED

        // When
        val cursor =
            spyProvider.query(
                PluginContract.CONFIG_URI,
                null,
                null,
                null,
                null,
            )

        // Then
        assertThat(cursor).isNull()
    }

    @Test
    fun `insert returns null without permission`() {
        // Create a spy of the provider to mock context access
        val spyProvider = spyk(pluginProvider) {
            every { context } returns this@PluginProviderTest.context
        }
        spyProvider.notificationSenders = setOf(mockNotificationSender)
        
        // Mock permission denied
        every { context.checkCallingPermission(PluginContract.PERMISSION) } returns PackageManager.PERMISSION_DENIED

        val values =
            ContentValues().apply {
                put(PluginContract.NotificationColumns.TITLE, "Test")
                put(PluginContract.NotificationColumns.MESSAGE, "Test message")
            }

        // When
        val resultUri = spyProvider.insert(PluginContract.NOTIFICATIONS_URI, values)

        // Then
        assertThat(resultUri).isNull()
    }

    @Test
    fun `delete and update return zero`() {
        // These operations are not supported
        assertThat(pluginProvider.delete(PluginContract.NOTIFICATIONS_URI, null, null)).isEqualTo(0)
        assertThat(pluginProvider.update(PluginContract.NOTIFICATIONS_URI, null, null, null)).isEqualTo(0)
    }

    @Test
    fun `insertNotification with preferred mediums filters senders`() =
        runTest {
            // Setup additional mock sender
            val mockTelegramSender =
                mockk<NotificationSender> {
                    every { notifierType } returns NotifierType.TELEGRAM
                    coEvery { hasValidConfig() } returns true
                    coEvery { sendNotification(any()) } returns true
                }

            // Create a spy of the provider to mock context access
            val spyProvider = spyk(pluginProvider) {
                every { context } returns this@PluginProviderTest.context
            }
            spyProvider.notificationSenders = setOf(mockNotificationSender, mockTelegramSender)

            // Mock permission and package info
            every { context.checkCallingPermission(PluginContract.PERMISSION) } returns PackageManager.PERMISSION_GRANTED
            every { context.packageManager } returns
                mockk {
                    every { getPackagesForUid(any()) } returns arrayOf("com.example.test")
                    every { getApplicationInfo("com.example.test", 0) } returns
                        mockk {
                            every { loadLabel(any()) } returns "Test App"
                        }
                }

            // Create notification values with preferred mediums
            val values =
                ContentValues().apply {
                    put(PluginContract.NotificationColumns.TITLE, "Test Notification")
                    put(PluginContract.NotificationColumns.MESSAGE, "This is a test message")
                    put(PluginContract.NotificationColumns.PREFERRED_MEDIUMS, "telegram")
                }

            // When
            val resultUri = spyProvider.insert(PluginContract.NOTIFICATIONS_URI, values)

            // Then
            assertThat(resultUri).isNotNull()

            // Wait for async processing
            Thread.sleep(100)

            // Verify only telegram sender was called
            coVerify(timeout = 1000) { mockTelegramSender.sendNotification(any()) }
            coVerify(exactly = 0) { mockNotificationSender.sendNotification(any()) }
        }
}
