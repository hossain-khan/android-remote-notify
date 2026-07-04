package dev.hossain.remotenotify.notifier

import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.WebhookConfigDataStore
import dev.hossain.remotenotify.model.RemoteAlert
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebhookRequestSenderTest {
    private val webhookConfigDataStore = mockk<WebhookConfigDataStore>()
    private val okHttpClient = mockk<OkHttpClient>()
    private val alertFormatter = AlertFormatter()

    private lateinit var sender: WebhookRequestSender

    @Before
    fun setUp() {
        sender =
            WebhookRequestSender(
                webhookConfigDataStore = webhookConfigDataStore,
                okHttpClient = okHttpClient,
                alertFormatter = alertFormatter,
            )
    }

    @Test
    fun `sendNotification posts formatted json to configured webhook`() =
        runTest {
            val alert = RemoteAlert.BatteryAlert(batteryPercentage = 20, currentBatteryLevel = 10)
            val requestSlot = slot<Request>()
            val call = mockk<Call>()

            every { webhookConfigDataStore.webhookUrl } returns flowOf("https://example.com/webhook")
            every { okHttpClient.newCall(capture(requestSlot)) } returns call
            every { call.execute() } returns successfulResponse()

            val result = sender.sendNotification(alert)

            assertThat(result).isTrue()
            val request = requestSlot.captured
            assertThat(request.url.toString()).isEqualTo("https://example.com/webhook")
            assertThat(request.header("Content-Type")).isNull()

            val buffer = Buffer()
            request.body!!.writeTo(buffer)
            val payload = buffer.readUtf8()
            assertThat(payload).contains("\"alertType\":\"BATTERY\"")
            assertThat(payload).contains("\"batteryLevel\":10")
            assertThat(payload).contains("\"batteryThresholdPercent\":20")
        }

    @Test
    fun `sendNotification returns false for unsuccessful response`() =
        runTest {
            val alert = RemoteAlert.StorageAlert(storageMinSpaceGb = 5, currentStorageGb = 2.0)
            val call = mockk<Call>()

            every { webhookConfigDataStore.webhookUrl } returns flowOf("https://example.com/webhook")
            every { okHttpClient.newCall(any()) } returns call
            every {
                call.execute()
            } returns
                Response
                    .Builder()
                    .request(Request.Builder().url("https://example.com/webhook").build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Server Error")
                    .build()

            val result = sender.sendNotification(alert)

            assertThat(result).isFalse()
        }

    private fun successfulResponse(): Response =
        Response
            .Builder()
            .request(Request.Builder().url("https://example.com/webhook").build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()
}
