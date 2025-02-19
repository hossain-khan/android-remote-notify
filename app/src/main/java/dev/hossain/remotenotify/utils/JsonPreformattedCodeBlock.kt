package dev.hossain.remotenotify.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.PreviewDynamicColors
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.hossain.remotenotify.theme.ComposeAppTheme

/** A simple composable view that show preformatted code block in a card. */
@Composable
fun PreformattedCodeBlock(
    codeBlock: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color = Color.Gray),
    ) {
        // Allows anyone to copy the content of code block
        SelectionContainer {
            Text(
                text = highlightJsonSyntax(formatJson(codeBlock)),
                modifier = Modifier.padding(16.dp),
                style =
                    MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
            )
        }
    }
}

@Composable
@PreviewLightDark
@PreviewDynamicColors
private fun PreformattedCodeBlockPreview() {
    ComposeAppTheme {
        Surface {
            PreformattedCodeBlock(
                // language=JSON
                """
                {
                  "widget": {
                    "debug": "on",
                    "window": {
                      "title": "Sample Konfabulator Widget",
                      "name": "main_window",
                      "width": 500,
                      "height": 500
                    },
                    "image": {
                      "src": "Images/Sun.png",
                      "name": "sun1",
                      "hOffset": 250,
                      "vOffset": 250,
                      "alignment": "center"
                    },
                    "text": {
                      "data": "Click Here",
                      "size": 36,
                      "style": "bold",
                      "name": "text1",
                      "hOffset": 250,
                      "vOffset": 100,
                      "alignment": "center",
                      "onMouseUp": "sun1.opacity = (sun1.opacity / 100) * 90;"
                    }
                  }
                }
                """.trimIndent(),
            )
        }
    }
}
