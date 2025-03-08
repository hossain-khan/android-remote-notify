package dev.hossain.remotenotify.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
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
}
