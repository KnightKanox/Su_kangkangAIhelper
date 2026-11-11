package com.kangkang.shiyangai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

private val inlineMarkdown = Regex("""\*\*(.+?)\*\*|__(.+?)__|\*([^*\n]+)\*|_([^_\n]+)_|`([^`]+)`|\[([^]]+)]\(([^)]+)\)""")
private val heading = Regex("""^#{1,6}\s+""")
private val bullet = Regex("""^\s*[-*+]\s+""")
private val quote = Regex("""^\s*>\s?""")

@Composable
fun MarkdownText(content: String, modifier: Modifier = Modifier) {
    val rendered = remember(content) { renderMarkdown(content) }
    Text(
        text = rendered,
        modifier = modifier,
        fontSize = 20.sp,
        lineHeight = 32.sp,
        color = MaterialTheme.colorScheme.onSurface
    )
}

internal fun renderMarkdown(content: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    content.replace("\r\n", "\n").lines().forEachIndexed { index, rawLine ->
        if (index > 0) builder.append("\n")
        var line = rawLine
        val isHeading = heading.containsMatchIn(line)
        if (isHeading) line = line.replaceFirst(heading, "")
        if (bullet.containsMatchIn(line)) line = line.replaceFirst(bullet, "•  ")
        if (quote.containsMatchIn(line)) line = line.replaceFirst(quote, "│  ")
        if (isHeading) builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 23.sp))

        var cursor = 0
        inlineMarkdown.findAll(line).forEach { match ->
            builder.append(line.substring(cursor, match.range.first))
            val groups = match.groupValues
            val value = groups.drop(1).first { it.isNotEmpty() }
            val style = when {
                groups[1].isNotEmpty() || groups[2].isNotEmpty() -> SpanStyle(fontWeight = FontWeight.Bold)
                groups[3].isNotEmpty() || groups[4].isNotEmpty() -> SpanStyle(fontWeight = FontWeight.Medium)
                groups[6].isNotEmpty() -> SpanStyle(textDecoration = TextDecoration.Underline)
                else -> SpanStyle()
            }
            builder.pushStyle(style)
            builder.append(value)
            builder.pop()
            cursor = match.range.last + 1
        }
        builder.append(line.substring(cursor))
        if (isHeading) builder.pop()
    }
    return builder.toAnnotatedString()
}
