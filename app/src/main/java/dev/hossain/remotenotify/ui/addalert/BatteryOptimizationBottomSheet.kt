package dev.hossain.remotenotify.ui.addalert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.R
import dev.hossain.remotenotify.theme.ComposeAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationBottomSheet(
    sheetState: SheetState,
    onSettingsClick: () -> Unit,
    onDontRemind: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        BatteryOptimizationUi(onSettingsClick = onSettingsClick, onDontRemind = onDontRemind)
    }
}

@Composable
private fun BatteryOptimizationUi(
    onSettingsClick: () -> Unit,
    onDontRemind: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Icon and Title
        Icon(
            painter = painterResource(id = R.drawable.battery_5_bar_24dp),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Battery Optimization",
            style = MaterialTheme.typography.titleLarge,
        )

        // Description
        Text(
            text = "For reliable background monitoring, this app needs to be excluded from battery optimization.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        val appName = stringResource(id = R.string.app_name)

        // Steps
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "How to disable battery optimization:",
                style = MaterialTheme.typography.titleSmall,
            )
            Text("1. Open device Settings")
            Text("2. Go to Apps > Select '$appName'")
            Text("3. Select 'Battery'")
            Text("4. Choose 'Unrestricted' or 'Not optimized'")
        }

        // Action Button
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            FilledTonalButton(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings")
            }

            TextButton(
                onClick = onDontRemind,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Don't remind me again")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewBatteryOptimizationUi() {
    ComposeAppTheme {
        Surface {
            BatteryOptimizationUi({}, {})
        }
    }
}
