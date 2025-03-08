package dev.hossain.remotenotify.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats timestamp to human readable time in past or future.
 *
 * Examples:
 * - Past: "2 hours 30 minutes ago"
 * - Future: "in 2 hours 30 minutes"
 * - Now: "just now"
 * - Past: "5 days 2 hours ago"
 * - Future: "in 5 days 2 hours"
 */
internal fun formatTimeElapsed(
    timestamp: Long,
    now: Long = System.currentTimeMillis(),
): String {
    val diff = timestamp - now
    val absoluteDiff = kotlin.math.abs(diff)

    // Function to handle pluralization
    fun pluralize(
        count: Long,
        unit: String,
    ) = "$count $unit${if (count > 1) "s" else ""}"

    // Build time components
    fun buildTimeString(
        primary: Pair<Long, String>,
        secondary: Pair<Long, String>? = null,
    ): String =
        buildString {
            append(pluralize(primary.first, primary.second))
            secondary?.let { (value, unit) ->
                if (value > 0) {
                    append(" ${pluralize(value, unit)}")
                }
            }
        }

    val timeString =
        when {
            absoluteDiff < 60_000 -> "just now"
            absoluteDiff < 3600_000 -> {
                val minutes = absoluteDiff / 60_000
                pluralize(minutes, "minute")
            }
            absoluteDiff < 86400_000 -> {
                val hours = absoluteDiff / 3600_000
                val minutes = (absoluteDiff % 3600_000) / 60_000
                buildTimeString(hours to "hour", minutes to "minute")
            }
            absoluteDiff < 2592000000 -> { // 30 days
                val days = absoluteDiff / 86400_000
                val hours = (absoluteDiff % 86400_000) / 3600_000
                buildTimeString(days to "day", hours to "hour")
            }
            else -> {
                val days = absoluteDiff / 86400_000
                pluralize(days, "day")
            }
        }

    return when {
        diff > 0 -> "in $timeString" // Future
        diff < 0 -> "$timeString ago" // Past
        else -> timeString // Now
    }
}

/**
 * Formats duration in minutes to human readable format.
 */
internal fun formatDuration(minutes: Int): String =
    when {
        minutes < 60 -> "$minutes ${if (minutes == 1) "minute" else "minutes"}"
        minutes % 60 == 0 -> {
            val hours = minutes / 60
            "$hours ${if (hours == 1) "hour" else "hours"}"
        }
        else -> {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            "$hours ${if (hours == 1) "hour" else "hours"} and " +
                "$remainingMinutes ${if (remainingMinutes == 1) "minute" else "minutes"}"
        }
    }

internal fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy h:mm a", Locale.getDefault())
        .apply { isLenient = true }
        .format(Date(timestamp))
