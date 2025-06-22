package dev.hossain.remotenotify.plugin

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import dev.hossain.remotenotify.RemoteAlertApp
import dev.hossain.remotenotify.di.AppComponent
import dev.hossain.remotenotify.model.RemoteAlert
import dev.hossain.remotenotify.model.RemoteAlert.PluginAlert
import dev.hossain.remotenotify.notifier.NotificationSender
import dev.hossain.remotenotify.notifier.NotifierType
import dev.hossain.remotenotify.plugin.model.PluginMediumConfig
import dev.hossain.remotenotify.plugin.model.PluginNotificationRequest
import dev.hossain.remotenotify.plugin.model.PluginNotificationResponse
import dev.hossain.remotenotify.plugin.model.PluginServiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ContentProvider that exposes the notification functionality to external apps.
 * This follows the plugin architecture pattern similar to Muzei.
 * 
 * External apps can use this provider to:
 * 1. Send notification requests through configured mediums
 * 2. Query available notification mediums and their status
 * 3. Check the overall plugin service status
 * 
 * Security: Requires the PLUGIN_ACCESS permission and validates calling packages.
 */
class PluginProvider : ContentProvider() {
    
    @Inject
    lateinit var notificationSenders: Set<@JvmSuppressWildcards NotificationSender>
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private val serviceStartTime = System.currentTimeMillis()
    private var notificationsSentToday = 0
    private var lastNotificationTimestamp = 0L
    
    companion object {
        private const val NOTIFICATIONS = 1
        private const val CONFIG = 2
        private const val STATUS = 3
        
        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(PluginContract.AUTHORITY, PluginContract.PATH_NOTIFICATIONS, NOTIFICATIONS)
            addURI(PluginContract.AUTHORITY, PluginContract.PATH_CONFIG, CONFIG)
            addURI(PluginContract.AUTHORITY, PluginContract.PATH_STATUS, STATUS)
        }
    }
    
    override fun onCreate(): Boolean {
        val context = context ?: return false
        
        // Initialize dependency injection
        val app = context.applicationContext as RemoteAlertApp
        app.appComponent.inject(this)
        
        Timber.d("PluginProvider initialized with ${notificationSenders.size} notification senders")
        return true
    }
    
    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            NOTIFICATIONS -> PluginContract.CONTENT_TYPE_NOTIFICATION
            CONFIG -> PluginContract.CONTENT_TYPE_CONFIG
            STATUS -> PluginContract.CONTENT_TYPE_STATUS
            else -> null
        }
    }
    
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        // Check permission
        if (!hasPermission()) {
            Timber.w("Plugin access denied for package: ${getCallingPackage()}")
            return null
        }
        
        return when (uriMatcher.match(uri)) {
            CONFIG -> queryConfig()
            STATUS -> queryStatus()
            else -> null
        }
    }
    
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        // Check permission
        if (!hasPermission()) {
            Timber.w("Plugin access denied for package: ${getCallingPackage()}")
            return null
        }
        
        return when (uriMatcher.match(uri)) {
            NOTIFICATIONS -> insertNotification(values)
            else -> null
        }
    }
    
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        // Not supported - notifications are fire-and-forget
        return 0
    }
    
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        // Not supported - notifications are immutable
        return 0
    }
    
    /**
     * Handles notification insertion requests from external apps.
     */
    private fun insertNotification(values: ContentValues?): Uri? {
        if (values == null) {
            Timber.w("Null values provided for notification request")
            return null
        }
        
        val title = values.getAsString(PluginContract.NotificationColumns.TITLE)
        val message = values.getAsString(PluginContract.NotificationColumns.MESSAGE)
        
        if (title.isNullOrBlank() || message.isNullOrBlank()) {
            Timber.w("Title and message are required for notification requests")
            return null
        }
        
        val priority = values.getAsString(PluginContract.NotificationColumns.PRIORITY) 
            ?: PluginContract.Priority.NORMAL
        val preferredMediums = values.getAsString(PluginContract.NotificationColumns.PREFERRED_MEDIUMS)
            ?.split(",")?.map { it.trim() }
        
        val requestId = UUID.randomUUID().toString()
        val callingPackage = getCallingPackage() ?: "unknown"
        val appName = getCallingAppName(callingPackage)
        
        val request = PluginNotificationRequest(
            title = title,
            message = message,
            sourcePackage = callingPackage,
            sourceAppName = appName,
            priority = priority,
            timestamp = System.currentTimeMillis(),
            requestId = requestId,
            preferredMediums = preferredMediums
        )
        
        // Process notification asynchronously
        coroutineScope.launch {
            processNotificationRequest(request)
        }
        
        Timber.d("Notification request received from $callingPackage: $title")
        
        // Return URI with request ID for tracking
        return PluginContract.NOTIFICATIONS_URI.buildUpon()
            .appendPath(requestId)
            .build()
    }
    
    /**
     * Processes a notification request by converting it to a RemoteAlert and sending
     * through the configured notification mediums.
     */
    private suspend fun processNotificationRequest(request: PluginNotificationRequest) {
        try {
            // Convert plugin request to internal RemoteAlert format
            val remoteAlert = createPluginAlert(request)
            
            // Determine which senders to use
            val sendersToUse = if (request.preferredMediums != null) {
                getSendersForMediums(request.preferredMediums)
            } else {
                getConfiguredSenders()
            }
            
            if (sendersToUse.isEmpty()) {
                Timber.w("No configured notification senders available for request: ${request.requestId}")
                return
            }
            
            var successCount = 0
            val usedMediums = mutableListOf<String>()
            val failedMediums = mutableListOf<String>()
            
            // Send through each configured medium
            for (sender in sendersToUse) {
                try {
                    if (sender.hasValidConfig()) {
                        val success = sender.sendNotification(remoteAlert)
                        if (success) {
                            successCount++
                            usedMediums.add(sender.notifierType.name.lowercase())
                            Timber.d("Notification sent successfully via ${sender.notifierType.displayName}")
                        } else {
                            failedMediums.add(sender.notifierType.name.lowercase())
                            Timber.w("Failed to send notification via ${sender.notifierType.displayName}")
                        }
                    } else {
                        Timber.w("Sender ${sender.notifierType.displayName} is not properly configured")
                        failedMediums.add(sender.notifierType.name.lowercase())
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error sending notification via ${sender.notifierType.displayName}")
                    failedMediums.add(sender.notifierType.name.lowercase())
                }
            }
            
            // Update statistics
            if (successCount > 0) {
                notificationsSentToday++
                lastNotificationTimestamp = System.currentTimeMillis()
            }
            
            Timber.i("Plugin notification processed: ${request.requestId}, " +
                    "sent via $successCount/${sendersToUse.size} mediums")
                    
        } catch (e: Exception) {
            Timber.e(e, "Error processing plugin notification request: ${request.requestId}")
        }
    }
    
    /**
     * Creates a RemoteAlert from a PluginNotificationRequest.
     * Since plugin requests are generic, we create a custom alert type.
     */
    private fun createPluginAlert(request: PluginNotificationRequest): RemoteAlert {
        // Create a PluginAlert that contains the plugin request information
        return PluginAlert(
            alertId = 0L,
            title = request.title,
            message = request.message,
            sourcePackage = request.sourcePackage,
            sourceAppName = request.sourceAppName,
            priority = request.priority,
            requestId = request.requestId
        )
    }
    
    /**
     * Gets notification senders for specific medium names.
     */
    private fun getSendersForMediums(mediumNames: List<String>): List<NotificationSender> {
        return notificationSenders.filter { sender ->
            mediumNames.contains(sender.notifierType.name.lowercase())
        }
    }
    
    /**
     * Gets all properly configured notification senders.
     */
    private suspend fun getConfiguredSenders(): List<NotificationSender> {
        return notificationSenders.filter { sender ->
            try {
                sender.hasValidConfig()
            } catch (e: Exception) {
                Timber.w(e, "Error checking config for ${sender.notifierType.displayName}")
                false
            }
        }
    }
    
    /**
     * Queries the configuration status of all notification mediums.
     */
    private fun queryConfig(): Cursor {
        val cursor = MatrixCursor(arrayOf(
            PluginContract.ConfigColumns.MEDIUM_NAME,
            PluginContract.ConfigColumns.MEDIUM_DISPLAY_NAME,
            PluginContract.ConfigColumns.IS_CONFIGURED,
            PluginContract.ConfigColumns.IS_AVAILABLE,
            PluginContract.ConfigColumns.CONFIG_DETAILS
        ))
        
        // Query each notification sender for its configuration status
        for (sender in notificationSenders) {
            val isConfigured = runBlocking { 
                try {
                    sender.hasValidConfig()
                } catch (e: Exception) {
                    false
                }
            }
            
            cursor.addRow(arrayOf(
                sender.notifierType.name.lowercase(),
                sender.notifierType.displayName,
                if (isConfigured) 1 else 0, // SQLite boolean as integer
                if (isConfigured) 1 else 0, // Available if configured
                "{}" // Empty JSON for now
            ))
        }
        
        return cursor
    }
    
    /**
     * Queries the overall plugin service status.
     */
    private fun queryStatus(): Cursor {
        val cursor = MatrixCursor(arrayOf(
            PluginContract.StatusColumns.SERVICE_STATUS,
            PluginContract.StatusColumns.API_VERSION,
            PluginContract.StatusColumns.CONFIGURED_MEDIUMS_COUNT,
            PluginContract.StatusColumns.LAST_NOTIFICATION_TIMESTAMP,
            PluginContract.StatusColumns.NOTIFICATIONS_SENT_TODAY,
            PluginContract.StatusColumns.UPTIME
        ))
        
        val configuredCount = runBlocking {
            notificationSenders.count { sender ->
                try {
                    sender.hasValidConfig()
                } catch (e: Exception) {
                    false
                }
            }
        }
        
        cursor.addRow(arrayOf(
            PluginContract.ServiceStatus.ACTIVE,
            PluginContract.API_VERSION,
            configuredCount,
            lastNotificationTimestamp,
            notificationsSentToday,
            System.currentTimeMillis() - serviceStartTime
        ))
        
        return cursor
    }
    
    /**
     * Checks if the calling app has the required permission.
     */
    private fun hasPermission(): Boolean {
        val context = context ?: return false
        return context.checkCallingPermission(PluginContract.PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Gets the human-readable name of the calling app.
     */
    private fun getCallingAppName(packageName: String): String {
        val context = context ?: return packageName
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}