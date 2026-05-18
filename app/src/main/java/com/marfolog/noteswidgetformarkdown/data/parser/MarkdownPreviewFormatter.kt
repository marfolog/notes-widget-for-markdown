package com.marfolog.noteswidgetformarkdown.data.parser

import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer

class MarkdownPreviewFormatter(
    private val previewLineCount: Int = DEFAULT_PREVIEW_LINE_COUNT
) {
    private val parser = Parser.builder()
        .extensions(extensions)
        .build()
    private val renderer = TextContentRenderer.builder()
        .extensions(extensions)
        .build()

    fun format(source: String): String {
        val markdown = source
            .lineSequence()
            .dropYamlFrontMatter()
            .map(::normalizeObsidianMarkdown)
            .joinToString("\n")

        val document = parser.parse(markdown)
        return renderer.render(document)
            .lineSequence()
            .map { cleanupRenderedText(it.trim()) }
            .filter { it.isNotBlank() }
            .take(previewLineCount)
            .joinToString("\n")
    }

    private fun normalizeObsidianMarkdown(line: String): String {
        return line
            .replace(calloutMarkerRegex, "")
            .replace(taskItemRegex) { match ->
                val checked = match.groupValues[1].equals("x", ignoreCase = true)
                if (checked) "Done: " else "Todo: "
            }
            .replace(wikiLinkWithAliasRegex) { match -> match.groupValues[2] }
            .replace(wikiLinkRegex) { match -> match.groupValues[1] }
    }

    private fun cleanupRenderedText(line: String): String {
        return line
            .replace(renderedLinkRegex) { match -> match.groupValues[1] }
            .replace(renderedQuotedInlineRegex) { match -> match.groupValues[1] }
            .replace(renderedWrappedInlineRegex) { match -> match.groupValues[1] }
            .trim()
    }

    private fun Sequence<String>.dropYamlFrontMatter(): Sequence<String> {
        val lines = toList()
        if (lines.firstOrNull()?.trim() != "---") {
            return lines.asSequence()
        }

        val endIndex = lines.drop(1).indexOfFirst { it.trim() == "---" }
        return if (endIndex == -1) {
            lines.asSequence()
        } else {
            lines.drop(endIndex + 2).asSequence()
        }
    }

    companion object {
        private const val DEFAULT_PREVIEW_LINE_COUNT = 5
        private val extensions: List<Extension> = listOf(
            TaskListItemsExtension.create(),
            StrikethroughExtension.create()
        )
        private val taskItemRegex = Regex("^\\s*[-*+]\\s+\\[([ xX])]\\s+")
        private val calloutMarkerRegex = Regex("^\\s*>\\s*\\[![A-Za-z]+]\\s*")
        private val wikiLinkWithAliasRegex = Regex("\\[\\[([^]|]+)\\|([^]]+)]]")
        private val wikiLinkRegex = Regex("\\[\\[([^]]+)]]")
        private val renderedLinkRegex = Regex("\"([^\"]+)\" \\([^)]*\\)")
        private val renderedQuotedInlineRegex = Regex("\"([^\"]+)\"")
        private val renderedWrappedInlineRegex = Regex("^[/_](.+)[/_]$")
    }
}
