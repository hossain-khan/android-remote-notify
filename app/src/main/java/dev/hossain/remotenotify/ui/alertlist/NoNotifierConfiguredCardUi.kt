package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun NoNotifierConfiguredCard(
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text =
                    "You haven't set up a notification method yet." +
                        "\n\nConfigure one now to receive alerts when this device's battery or storage level drops below your chosen limit.",
            )
            Spacer(modifier = Modifier.size(8.dp))
            Button(onClick = onConfigureClick) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Configure")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewNoNotifierConfiguredCard() {
    ComposeAppTheme {
        NoNotifierConfiguredCard(
            onConfigureClick = {},
        )
    }
}
