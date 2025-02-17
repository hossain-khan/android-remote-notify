package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.WebhookConfigDataStore.Companion.ValidationKeys
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun WebhookConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = alertMediumConfig as AlertMediumConfig.WebhookConfig?
    val errors = configValidationResult.errors
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        TextField(
            value = config?.url ?: "",
            onValueChange = {
                onConfigUpdate(config?.copy(url = it))
            },
            label = { Text("Webhook URL") },
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
                        Text("Enter the URL to receive notifications via webhook")
                        Text("HTTP POST with json payload will be sent to the URL.")
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewWebhookConfigInputUi() {
    ComposeAppTheme {
        WebhookConfigInputUi(
            alertMediumConfig = AlertMediumConfig.WebhookConfig(url = "https://example.com"),
            configValidationResult =
                ConfigValidationResult(
                    isValid = false,
                    errors = mapOf(ValidationKeys.URL to "Invalid URL format"),
                ),
            shouldShowValidationError = true,
            onConfigUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewWebhookConfigInputUiEmpty() {
    ComposeAppTheme {
        WebhookConfigInputUi(
            alertMediumConfig = null,
            configValidationResult = ConfigValidationResult(true, emptyMap()),
            shouldShowValidationError = false,
            onConfigUpdate = {},
        )
    }
}
