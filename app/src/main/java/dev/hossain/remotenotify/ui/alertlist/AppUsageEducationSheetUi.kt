package dev.hossain.remotenotify.ui.alertlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.theme.ComposeAppTheme

/**
 * UI for first time user education sheet.
 * - https://developer.android.com/develop/ui/compose/components/bottom-sheets
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUsageEducationSheetUi(
    sheetState: SheetState,
    onDismissLearnMore: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = {
            onDismissLearnMore()
        },
        sheetState = sheetState,
    ) {
        EducationContentUi(onDismissLearnMore)
    }
}

@Composable
private fun EducationContentUi(onDismissLearnMore: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Did you know?",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text =
                buildAnnotatedString {
                    append("This app is designed to be installed on a ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("secondary phone/device")
                    }
                    append(" that can notify you (on your ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("primary phone")
                    }
                    append(
                        " via the notification mediums) about secondary device's low battery and storage alert based on your configuration.",
                    )
                },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onDismissLearnMore()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Got it")
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewEducationContentUi() {
    ComposeAppTheme {
        Surface {
            EducationContentUi(
                onDismissLearnMore = {},
            )
        }
    }
}
