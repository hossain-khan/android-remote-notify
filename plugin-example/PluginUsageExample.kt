package dev.hossain.remotenotify.plugin.example

import android.content.Context
import android.util.Log

/**
 * Example demonstrating how to use the Remote Notify plugin in your Android app.
 * 
 * This class shows various use cases and best practices for integrating with
 * the plugin architecture.
 */
class PluginUsageExample(context: Context) {
    
    private val pluginHelper = RemoteNotifyPluginHelper(context)
    
    companion object {
        private const val TAG = "PluginExample"
    }
    
    /**
     * Example 1: Basic notification sending
     */
    fun sendBasicNotification() {
        val success = pluginHelper.sendNotification(
            title = "Hello from Plugin",
            message = "This is a test notification sent through Remote Notify plugin!"
        )
        
        if (success) {
            Log.i(TAG, "Basic notification sent successfully")
        } else {
            Log.e(TAG, "Failed to send basic notification")
        }
    }
    
    /**
     * Example 2: High priority notification
     */
    fun sendUrgentNotification() {
        val success = pluginHelper.sendNotification(
            title = "URGENT: Server Down",
            message = "Production server is not responding. Immediate attention required.",
            priority = RemoteNotifyPluginHelper.Priority.HIGH
        )
        
        if (success) {
            Log.i(TAG, "Urgent notification sent successfully")
        } else {
            Log.e(TAG, "Failed to send urgent notification")
        }
    }
    
    /**
     * Example 3: Notification with preferred mediums
     */
    fun sendNotificationWithPreferences() {
        val success = pluginHelper.sendNotification(
            title = "Database Backup Complete",
            message = "Weekly database backup completed successfully at ${java.util.Date()}",
            priority = RemoteNotifyPluginHelper.Priority.NORMAL,
            preferredMediums = listOf("email", "telegram")
        )
        
        if (success) {
            Log.i(TAG, "Notification with preferences sent successfully")
        } else {
            Log.e(TAG, "Failed to send notification with preferences")
        }
    }
    
    /**
     * Example 4: Safe notification sending with error handling
     */
    fun sendNotificationSafely(title: String, message: String): Boolean {
        return try {
            // Check if plugin is ready
            if (!pluginHelper.isPluginReady()) {
                Log.w(TAG, "Plugin not ready, falling back to local notification")
                sendLocalNotificationFallback(title, message)
                return false
            }
            
            // Send through plugin
            val success = pluginHelper.sendNotification(title, message)
            
            if (!success) {
                Log.w(TAG, "Plugin failed, falling back to local notification")
                sendLocalNotificationFallback(title, message)
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification through plugin", e)
            sendLocalNotificationFallback(title, message)
            false
        }
    }
    
    /**
     * Example 5: Checking available mediums before sending
     */
    fun sendNotificationToSpecificMedium() {
        val mediums = pluginHelper.getAvailableMediums()
        val emailMedium = mediums.find { it.name == "email" }
        
        if (emailMedium?.isConfigured == true && emailMedium.isAvailable) {
            val success = pluginHelper.sendNotification(
                title = "Monthly Report",
                message = "Please find the attached monthly performance report.",
                priority = RemoteNotifyPluginHelper.Priority.NORMAL,
                preferredMediums = listOf("email")
            )
            
            if (success) {
                Log.i(TAG, "Email notification sent successfully")
            } else {
                Log.e(TAG, "Failed to send email notification")
            }
        } else {
            Log.w(TAG, "Email medium not available")
        }
    }
    
    /**
     * Example 6: Monitoring plugin status
     */
    fun monitorPluginHealth() {
        val status = pluginHelper.getPluginStatus()
        
        if (status != null) {
            Log.i(TAG, "Plugin Status:")
            Log.i(TAG, "  Service: ${status.serviceStatus}")
            Log.i(TAG, "  API Version: ${status.apiVersion}")
            Log.i(TAG, "  Configured Mediums: ${status.configuredMediumsCount}")
            Log.i(TAG, "  Notifications Today: ${status.notificationsSentToday}")
            Log.i(TAG, "  Uptime: ${status.uptime / 1000} seconds")
            
            // Alert if no mediums are configured
            if (status.configuredMediumsCount == 0) {
                Log.w(TAG, "No notification mediums configured in Remote Notify")
            }
            
            // Alert if service is not active
            if (status.serviceStatus != "active") {
                Log.w(TAG, "Plugin service is not active: ${status.serviceStatus}")
            }
        } else {
            Log.e(TAG, "Could not retrieve plugin status")
        }
    }
    
    /**
     * Example 7: Batch notifications with rate limiting
     */
    fun sendBatchNotifications(notifications: List<Pair<String, String>>) {
        Log.i(TAG, "Sending ${notifications.size} notifications...")
        
        notifications.forEachIndexed { index, (title, message) ->
            val success = sendNotificationSafely(title, message)
            
            Log.i(TAG, "Notification ${index + 1}/${notifications.size}: " +
                      "${if (success) "SUCCESS" else "FAILED"} - $title")
            
            // Add delay to avoid overwhelming the plugin
            if (index < notifications.size - 1) {
                Thread.sleep(1000) // 1 second delay
            }
        }
    }
    
    /**
     * Example 8: Application lifecycle integration
     */
    fun setupPluginMonitoring() {
        // Check plugin availability on app startup
        if (pluginHelper.isRemoteNotifyInstalled()) {
            Log.i(TAG, "Remote Notify app detected")
            
            if (pluginHelper.hasPermission()) {
                Log.i(TAG, "Plugin permission granted")
                
                if (pluginHelper.isPluginReady()) {
                    Log.i(TAG, "Plugin ready for notifications")
                    
                    // Send app startup notification
                    pluginHelper.sendNotification(
                        title = "App Started",
                        message = "Your app has started successfully",
                        priority = RemoteNotifyPluginHelper.Priority.LOW
                    )
                } else {
                    Log.w(TAG, "Plugin not ready - check Remote Notify configuration")
                }
            } else {
                Log.w(TAG, "Plugin permission not granted")
            }
        } else {
            Log.i(TAG, "Remote Notify app not installed")
        }
    }
    
    /**
     * Example 9: Error notifications for app monitoring
     */
    fun sendErrorNotification(error: Throwable, context: String) {
        val title = "App Error: $context"
        val message = """
            An error occurred in your application:
            
            Error: ${error.message}
            Type: ${error.javaClass.simpleName}
            Time: ${java.util.Date()}
            
            Please check the application logs for more details.
        """.trimIndent()
        
        pluginHelper.sendNotification(
            title = title,
            message = message,
            priority = RemoteNotifyPluginHelper.Priority.HIGH
        )
    }
    
    /**
     * Example 10: Performance monitoring notifications
     */
    fun sendPerformanceAlert(metric: String, value: Double, threshold: Double) {
        if (value > threshold) {
            val title = "Performance Alert: $metric"
            val message = """
                Performance threshold exceeded:
                
                Metric: $metric
                Current Value: $value
                Threshold: $threshold
                Time: ${java.util.Date()}
                
                Consider investigating the cause.
            """.trimIndent()
            
            pluginHelper.sendNotification(
                title = title,
                message = message,
                priority = RemoteNotifyPluginHelper.Priority.HIGH,
                preferredMediums = listOf("email", "telegram")
            )
        }
    }
    
    /**
     * Fallback method for local notifications when plugin is unavailable
     */
    private fun sendLocalNotificationFallback(title: String, message: String) {
        // Implement your local notification logic here
        Log.i(TAG, "Local notification fallback: $title - $message")
        
        // Example: Use Android's NotificationManager
        // val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // ... create and show local notification
    }
}