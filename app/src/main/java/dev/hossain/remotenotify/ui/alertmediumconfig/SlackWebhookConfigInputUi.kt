package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
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
import dev.hossain.remotenotify.data.SlackWebhookConfigDataStore.Companion.ValidationKeys
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun SlackWebhookConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config = alertMediumConfig as AlertMediumConfig.WebhookConfig?
    val errors = configValidationResult.errors
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        Text(
            text =
                buildAnnotatedString {
                    append("Use a Slack workflow webhook to trigger automated workflows in Slack. ")
                    append("When an alert is triggered, a POST request will be sent to your Slack workflow URL, which can ")
                    append("start a workflow to notify your team, create tickets, or trigger other actions in Slack. ")
                    pushStringAnnotation(
                        tag = "URL",
                        annotation = "https://slack.com/help/articles/360041352714-Build-a-workflow--Create-a-workflow-that-starts-outside-of-Slack",
                    )
                    withStyle(
                        style =
                            SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                            ),
                    ) {
                        append("Learn how to create a Slack workflow webhook")
                    }
                },
            style = MaterialTheme.typography.bodyMedium,
            modifier =
                Modifier.clickable {
                    uriHandler.openUri(
                        "https://slack.com/help/articles/360041352714-Build-a-workflow--Create-a-workflow-that-starts-outside-of-Slack",
                    )
                },
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.url ?: "",
            onValueChange = {
                onConfigUpdate(config?.copy(url = it))
            },
            label = { Text("Slack Workflow Webhook URL") },
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
                        Text("Enter the Slack workflow webhook URL provided after publishing your workflow")
                    }
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSlackWebhookConfigInputUi() {
    ComposeAppTheme {
        SlackWebhookConfigInputUi(
            alertMediumConfig =
                AlertMediumConfig.WebhookConfig(
                    url = "https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX",
                ),
            configValidationResult =
                ConfigValidationResult(
                    isValid = false,
                    errors = mapOf(ValidationKeys.URL to "Invalid Slack webhook URL format"),
                ),
            shouldShowValidationError = true,
            onConfigUpdate = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewSlackWebhookConfigInputUiEmpty() {
    ComposeAppTheme {
        SlackWebhookConfigInputUi(
            alertMediumConfig = null,
            configValidationResult = ConfigValidationResult(true, emptyMap()),
            shouldShowValidationError = false,
            onConfigUpdate = {},
        )
    }
}
