package dev.hossain.remotenotify.ui.addalertmedium

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.data.AlertMediumConfig
import timber.log.Timber

@Composable
internal fun TelegramConfigInputUi(
    alertMediumConfig: AlertMediumConfig?,
    onConfigUpdate: (AlertMediumConfig?) -> Unit,
) {
    val config = alertMediumConfig as AlertMediumConfig.TelegramConfig?

    SideEffect {
        Timber.d("Rendering TelegramConfigInputUi with: $config")
    }

    Column {
        TextField(
            value = config?.botToken ?: "",
            onValueChange = {
                onConfigUpdate(config?.copy(botToken = it))
            },
            label = { Text("Bot Token") },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = config?.chatId ?: "",
            onValueChange = {
                onConfigUpdate(config?.copy(chatId = it))
            },
            label = { Text("Chat ID") },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
