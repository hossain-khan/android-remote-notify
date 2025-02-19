package dev.hossain.remotenotify.utils

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import dev.hossain.remotenotify.theme.ComposeAppTheme
import dev.hossain.remotenotify.theme.jsonBooleanColor
import dev.hossain.remotenotify.theme.jsonBooleanColorDark
import dev.hossain.remotenotify.theme.jsonKeyColor
import dev.hossain.remotenotify.theme.jsonKeyColorDark
import dev.hossain.remotenotify.theme.jsonNumberColor
import dev.hossain.remotenotify.theme.jsonNumberColorDark
import dev.hossain.remotenotify.theme.jsonStringColor
import dev.hossain.remotenotify.theme.jsonStringColorDark
import org.intellij.lang.annotations.Language
import org.json.JSONObject

private const val JSON_KEYWORD_TRUE = "true"
private const val JSON_KEYWORD_FALSE = "false"
private const val JSON_KEYWORD_NULL = "null"

/**
 * Formats a JSON string by applying syntax highlighting using `AnnotatedString`. Different JSON
 * components (keys, values, strings, numbers, booleans, null, and punctuation) are highlighted with
 * distinct styles.
 *
 * @param jsonText The raw JSON string to be formatted with syntax highlighting.
 * @return An [AnnotatedString] where the JSON components are highlighted with respective styles.
 *
 * Example usage:
 * ```kotlin
 * val json = """
 * {
 *     "name": "Hossain Khan",
 *     "version": 0.99,
 *     "isAI": true,
 *     "hasAGI": false,
 *     "features": null,
 *     "supported_arc": ["arm", "x86", "mips"]
 * }
 * """.trimIndent()
 *
 * val annotatedJson = highlightJsonSyntax(json)
 * Text(text = annotatedJson)
 * ```
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun highlightJsonSyntax(
    @Language("json") jsonText: String,
): AnnotatedString {
    val isDark = isSystemInDarkTheme()

    // Theme-aware styles
    val keyStyle =
        SpanStyle(
            color = if (isDark) jsonKeyColorDark else jsonKeyColor,
        )
    val stringStyle =
        SpanStyle(
            color = if (isDark) jsonStringColorDark else jsonStringColor,
        )
    val numberStyle =
        SpanStyle(
            color = if (isDark) jsonNumberColorDark else jsonNumberColor,
        )
    val booleanStyle =
        SpanStyle(
            color = if (isDark) jsonBooleanColorDark else jsonBooleanColor,
        )
    val nullStyle =
        SpanStyle(
            color = if (isDark) jsonBooleanColorDark else jsonBooleanColor,
            fontWeight = FontWeight.Bold,
        )
    val punctuationStyle =
        SpanStyle(
            color = MaterialTheme.colorScheme.onSurface,
        )

    val builder =
        buildAnnotatedString {
            var currentIndex = 0

            // Simple JSON lexer logic (could be replaced with more robust parsing)
            while (currentIndex < jsonText.length) {
                val char = jsonText[currentIndex]
                when {
                    // Handle new lines to preserve them
                    char == '\n' -> {
                        append("\n")
                        currentIndex++
                    }

                    // Keys (strings before colon)
                    char == '"' -> {
                        val start = currentIndex
                        currentIndex++
                        while (currentIndex < jsonText.length && jsonText[currentIndex] != '"') {
                            currentIndex++
                        }
                        currentIndex++ // Move past the closing quote

                        // Check if this string is followed by a colon (indicating it's a key)
                        if (currentIndex < jsonText.length && jsonText[currentIndex] == ':') {
                            appendWithStyle(jsonText.substring(start, currentIndex), keyStyle)
                        } else {
                            appendWithStyle(jsonText.substring(start, currentIndex), stringStyle)
                        }
                    }

                    // Numbers
                    char.isDigit() || (char == '-' && jsonText[currentIndex + 1].isDigit()) -> {
                        val start = currentIndex
                        while (
                            currentIndex < jsonText.length &&
                            (jsonText[currentIndex].isDigit() || jsonText[currentIndex] == '.')
                        ) {
                            currentIndex++
                        }
                        appendWithStyle(jsonText.substring(start, currentIndex), numberStyle)
                    }

                    // Booleans (true, false) or null
                    jsonText.startsWith(JSON_KEYWORD_TRUE, currentIndex) -> {
                        appendWithStyle(JSON_KEYWORD_TRUE, booleanStyle)
                        currentIndex += JSON_KEYWORD_TRUE.length
                    }

                    jsonText.startsWith(JSON_KEYWORD_FALSE, currentIndex) -> {
                        appendWithStyle(JSON_KEYWORD_FALSE, booleanStyle)
                        currentIndex += JSON_KEYWORD_FALSE.length
                    }

                    jsonText.startsWith(JSON_KEYWORD_NULL, currentIndex) -> {
                        appendWithStyle(JSON_KEYWORD_NULL, nullStyle)
                        currentIndex += JSON_KEYWORD_NULL.length
                    }

                    // Punctuation ({}[]:,)
                    char in "{}[]:, " -> {
                        appendWithStyle(char.toString(), punctuationStyle)
                        currentIndex++
                    }

                    // Skip unrecognized characters
                    else -> currentIndex++
                }
            }
        }

    return builder
}

// Helper function to append text with a specific style
private fun AnnotatedString.Builder.appendWithStyle(
    text: String,
    style: SpanStyle,
) {
    withStyle(style = style) { append(text) }
}

/**
 * Internal function to re-format JSON code with indentation for pretty JSON output.
 */
internal fun formatJson(jsonString: String): String {
    val jsonObject = JSONObject(jsonString)
    return jsonObject.toString(2) // Indentation with 2 spaces
}

@PreviewLightDark
@Composable
private fun PreviewHighlightJsonSyntax() {
    ComposeAppTheme {
        Surface {
            Column {
                Text(
                    text =
                        highlightJsonSyntax(
                            """
                            {
                              "widget": {
                                "debug": "on",
                                "window": {
                                  "title": "Sample Amazing Widget",
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
                        ),
                    modifier = Modifier.padding(16.dp),
                )
                Text(
                    text =
                        highlightJsonSyntax(
                            """
                            {
                                "name": "Hossain Khan",
                                "version": 0.99,
                                "isAI": true,
                                "hasAGI": false,
                                "features": null,
                                "supported_arc": ["arm", "x86", "mips"]
                            }
                            """.trimIndent(),
                        ),
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
