package dev.hossain.remotenotify.ui.addalertmedium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.WebhookConfigDataStore.Companion.ValidationKeys

@Composable
internal fun WebhookConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
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
            isError = shouldShowValidationError && errors[ValidationKeys.URL] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.URL] != null) {
                    Text(errors[ValidationKeys.URL]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter the URL to receive notifications")
                }
            },
        )
    }
}
