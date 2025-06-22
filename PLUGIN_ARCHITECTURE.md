# Plugin Architecture Documentation

The Remote Notify app supports a plugin architecture that allows external Android apps to leverage the notification mediums configured in Remote Notify. This enables other apps to send notifications through Email, Telegram, Twilio SMS, Slack, and REST Webhooks without implementing these integrations themselves.

## Overview

The plugin architecture uses Android's ContentProvider mechanism for cross-app communication. External apps can:

1. **Send Notifications**: Submit notification requests to be sent through configured mediums
2. **Query Configuration**: Check which notification mediums are available and configured
3. **Check Status**: Monitor the plugin service health and usage statistics

## Getting Started

### 1. Add Permission to Your App

Add the plugin access permission to your app's `AndroidManifest.xml`:

```xml
<uses-permission android:name="dev.hossain.remotenotify.plugin.ACCESS" />
```

### 2. Send a Basic Notification

```kotlin
import android.content.ContentValues
import android.content.Context
import android.net.Uri

class RemoteNotifyPlugin(private val context: Context) {
    
    companion object {
        const val AUTHORITY = "dev.hossain.remotenotify.plugin"
        val NOTIFICATIONS_URI = Uri.parse("content://$AUTHORITY/notifications")
    }
    
    fun sendNotification(title: String, message: String): Boolean {
        val values = ContentValues().apply {
            put("title", title)
            put("message", message)
            put("priority", "normal") // optional: low, normal, high
        }
        
        return try {
            val resultUri = context.contentResolver.insert(NOTIFICATIONS_URI, values)
            resultUri != null
        } catch (e: Exception) {
            false
        }
    }
}
```

### 3. Advanced Usage with Preferred Mediums

```kotlin
fun sendNotificationWithPreferences(
    title: String, 
    message: String, 
    preferredMediums: List<String>
): Boolean {
    val values = ContentValues().apply {
        put("title", title)
        put("message", message)
        put("priority", "high")
        put("preferred_mediums", preferredMediums.joinToString(","))
    }
    
    return try {
        val resultUri = context.contentResolver.insert(NOTIFICATIONS_URI, values)
        resultUri != null
    } catch (e: Exception) {
        false
    }
}

// Example usage
sendNotificationWithPreferences(
    title = "Server Alert",
    message = "Database connection failed",
    preferredMediums = listOf("email", "telegram")
)
```

## API Reference

### ContentProvider URIs

- **Authority**: `dev.hossain.remotenotify.plugin`
- **Notifications**: `content://dev.hossain.remotenotify.plugin/notifications`
- **Configuration**: `content://dev.hossain.remotenotify.plugin/config`
- **Status**: `content://dev.hossain.remotenotify.plugin/status`

### Notification Parameters

| Column | Type | Required | Description |
|--------|------|----------|-------------|
| `title` | String | Yes | The notification title/subject |
| `message` | String | Yes | The notification message body |
| `priority` | String | No | Priority level: "low", "normal", "high" (default: "normal") |
| `preferred_mediums` | String | No | Comma-separated list of preferred mediums |

### Available Notification Mediums

- `email` - Email notifications via Mailgun
- `telegram` - Telegram Bot notifications
- `twilio` - SMS notifications via Twilio
- `webhook_slack_workflow` - Slack webhook notifications
- `webhook_rest_api` - Generic REST webhook notifications

### Querying Configuration

```kotlin
fun getAvailableMediums(): List<MediumInfo> {
    val configUri = Uri.parse("content://$AUTHORITY/config")
    val cursor = context.contentResolver.query(configUri, null, null, null, null)
    
    val mediums = mutableListOf<MediumInfo>()
    cursor?.use {
        while (it.moveToNext()) {
            val mediumName = it.getString(it.getColumnIndexOrThrow("medium_name"))
            val displayName = it.getString(it.getColumnIndexOrThrow("medium_display_name"))
            val isConfigured = it.getInt(it.getColumnIndexOrThrow("is_configured")) == 1
            val isAvailable = it.getInt(it.getColumnIndexOrThrow("is_available")) == 1
            
            mediums.add(MediumInfo(mediumName, displayName, isConfigured, isAvailable))
        }
    }
    return mediums
}

data class MediumInfo(
    val name: String,
    val displayName: String,
    val isConfigured: Boolean,
    val isAvailable: Boolean
)
```

### Checking Plugin Status

```kotlin
fun getPluginStatus(): PluginStatus? {
    val statusUri = Uri.parse("content://$AUTHORITY/status")
    val cursor = context.contentResolver.query(statusUri, null, null, null, null)
    
    return cursor?.use {
        if (it.moveToFirst()) {
            PluginStatus(
                serviceStatus = it.getString(it.getColumnIndexOrThrow("service_status")),
                apiVersion = it.getInt(it.getColumnIndexOrThrow("api_version")),
                configuredMediumsCount = it.getInt(it.getColumnIndexOrThrow("configured_mediums_count")),
                notificationsSentToday = it.getInt(it.getColumnIndexOrThrow("notifications_sent_today"))
            )
        } else null
    }
}

data class PluginStatus(
    val serviceStatus: String,
    val apiVersion: Int,
    val configuredMediumsCount: Int,
    val notificationsSentToday: Int
)
```

## Best Practices

### 1. Check Permission Before Use

```kotlin
fun hasPermission(): Boolean {
    return context.checkSelfPermission("dev.hossain.remotenotify.plugin.ACCESS") == 
           PackageManager.PERMISSION_GRANTED
}
```

### 2. Validate Remote Notify Installation

```kotlin
fun isRemoteNotifyInstalled(): Boolean {
    return try {
        context.packageManager.getPackageInfo("dev.hossain.remotenotify", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
```

### 3. Check Available Mediums Before Sending

```kotlin
fun canSendNotifications(): Boolean {
    val mediums = getAvailableMediums()
    return mediums.any { it.isConfigured && it.isAvailable }
}
```

### 4. Use Appropriate Priority Levels

- **Low**: Non-urgent information, logs, status updates
- **Normal**: Standard notifications, alerts (default)
- **High**: Critical alerts, errors, urgent notifications

### 5. Handle Errors Gracefully

```kotlin
fun sendNotificationSafely(title: String, message: String): Result<String> {
    return try {
        if (!hasPermission()) {
            return Result.failure(Exception("Plugin permission not granted"))
        }
        
        if (!isRemoteNotifyInstalled()) {
            return Result.failure(Exception("Remote Notify app not installed"))
        }
        
        if (!canSendNotifications()) {
            return Result.failure(Exception("No notification mediums available"))
        }
        
        val success = sendNotification(title, message)
        if (success) {
            Result.success("Notification sent successfully")
        } else {
            Result.failure(Exception("Failed to send notification"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

## Example Integration

Here's a complete example of integrating the plugin into your app:

```kotlin
class NotificationService {
    private val context: Context
    
    fun sendAlert(title: String, message: String, priority: String = "normal") {
        // Check if Remote Notify is available
        if (!isRemoteNotifyAvailable()) {
            // Fallback to local notifications or other method
            sendLocalNotification(title, message)
            return
        }
        
        // Send through Remote Notify plugin
        val success = sendNotificationSafely(title, message, priority)
        if (!success) {
            // Fallback if plugin fails
            sendLocalNotification(title, message)
        }
    }
    
    private fun isRemoteNotifyAvailable(): Boolean {
        return isRemoteNotifyInstalled() && 
               hasPermission() && 
               canSendNotifications()
    }
    
    private fun sendLocalNotification(title: String, message: String) {
        // Your local notification implementation
    }
}
```

## Security Considerations

1. **Permission**: The plugin requires explicit permission from external apps
2. **Package Validation**: The plugin logs the source package of all requests
3. **Rate Limiting**: Consider implementing rate limiting in your app to avoid spam
4. **Content Filtering**: Validate notification content before sending

## Troubleshooting

### Common Issues

1. **Permission Denied**: Ensure the permission is declared in your manifest
2. **Provider Not Found**: Check that Remote Notify is installed and updated
3. **No Mediums Available**: Remote Notify needs to be configured with at least one notification medium
4. **Send Failures**: Check the plugin status and medium availability

### Debugging

Enable logging to troubleshoot issues:

```kotlin
fun debugPluginStatus() {
    Log.d("Plugin", "Has permission: ${hasPermission()}")
    Log.d("Plugin", "App installed: ${isRemoteNotifyInstalled()}")
    
    val status = getPluginStatus()
    Log.d("Plugin", "Service status: ${status?.serviceStatus}")
    Log.d("Plugin", "Configured mediums: ${status?.configuredMediumsCount}")
    
    val mediums = getAvailableMediums()
    mediums.forEach { medium ->
        Log.d("Plugin", "Medium ${medium.name}: configured=${medium.isConfigured}, available=${medium.isAvailable}")
    }
}
```

## Migration and Compatibility

The plugin API uses versioning to ensure compatibility:

- **API Version 1**: Initial release with basic notification support
- Future versions will maintain backward compatibility when possible

Check the API version in your integration:

```kotlin
fun checkApiCompatibility(): Boolean {
    val status = getPluginStatus()
    return status?.apiVersion == 1 // Your supported version
}
```

## Support

For plugin development support, issues, or feature requests:

1. Check the [GitHub repository](https://github.com/hossain-khan/android-remote-notify)
2. Review existing [issues and discussions](https://github.com/hossain-khan/android-remote-notify/issues)
3. Submit new issues with the "plugin" label