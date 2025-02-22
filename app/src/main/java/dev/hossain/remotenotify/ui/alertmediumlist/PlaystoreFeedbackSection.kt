package dev.hossain.remotenotify.ui.alertmediumlist

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.utils.InAppReviewManager
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun FeedbackAndRequestMediumUi(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    Column(
        modifier =
            modifier
                .wrapContentSize()
                .padding(top = 32.dp)
                .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedButton(
            onClick = {
                activity?.let {
                    scope.launch {
                        InAppReviewManager(it).requestReview()
                    }
                } ?: run {
                    Timber.w("Activity is null, cannot request review.")
                }
            },
        ) {
            Text(
                text = "Request New Medium",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Provide feedback on Google Play Store and request new medium.",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(horizontal = 64.dp),
        )
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreviewRatingSection() {
    ComposeAppTheme {
        Surface {
            FeedbackAndRequestMediumUi()
        }
    }
}
