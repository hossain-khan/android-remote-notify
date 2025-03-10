package dev.hossain.remotenotify.utils

import java.util.Locale

/**
 * Converts the first character of the string to title case and the rest to lowercase.
 *
 * @return A string with the first character in title case and the rest in lowercase.
 */
internal fun String.toTitleCase(): String =
    lowercase().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
    }
