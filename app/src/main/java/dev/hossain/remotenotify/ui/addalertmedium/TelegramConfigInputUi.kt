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
            isError = errors["botToken"] != null,
            supportingText =
                errors["botToken"]?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
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
            isError = errors["chatId"] != null,
            supportingText =
                errors["chatId"]?.let { error ->
                    { Text(error, color = MaterialTheme.colorScheme.error) }
                },
        )
    }
}
