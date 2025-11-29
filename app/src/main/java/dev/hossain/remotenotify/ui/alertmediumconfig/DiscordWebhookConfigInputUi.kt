package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.DiscordWebhookConfigDataStore.Companion.ValidationKeys
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun DiscordWebhookConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = alertMediumConfig as AlertMediumConfig.DiscordConfig?
    val errors = configValidationResult.errors
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        Text(
            text =
                buildAnnotatedString {
                    append("Use a Discord webhook to send rich embed notifications to your Discord server. ")
                    append("When an alert is triggered, a POST request will be sent to your Discord webhook URL with color-coded embeds. ")
                    @Suppress("ktlint:standard:max-line-length")
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks",
                    )
                    withStyle(
                        style =
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                    ) {
                        append("Learn how to create a Discord webhook")
                    }
                },
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.clickable {
                    uriHandler.openUri(
                        "https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks",
                    )
                },
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Add info card about Discord webhook features
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Information",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Discord webhooks are free and support rich embeds with colors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.webhookUrl ?: "",
            onValueChange = {
                onConfigUpdate(AlertMediumConfig.DiscordConfig(webhookUrl = it))
            },
            label = { Text("Discord Webhook URL") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    },
                ),
            isError = shouldShowValidationError && errors[ValidationKeys.URL] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.URL] != null) {
                    Text(errors[ValidationKeys.URL]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Column {
                        Text("Enter your Discord webhook URL (Server Settings → Integrations → Webhooks)")
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewDiscordWebhookConfigInputUi() {
    ComposeAppTheme {
        DiscordWebhookConfigInputUi(
            alertMediumConfig =
                AlertMediumConfig.DiscordConfig(
                    webhookUrl = "https://discord.com/api/webhooks/1234567890/ABCDEFGHIJKLMNOP",
                ),
            configValidationResult =
                ConfigValidationResult(
                    isValid = false,
                    errors = mapOf(ValidationKeys.URL to "Invalid Discord webhook URL format"),
                ),
            shouldShowValidationError = true,
            onConfigUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewDiscordWebhookConfigInputUiEmpty() {
    ComposeAppTheme {
        DiscordWebhookConfigInputUi(
            alertMediumConfig = null,
            configValidationResult = ConfigValidationResult(true, emptyMap()),
            shouldShowValidationError = false,
            onConfigUpdate = {},
        )
    }
}
