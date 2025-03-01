package dev.hossain.remotenotify.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit

class StringFormatExtTest {
    @Test
    fun `toTitleCase properly capitalizes strings`() {
        // Test lowercase to title case
        assertThat("hello".toTitleCase()).isEqualTo("Hello")

        // Test uppercase to title case
        assertThat("HELLO".toTitleCase()).isEqualTo("Hello")

        // Test mixed case to title case
        assertThat("hElLo".toTitleCase()).isEqualTo("Hello")

        // Test with spaces
        assertThat("hello world".toTitleCase()).isEqualTo("Hello world")

        // Test with empty string
        assertThat("".toTitleCase()).isEqualTo("")
    }

    @Test
    fun `formatTimeDuration handles just now time range`() {
        val now = System.currentTimeMillis()

        // Test almost current time (5 seconds ago)
        assertThat(formatTimeDuration(now - TimeUnit.SECONDS.toMillis(5))).isEqualTo("just now ago")

        // Test almost current time (30 seconds ago)
        assertThat(formatTimeDuration(now - TimeUnit.SECONDS.toMillis(30))).isEqualTo("just now ago")

        // Test almost current time (59 seconds ago)
        assertThat(formatTimeDuration(now - TimeUnit.SECONDS.toMillis(59))).isEqualTo("just now ago")
    }

    @Test
    fun `formatTimeDuration handles minutes properly`() {
        val now = System.currentTimeMillis()

        // Test 1 minute ago
        assertThat(formatTimeDuration(now - TimeUnit.MINUTES.toMillis(1))).isEqualTo("1 minute ago")

        // Test 5 minutes ago
        assertThat(formatTimeDuration(now - TimeUnit.MINUTES.toMillis(5))).isEqualTo("5 minutes ago")

        // Test 59 minutes ago
        assertThat(formatTimeDuration(now - TimeUnit.MINUTES.toMillis(59))).isEqualTo("59 minutes ago")
    }

    @Test
    fun `formatTimeDuration handles hours properly`() {
        val now = System.currentTimeMillis()

        // Test 1 hour ago
        assertThat(formatTimeDuration(now - TimeUnit.HOURS.toMillis(1))).isEqualTo("1 hour ago")

        // Test 2 hours ago
        assertThat(formatTimeDuration(now - TimeUnit.HOURS.toMillis(2))).isEqualTo("2 hours ago")

        // Test 2 hours 30 minutes ago
        val twoAndHalfHoursInMillis = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30)
        assertThat(formatTimeDuration(now - twoAndHalfHoursInMillis)).isEqualTo("2 hours 30 minutes ago")
    }

    @Test
    fun `formatTimeDuration handles days properly`() {
        val now = System.currentTimeMillis()

        // Test 1 day ago
        assertThat(formatTimeDuration(now - TimeUnit.DAYS.toMillis(1))).isEqualTo("1 day ago")

        // Test 3 days ago
        assertThat(formatTimeDuration(now - TimeUnit.DAYS.toMillis(3))).isEqualTo("3 days ago")

        // Test 3 days 2 hours ago
        val threeDaysTwoHoursInMillis = TimeUnit.DAYS.toMillis(3) + TimeUnit.HOURS.toMillis(2)
        assertThat(formatTimeDuration(now - threeDaysTwoHoursInMillis)).isEqualTo("3 days 2 hours ago")
    }

    @Test
    fun `formatTimeDuration handles future times`() {
        val now = System.currentTimeMillis()

        // Test in 5 minutes
        assertThat(formatTimeDuration(now + TimeUnit.MINUTES.toMillis(5))).isEqualTo("in 5 minutes")

        // Test in 2 hours
        assertThat(formatTimeDuration(now + TimeUnit.HOURS.toMillis(2))).isEqualTo("in 2 hours")

        // Test in 2 hours 30 minutes
        val twoAndHalfHoursInMillis = TimeUnit.HOURS.toMillis(2) + TimeUnit.MINUTES.toMillis(30)
        assertThat(formatTimeDuration(now + twoAndHalfHoursInMillis)).isEqualTo("in 2 hours 30 minutes")

        // Test in 3 days
        assertThat(formatTimeDuration(now + TimeUnit.DAYS.toMillis(3))).isEqualTo("in 3 days")
    }
}
