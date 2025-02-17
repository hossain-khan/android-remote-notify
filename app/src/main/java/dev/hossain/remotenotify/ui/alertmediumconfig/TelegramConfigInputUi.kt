package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import dev.hossain.remotenotify.data.TelegramConfigDataStore.Companion.ValidationKeys
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun TelegramConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = alertMediumConfig as AlertMediumConfig.TelegramConfig?
    val errors = configValidationResult.errors
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        Text(
            text =
                buildAnnotatedString {
                    append("Create a Telegram bot using ")
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://t.me/BotFather",
                    )
                    withStyle(
                        style =
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                    ) {
                        append("@BotFather")
                    }
                    append(" and get the bot token.")
                },
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.clickable {
                    uriHandler.openUri("https://t.me/BotFather")
                },
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.botToken ?: "",
            onValueChange = {
                onConfigUpdate(config?.copy(botToken = it))
            },
            label = { Text("Bot Token") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            isError = shouldShowValidationError && errors[ValidationKeys.BOT_TOKEN] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.BOT_TOKEN] != null) {
                    Text(errors[ValidationKeys.BOT_TOKEN]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter bot token provided by BotFather")
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.chatId ?: "",
            onValueChange = {
                onConfigUpdate(config?.copy(chatId = it))
            },
            label = { Text("Username/Chat ID") },
            modifier = Modifier.fillMaxWidth(),
            isError = shouldShowValidationError && errors[ValidationKeys.CHAT_ID] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.CHAT_ID] != null) {
                    Text(errors[ValidationKeys.CHAT_ID]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter receiver chat ID or @channel username")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTelegramConfigInputUi() {
    ComposeAppTheme {
        TelegramConfigInputUi(
            alertMediumConfig =
                AlertMediumConfig.TelegramConfig(
                    botToken = "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11",
                    chatId = "@mychannel",
                ),
            configValidationResult =
                ConfigValidationResult(
                    isValid = false,
                    errors = mapOf(ValidationKeys.BOT_TOKEN to "Invalid bot token format"),
                ),
            shouldShowValidationError = true,
            onConfigUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewTelegramConfigInputUiEmpty() {
    ComposeAppTheme {
        TelegramConfigInputUi(
            alertMediumConfig = null,
            configValidationResult = ConfigValidationResult(true, emptyMap()),
            shouldShowValidationError = false,
            onConfigUpdate = {},
        )
    }
}
