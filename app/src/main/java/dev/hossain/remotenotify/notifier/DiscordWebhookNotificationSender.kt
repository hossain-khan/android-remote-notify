package dev.hossain.remotenotify.notifier

import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.DiscordWebhookConfigDataStore
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.model.RemoteAlert
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Named
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@ContributesIntoSet(AppScope::class)
@Named("discord")
@Inject
class DiscordWebhookNotificationSender
    constructor(
        private val discordWebhookConfigDataStore: DiscordWebhookConfigDataStore,
        private val okHttpClient: OkHttpClient,
    ) : NotificationSender {
        override val notifierType: NotifierType = NotifierType.WEBHOOK_DISCORD

        companion object {
            // Discord embed color codes (decimal)
            private const val COLOR_BATTERY_CRITICAL = 16711680 // #FF0000 (red)
            private const val COLOR_STORAGE_CRITICAL = 16753920 // #FFA500 (orange)
        }

        override suspend fun sendNotification(remoteAlert: RemoteAlert): Boolean {
            val webhookUrl =
                requireNotNull(discordWebhookConfigDataStore.discordWebhookUrl.first()) {
                    "Discord Webhook URL is required. Check `hasValidConfiguration` before using the notifier."
                }

            val json = buildDiscordEmbed(remoteAlert)
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request =
                Request
                    .Builder()
                    .url(webhookUrl)
                    .post(body)
                    .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.e("Failed to send Discord webhook: ${response.code} - ${response.message}")
                    return false
                }
                Timber.d("Discord webhook sent successfully")
                return true
            }
        }

        private fun buildDiscordEmbed(remoteAlert: RemoteAlert): String {
            val timestamp = LocalDateTime.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT)
            val deviceName = "${android.os.Build.BRAND.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
            }} ${android.os.Build.MODEL}"

            return when (remoteAlert) {
                is RemoteAlert.BatteryAlert -> {
                    val currentLevel = remoteAlert.currentBatteryLevel ?: remoteAlert.batteryPercentage
                    val threshold = remoteAlert.batteryPercentage

                    buildJsonEmbed(
                        title = "🪫 Battery Alert",
                        description = "Device battery level is critically low",
                        color = COLOR_BATTERY_CRITICAL,
                        fields =
                            listOf(
                                Field("🔋 Current Level", "$currentLevel%"),
                                Field("⚠️ Alert Threshold", "$threshold%"),
                                Field("📱 Device", deviceName, inline = true),
                                Field("📍 Android", android.os.Build.VERSION.RELEASE, inline = true),
                            ),
                        footerText = "Remote Alert Notifier",
                        timestamp = timestamp,
                    )
                }
                is RemoteAlert.StorageAlert -> {
                    val currentStorage = remoteAlert.currentStorageGb ?: remoteAlert.storageMinSpaceGb.toDouble()
                    val threshold = remoteAlert.storageMinSpaceGb

                    buildJsonEmbed(
                        title = "💾 Storage Alert",
                        description = "Available storage space is critically low",
                        color = COLOR_STORAGE_CRITICAL,
                        fields =
                            listOf(
                                Field("💿 Available Space", String.format(Locale.US, "%.1f GB", currentStorage)),
                                Field("⚠️ Alert Threshold", "$threshold GB"),
                                Field("📱 Device", deviceName, inline = true),
                                Field("📍 Android", android.os.Build.VERSION.RELEASE, inline = true),
                            ),
                        footerText = "Remote Alert Notifier",
                        timestamp = timestamp,
                    )
                }
            }
        }

        private data class Field(
            val name: String,
            val value: String,
            val inline: Boolean = false,
        )

        private fun buildJsonEmbed(
            title: String,
            description: String,
            color: Int,
            fields: List<Field>,
            footerText: String,
            timestamp: String,
        ): String {
            val fieldsJson =
                fields.joinToString(",") { field ->
                    """{"name":"${escapeJson(field.name)}","value":"${escapeJson(field.value)}","inline":${field.inline}}"""
                }

            return buildString {
                append("{")
                append(""""embeds": [{""")
                append(""""title": "${escapeJson(title)}",""")
                append(""""description": "${escapeJson(description)}",""")
                append(""""color": $color,""")
                append(""""fields": [$fieldsJson],""")
                append(""""footer": {"text": "${escapeJson(footerText)}"},""")
                append(""""timestamp": "$timestamp"""")
                append("}]}")
            }
        }

        private fun escapeJson(text: String): String =
            text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")

        override suspend fun hasValidConfig(): Boolean = discordWebhookConfigDataStore.hasValidConfig()

        override suspend fun saveConfig(alertMediumConfig: AlertMediumConfig) {
            when (alertMediumConfig) {
                is AlertMediumConfig.DiscordConfig -> discordWebhookConfigDataStore.saveDiscordWebhookUrl(alertMediumConfig.webhookUrl)
                else -> throw IllegalArgumentException("Invalid configuration type: $alertMediumConfig")
            }
        }

        override suspend fun getConfig(): AlertMediumConfig = discordWebhookConfigDataStore.getConfig()

        override suspend fun clearConfig() {
            discordWebhookConfigDataStore.clearConfig()
        }

        override suspend fun validateConfig(alertMediumConfig: AlertMediumConfig): ConfigValidationResult =
            discordWebhookConfigDataStore.validateConfig(alertMediumConfig)
    }
