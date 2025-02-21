package dev.hossain.remotenotify.ui.addalert

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.theme.ComposeAppTheme

@Composable
internal fun BatteryOptimizationCard(
    onOptimizeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    "Enable More Reliable Checks",
                    style = MaterialTheme.typography.titleMedium,
                )
            },
            supportingContent = {
                Text(
                    "For more reliable monitoring, disable battery optimization for this app",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.battery_5_bar_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingContent = {
                Button(
                    onClick = onOptimizeClick,
                ) {
                    Text("Optimize")
                }
            },
        )
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewBatteryOptimizationCard() {
    ComposeAppTheme {
        Surface {
            BatteryOptimizationCard({})
        }
    }
}
