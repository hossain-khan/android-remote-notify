package dev.hossain.remotenotify.utils

import java.util.Locale

internal fun String.toTitleCase(): String =
    lowercase().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
    }

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
internal fun formatTimeDuration(timestamp: Long): String {
    val now = System.currentTimeMillis()
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
