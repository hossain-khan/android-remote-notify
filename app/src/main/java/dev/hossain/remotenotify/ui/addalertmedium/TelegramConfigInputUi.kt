package dev.hossain.remotenotify.ui.addalertmedium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.data.AlertMediumConfig
import dev.hossain.remotenotify.data.ConfigValidationResult

@Composable
internal fun TelegramConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
) {
    val config = alertMediumConfig as AlertMediumConfig.TelegramConfig?
    val errors = configValidationResult.errors

    Column {
        TextField(
            value = config?.botToken ?: "",
            onValueChange = {
                onConfigUpdate(config?.copy(botToken = it))
            },
            label = { Text("Bot Token") },
            modifier = Modifier.fillMaxWidth(),
            isError = shouldShowValidationError && errors["botToken"] != null,
            supportingText = {
                if (shouldShowValidationError && errors["botToken"] != null) {
                    Text(errors["botToken"]!!, color = MaterialTheme.colorScheme.error)
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
            label = { Text("Chat ID") },
            modifier = Modifier.fillMaxWidth(),
            isError = shouldShowValidationError && errors["chatId"] != null,
            supportingText = {
                if (shouldShowValidationError && errors["chatId"] != null) {
                    Text(errors["chatId"]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter chat ID or @channel username")
                }
            },
        )
    }
}
