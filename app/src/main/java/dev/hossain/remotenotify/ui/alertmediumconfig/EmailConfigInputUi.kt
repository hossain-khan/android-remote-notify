package dev.hossain.remotenotify.ui.alertmediumconfig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.hossain.remotenotify.data.ConfigValidationResult
import dev.hossain.remotenotify.data.MailgunConfigDataStore.Companion.ValidationKeys
import dev.hossain.remotenotify.model.AlertMediumConfig
import dev.hossain.remotenotify.notifier.mailgun.MailgunConfig

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
