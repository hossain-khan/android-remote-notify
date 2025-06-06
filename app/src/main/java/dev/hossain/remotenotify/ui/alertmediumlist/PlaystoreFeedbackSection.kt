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

/**
 * Composable function that provides a UI section for users to share feedback.
 *
 * This function displays a button that, when clicked, triggers the `onShareFeedback` callback
 * and then attempts to launch an in-app review flow using [InAppReviewManager].
 * It also includes a text prompt encouraging users to share feedback or suggest new mediums.
 *
 * @param modifier Optional [Modifier] to be applied to the composable.
 * @param onShareFeedback A callback function that is invoked when the feedback button is clicked.
 *                       This can be used to log analytics events or perform other actions
 *                       before the in-app review flow is initiated.
 */
@Composable
fun FeedbackAndRequestMediumUi(
    modifier: Modifier = Modifier,
    onShareFeedback: () -> Unit = {},
) {
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
                    onShareFeedback()
                    scope.launch {
                        InAppReviewManager(it).requestReview()
                    }
                } ?: run {
                    Timber.w("Activity is null, cannot request review.")
                }
            },
        ) {
            Text(
                text = "Share Feedback",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Consider sharing your feedback on Play Store or suggesting new mediums.",
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
