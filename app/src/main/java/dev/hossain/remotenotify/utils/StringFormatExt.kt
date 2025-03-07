package dev.hossain.remotenotify.utils

import java.util.Locale

internal fun String.toTitleCase(): String =
    lowercase().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
    }
