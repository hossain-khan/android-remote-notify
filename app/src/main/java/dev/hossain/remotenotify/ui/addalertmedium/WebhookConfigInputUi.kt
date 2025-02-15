package dev.hossain.remotenotify.ui.addalertmedium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.hossain.remotenotify.data.AlertMediumConfig
import dev.hossain.remotenotify.data.ConfigValidationResult

@Composable
internal fun WebhookConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
) {
    val config = alertMediumConfig as AlertMediumConfig.WebhookConfig?
    val errors = configValidationResult.errors

    Column {
        TextField(
            value = config?.url ?: "",
            onValueChange = {
                onConfigUpdate(config?.copy(url = it))
            },
            label = { Text("Webhook URL") },
            modifier = Modifier.fillMaxWidth(),
            isError = errors["url"] != null,
            supportingText = {
                if (errors["url"] != null) {
                    Text(errors["url"]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter the URL to receive notifications")
                }
            },
        )
    }
}
