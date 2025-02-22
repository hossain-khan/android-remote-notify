package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun EmptyNotificationsState(onLearnMoreClick: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            modifier =
                Modifier
                    .size(72.dp)
                    .padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            "No Alerts Yet",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            "Add your first alert to start monitoring",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ElevatedButton(
            onClick = onLearnMoreClick,
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Learn More")
        }
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewEmptyNotificationsState() {
    ComposeAppTheme {
        Surface {
            EmptyNotificationsState({})
        }
    }
}
