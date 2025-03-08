package dev.hossain.remotenotify.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DateTimeFormatterTest {
    @Test
    fun `formatTimeDuration handles just now time range`() {
        val now = System.currentTimeMillis()

        // Test almost current time (5 seconds ago)
        assertThat(formatTimeElapsed(now - TimeUnit.SECONDS.toMillis(5))).isEqualTo("just now ago")

        // Test almost current time (30 seconds ago)
        assertThat(formatTimeElapsed(now - TimeUnit.SECONDS.toMillis(30))).isEqualTo("just now ago")

        // Test almost current time (59 seconds ago)
        assertThat(formatTimeElapsed(now - TimeUnit.SECONDS.toMillis(59))).isEqualTo("just now ago")
    }

    @Test
    fun `formatTimeDuration handles minutes properly`() {
        val now = System.currentTimeMillis()

        // Test 1 minute ago
        assertThat(formatTimeElapsed(now - TimeUnit.MINUTES.toMillis(1))).isEqualTo("1 minute ago")

        // Test 5 minutes ago
        assertThat(formatTimeElapsed(now - TimeUnit.MINUTES.toMillis(5))).isEqualTo("5 minutes ago")

        // Test 59 minutes ago
        assertThat(formatTimeElapsed(now - TimeUnit.MINUTES.toMillis(59))).isEqualTo("59 minutes ago")
    }

    @Test
    fun `formatTimeDuration handles hours properly`() {
        val now = System.currentTimeMillis()

        // Test 1 hour ago
        assertThat(formatTimeElapsed(now - TimeUnit.HOURS.toMillis(1))).isEqualTo("1 hour ago")

        // Test 2 hours ago
        assertThat(formatTimeElapsed(now - TimeUnit.HOURS.toMillis(2))).isEqualTo("2 hours ago")

        // Test 2 hours 30 minutes ago
        val twoAndHalfHoursInMillis = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30)
        assertThat(formatTimeElapsed(now - twoAndHalfHoursInMillis)).isEqualTo("2 hours 30 minutes ago")
    }

    @Test
    fun `formatTimeDuration handles days properly`() {
        val now = System.currentTimeMillis()

        // Test 1 day ago
        assertThat(formatTimeElapsed(now - TimeUnit.DAYS.toMillis(1))).isEqualTo("1 day ago")

        // Test 3 days ago
        assertThat(formatTimeElapsed(now - TimeUnit.DAYS.toMillis(3))).isEqualTo("3 days ago")

        // Test 3 days 2 hours ago
        val threeDaysTwoHoursInMillis = TimeUnit.DAYS.toMillis(3) + TimeUnit.HOURS.toMillis(2)
        assertThat(formatTimeElapsed(now - threeDaysTwoHoursInMillis)).isEqualTo("3 days 2 hours ago")
    }

    @Test
    fun `formatTimeDuration handles future times`() {
        // Use a fixed timestamp instead of current time
        val baseTime = 1704067200000L // 2024-01-01 00:00:00 UTC

        // Test in 5 minutes
        assertThat(formatTimeElapsed(baseTime + TimeUnit.MINUTES.toMillis(5), baseTime))
            .isEqualTo("in 5 minutes")

        // Test in 2 hours
        assertThat(formatTimeElapsed(baseTime + TimeUnit.HOURS.toMillis(2), baseTime))
            .isEqualTo("in 2 hours")

        // Test in 2 hours 30 minutes
        val twoAndHalfHoursInMillis = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30)
        assertThat(formatTimeElapsed(baseTime + twoAndHalfHoursInMillis, baseTime))
            .isEqualTo("in 2 hours 30 minutes")

        // Test in 3 days
        assertThat(formatTimeElapsed(baseTime + TimeUnit.DAYS.toMillis(3), baseTime))
            .isEqualTo("in 3 days")
    }

    @Test
    fun `formatDuration handles minutes properly`() {
        assertThat(formatDuration(1)).isEqualTo("1 minute")
        assertThat(formatDuration(5)).isEqualTo("5 minutes")
        assertThat(formatDuration(59)).isEqualTo("59 minutes")
    }

    @Test
    fun `formatDuration handles hours properly`() {
        assertThat(formatDuration(60)).isEqualTo("1 hour")
        assertThat(formatDuration(120)).isEqualTo("2 hours")
        assertThat(formatDuration(180)).isEqualTo("3 hours")
    }

    @Test
    fun `formatDuration handles hours and minutes properly`() {
        assertThat(formatDuration(61)).isEqualTo("1 hour and 1 minute")
        assertThat(formatDuration(122)).isEqualTo("2 hours and 2 minutes")
        assertThat(formatDuration(150)).isEqualTo("2 hours and 30 minutes")
    }

    @Test
    fun `formatDateTime formats timestamps correctly`() {
        // Override the locale in the test to ensure consistent formatting
        val testLocale = Locale.US

        // January 1, 2023 at 10:30am
        val calendar1 =
            Calendar.getInstance().apply {
                set(2023, Calendar.JANUARY, 1, 10, 30, 0)
                set(Calendar.MILLISECOND, 0)
            }

        // Use the same format pattern as in the implementation but with our controlled locale
        val formatter = SimpleDateFormat("EEE, d MMM yyyy h:mm a", testLocale)
        val expectedFormat1 = formatter.format(Date(calendar1.timeInMillis))

        // Test by calling the actual function and comparing with our controlled formatting
        assertThat(formatDateTime(calendar1.timeInMillis, testLocale)).isEqualTo(expectedFormat1)

        // December 31, 2023 at 11:59pm
        val calendar2 =
            Calendar.getInstance().apply {
                set(2023, Calendar.DECEMBER, 31, 23, 59, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val expectedFormat2 = formatter.format(Date(calendar2.timeInMillis))
        assertThat(formatDateTime(calendar2.timeInMillis, testLocale)).isEqualTo(expectedFormat2)

        // February 29, 2024 (leap year) at 1:05pm
        val calendar3 =
            Calendar.getInstance().apply {
                set(2024, Calendar.FEBRUARY, 29, 13, 5, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val expectedFormat3 = formatter.format(Date(calendar3.timeInMillis))
        assertThat(formatDateTime(calendar3.timeInMillis, testLocale)).isEqualTo(expectedFormat3)
    }
}
