package dev.hossain.remotenotify.notifier

import com.google.common.truth.Truth.assertThat
import dev.hossain.remotenotify.data.AlertFormatter
import dev.hossain.remotenotify.data.EmailConfigDataStore
import dev.hossain.remotenotify.data.EmailQuotaManager
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.AlertMode
import dev.hossain.remotenotify.model.RemoteAlert
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MailgunEmailNotificationSenderTest {
    private val emailConfigDataStore = mockk<EmailConfigDataStore>()
    private val emailQuotaManager = mockk<EmailQuotaManager>()
    private val okHttpClient = mockk<OkHttpClient>()
    private val alertFormatter = AlertFormatter()

    private lateinit var sender: MailgunEmailNotificationSender

    @Before
    fun setUp() {
        sender =
            MailgunEmailNotificationSender(
                emailConfigDataStore = emailConfigDataStore,
                emailQuotaManager = emailQuotaManager,
                okHttpClient = okHttpClient,
                alertFormatter = alertFormatter,
            )
    }

    @Test
    fun `sendNotification skips periodic email alerts without network call`() =
        runTest {
            val alert =
                RemoteAlert.BatteryAlert(
                    batteryPercentage = 20,
                    currentBatteryLevel = 80,
                    alertMode = AlertMode.PERIODIC,
                )

            val result = sender.sendNotification(alert)

            assertThat(result).isTrue()
            coVerify(exactly = 0) { emailQuotaManager.canSendEmail() }
            coVerify(exactly = 0) { emailConfigDataStore.getConfig() }
        }

    @Test
    fun `sendNotification returns false when daily quota is exhausted`() =
        runTest {
            val alert = RemoteAlert.BatteryAlert(batteryPercentage = 20, currentBatteryLevel = 10)
            coEvery { emailQuotaManager.canSendEmail() } returns false

            val result = sender.sendNotification(alert)

            assertThat(result).isFalse()
            coVerify(exactly = 0) { emailConfigDataStore.getConfig() }
        }

    @Test
    fun `sendNotification builds expected Mailgun request and records quota on success`() =
        runTest {
            val alert = RemoteAlert.BatteryAlert(batteryPercentage = 20, currentBatteryLevel = 10)
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = "api-key",
                    domain = "mg.example.com",
                    fromEmail = "alerts@example.com",
                    toEmail = "user@example.com",
                )
            val requestSlot = slot<Request>()
            val call = mockk<Call>()

            coEvery { emailQuotaManager.canSendEmail() } returns true
            coEvery { emailConfigDataStore.getConfig() } returns config
            every { okHttpClient.newCall(capture(requestSlot)) } returns call
            every { call.execute() } returns successfulResponse(request = Request.Builder().url("https://placeholder.invalid").build())
            coEvery { emailQuotaManager.recordEmailSent() } returns Unit

            val result = sender.sendNotification(alert)

            assertThat(result).isTrue()
            val request = requestSlot.captured
            assertThat(request.url.toString()).isEqualTo("https://api.mailgun.net/v3/mg.example.com/messages")
            assertThat(request.header("Authorization")).startsWith("Basic ")
            val formBody = request.body as FormBody
            val formEntries = (0 until formBody.size).associate { formBody.name(it) to formBody.value(it) }
            assertThat(formEntries["from"]).isEqualTo("alerts@example.com")
            assertThat(formEntries["to"]).isEqualTo("user@example.com")
            assertThat(formEntries["subject"]).isEqualTo("Remote Notify Alert: Battery")
            assertThat(formEntries["html"]).contains("Current: 10%")
            coVerify { emailQuotaManager.recordEmailSent() }
        }

    @Test
    fun `sendNotification returns false and does not record quota on unsuccessful response`() =
        runTest {
            val alert = RemoteAlert.StorageAlert(storageMinSpaceGb = 5, currentStorageGb = 2.0)
            val config =
                AlertMediumConfig.EmailConfig(
                    apiKey = "api-key",
                    domain = "mg.example.com",
                    fromEmail = "alerts@example.com",
                    toEmail = "user@example.com",
                )
            val call = mockk<Call>()

            coEvery { emailQuotaManager.canSendEmail() } returns true
            coEvery { emailConfigDataStore.getConfig() } returns config
            every { okHttpClient.newCall(any()) } returns call
            every {
                call.execute()
            } returns
                Response
                    .Builder()
                    .request(Request.Builder().url("https://placeholder.invalid").build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Server Error")
                    .build()

            val result = sender.sendNotification(alert)

            assertThat(result).isFalse()
            coVerify(exactly = 0) { emailQuotaManager.recordEmailSent() }
        }

    private fun successfulResponse(request: Request): Response =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()
}
