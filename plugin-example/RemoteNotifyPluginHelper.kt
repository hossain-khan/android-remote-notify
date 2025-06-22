package dev.hossain.remotenotify.plugin.example

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.util.Log

/**
 * Example helper class demonstrating how to integrate with the Remote Notify plugin.
 * 
 * This class provides a simple API for external apps to send notifications
 * through Remote Notify's configured notification mediums.
 */
class RemoteNotifyPluginHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "RemoteNotifyPlugin"
        private const val AUTHORITY = "dev.hossain.remotenotify.plugin"
        private const val PERMISSION = "dev.hossain.remotenotify.plugin.ACCESS"
        private const val REMOTE_NOTIFY_PACKAGE = "dev.hossain.remotenotify"
        
        val NOTIFICATIONS_URI: Uri = Uri.parse("content://$AUTHORITY/notifications")
        val CONFIG_URI: Uri = Uri.parse("content://$AUTHORITY/config")
        val STATUS_URI: Uri = Uri.parse("content://$AUTHORITY/status")
    }
    
    /**
     * Checks if the Remote Notify app is installed on the device.
     */
    fun isRemoteNotifyInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(REMOTE_NOTIFY_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Remote Notify app not installed")
            false
        }
    }
    
    /**
     * Checks if the app has permission to access the plugin.
     */
    fun hasPermission(): Boolean {
        val result = context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED
        if (!result) {
            Log.w(TAG, "Plugin permission not granted")
        }
        return result
    }
    
    /**
     * Checks if the plugin is ready to send notifications.
     */
    fun isPluginReady(): Boolean {
        if (!isRemoteNotifyInstalled()) return false
        if (!hasPermission()) return false
        
        val mediums = getAvailableMediums()
        val hasConfiguredMedium = mediums.any { it.isConfigured && it.isAvailable }
        
        if (!hasConfiguredMedium) {
            Log.w(TAG, "No configured notification mediums available")
        }
        
        return hasConfiguredMedium
    }
    
    /**
     * Sends a basic notification with title and message.
     * 
     * @param title The notification title
     * @param message The notification message
     * @return true if the notification was submitted successfully
     */
    fun sendNotification(title: String, message: String): Boolean {
        return sendNotification(title, message, Priority.NORMAL)
    }
    
    /**
     * Sends a notification with specified priority.
     * 
     * @param title The notification title
     * @param message The notification message
     * @param priority The notification priority (LOW, NORMAL, HIGH)
     * @return true if the notification was submitted successfully
     */
    fun sendNotification(title: String, message: String, priority: Priority): Boolean {
        if (!isPluginReady()) {
            Log.e(TAG, "Plugin not ready to send notifications")
            return false
        }
        
        val values = ContentValues().apply {
            put("title", title)
            put("message", message)
            put("priority", priority.value)
        }
        
        return try {
            val resultUri = context.contentResolver.insert(NOTIFICATIONS_URI, values)
            val success = resultUri != null
            
            if (success) {
                Log.i(TAG, "Notification sent successfully: $title")
            } else {
                Log.e(TAG, "Failed to send notification: $title")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            false
        }
    }
    
    /**
     * Sends a notification with preferred mediums.
     * 
     * @param title The notification title
     * @param message The notification message
     * @param priority The notification priority
     * @param preferredMediums List of preferred notification mediums
     * @return true if the notification was submitted successfully
     */
    fun sendNotification(
        title: String, 
        message: String, 
        priority: Priority,
        preferredMediums: List<String>
    ): Boolean {
        if (!isPluginReady()) {
            Log.e(TAG, "Plugin not ready to send notifications")
            return false
        }
        
        val values = ContentValues().apply {
            put("title", title)
            put("message", message)
            put("priority", priority.value)
            put("preferred_mediums", preferredMediums.joinToString(","))
        }
        
        return try {
            val resultUri = context.contentResolver.insert(NOTIFICATIONS_URI, values)
            val success = resultUri != null
            
            if (success) {
                Log.i(TAG, "Notification sent successfully via preferred mediums: $title")
            } else {
                Log.e(TAG, "Failed to send notification: $title")
            }
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error sending notification", e)
            false
        }
    }
    
    /**
     * Gets the list of available notification mediums and their status.
     * 
     * @return List of available mediums
     */
    fun getAvailableMediums(): List<MediumInfo> {
        if (!hasPermission()) return emptyList()
        
        val mediums = mutableListOf<MediumInfo>()
        
        try {
            val cursor = context.contentResolver.query(CONFIG_URI, null, null, null, null)
            cursor?.use {
                while (it.moveToNext()) {
                    val mediumName = it.getString(it.getColumnIndexOrThrow("medium_name"))
                    val displayName = it.getString(it.getColumnIndexOrThrow("medium_display_name"))
                    val isConfigured = it.getInt(it.getColumnIndexOrThrow("is_configured")) == 1
                    val isAvailable = it.getInt(it.getColumnIndexOrThrow("is_available")) == 1
                    
                    mediums.add(MediumInfo(mediumName, displayName, isConfigured, isAvailable))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying available mediums", e)
        }
        
        return mediums
    }
    
    /**
     * Gets the current plugin status and statistics.
     * 
     * @return Plugin status information or null if unavailable
     */
    fun getPluginStatus(): PluginStatus? {
        if (!hasPermission()) return null
        
        return try {
            val cursor = context.contentResolver.query(STATUS_URI, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    PluginStatus(
                        serviceStatus = it.getString(it.getColumnIndexOrThrow("service_status")),
                        apiVersion = it.getInt(it.getColumnIndexOrThrow("api_version")),
                        configuredMediumsCount = it.getInt(it.getColumnIndexOrThrow("configured_mediums_count")),
                        notificationsSentToday = it.getInt(it.getColumnIndexOrThrow("notifications_sent_today")),
                        uptime = it.getLong(it.getColumnIndexOrThrow("uptime"))
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying plugin status", e)
            null
        }
    }
    
    /**
     * Logs debug information about the plugin status.
     */
    fun debugPluginStatus() {
        Log.d(TAG, "=== Remote Notify Plugin Debug Info ===")
        Log.d(TAG, "App installed: ${isRemoteNotifyInstalled()}")
        Log.d(TAG, "Has permission: ${hasPermission()}")
        Log.d(TAG, "Plugin ready: ${isPluginReady()}")
        
        val status = getPluginStatus()
        if (status != null) {
            Log.d(TAG, "Service status: ${status.serviceStatus}")
            Log.d(TAG, "API version: ${status.apiVersion}")
            Log.d(TAG, "Configured mediums: ${status.configuredMediumsCount}")
            Log.d(TAG, "Notifications sent today: ${status.notificationsSentToday}")
        } else {
            Log.d(TAG, "Plugin status unavailable")
        }
        
        val mediums = getAvailableMediums()
        Log.d(TAG, "Available mediums:")
        mediums.forEach { medium ->
            Log.d(TAG, "  ${medium.displayName} (${medium.name}): " +
                      "configured=${medium.isConfigured}, available=${medium.isAvailable}")
        }
    }
    
    /**
     * Priority levels for notifications.
     */
    enum class Priority(val value: String) {
        LOW("low"),
        NORMAL("normal"),
        HIGH("high")
    }
    
    /**
     * Information about a notification medium.
     */
    data class MediumInfo(
        val name: String,
        val displayName: String,
        val isConfigured: Boolean,
        val isAvailable: Boolean
    )
    
    /**
     * Plugin status information.
     */
    data class PluginStatus(
        val serviceStatus: String,
        val apiVersion: Int,
        val configuredMediumsCount: Int,
        val notificationsSentToday: Int,
        val uptime: Long
    )
}