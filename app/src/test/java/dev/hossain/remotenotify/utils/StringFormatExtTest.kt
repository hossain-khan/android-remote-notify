package dev.hossain.remotenotify.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
