package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.EmailConfigDataStore.Companion.ValidationKeys
import dev.hossain.remotenotify.data.EmailQuotaManager.Companion.ValidationKeys.EMAIL_DAILY_QUOTA
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.notifier.mailgun.MailgunConfig
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun EmailConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    configValidationResult: ConfigValidationResult,
    shouldShowValidationError: Boolean,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val config =
        alertMediumConfig as AlertMediumConfig.EmailConfig?
            ?: AlertMediumConfig.EmailConfig(
                apiKey = MailgunConfig.API_KEY,
                domain = MailgunConfig.DOMAIN,
                fromEmail = MailgunConfig.FROM_EMAIL,
                toEmail = "",
            )
    val errors = configValidationResult.errors

    Column(modifier = modifier) {
        SelectionContainer {
            Text(
                text =
                    buildAnnotatedString {
                        append("Note: Alerts will be sent from <")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(MailgunConfig.FROM_EMAIL)
                        }
                        append(">. Please check your spam folder and consider adding this address to your contacts.")
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Show quota warning
        Text(
            text = "⚠️ Limited to 2 emails per day due to service quota limitations.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )

        if (shouldShowValidationError && configValidationResult.errors[EMAIL_DAILY_QUOTA] != null) {
            Text(
                text = configValidationResult.errors[EMAIL_DAILY_QUOTA]!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config.toEmail,
            onValueChange = {
                onConfigUpdate(config.copy(toEmail = it))
            },
            label = { Text("Your Email Address") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
            isError = shouldShowValidationError && errors[ValidationKeys.TO_EMAIL] != null,
            supportingText = {
                if (shouldShowValidationError && errors[ValidationKeys.TO_EMAIL] != null) {
                    Text(errors[ValidationKeys.TO_EMAIL]!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("Enter your email address to receive alerts")
                }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewEmailConfigInputWithError() {
    ComposeAppTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            EmailConfigInputUi(
                alertMediumConfig =
                    AlertMediumConfig.EmailConfig(
                        apiKey = MailgunConfig.API_KEY,
                        domain = MailgunConfig.DOMAIN,
                        fromEmail = MailgunConfig.FROM_EMAIL,
                        toEmail = "invalid-email",
                    ),
                configValidationResult =
                    ConfigValidationResult(
                        isValid = false,
                        errors =
                            mapOf(
                                ValidationKeys.TO_EMAIL to "Invalid email address format",
                                EMAIL_DAILY_QUOTA to "Daily email quota exceeded",
                            ),
                    ),
                shouldShowValidationError = true,
                onConfigUpdate = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewEmailConfigInputUiEmpty() {
    ComposeAppTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            EmailConfigInputUi(
                alertMediumConfig = null,
                configValidationResult = ConfigValidationResult(true, emptyMap()),
                shouldShowValidationError = false,
                onConfigUpdate = {},
            )
        }
    }
}
