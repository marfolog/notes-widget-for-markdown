package com.marfolog.noteswidgetformarkdown.data.parser

import org.commonmark.Extension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser

class MarkdownPreviewFormatter(
    private val previewLineCount: Int = DEFAULT_PREVIEW_LINE_COUNT
) {
    private val parser = Parser.builder()
        .extensions(extensions)
        .build()

    fun format(source: String): String {
        val markdown = source
            .lineSequence()
            .dropYamlFrontMatter()
            .map(::normalizeObsidianMarkdown)
            .joinToString("\n")

        val document = parser.parse(markdown)
        return PreviewVisitor().render(document)
            .lineSequence()
            .map { cleanupRenderedText(it.trimEnd()) }
            .filter { it.isNotBlank() }
            .take(previewLineCount)
            .joinToString("\n")
    }

    private fun normalizeObsidianMarkdown(line: String): String {
        return line
            .replace(calloutMarkerRegex, "")
            .replace(wikiLinkWithAliasRegex) { match -> match.groupValues[2] }
            .replace(wikiLinkRegex) { match -> match.groupValues[1] }
    }

    private fun cleanupRenderedText(line: String): String {
        return line
            .replace(renderedLinkRegex) { match -> match.groupValues[1] }
            .replace(renderedQuotedInlineRegex) { match -> match.groupValues[1] }
            .replace(renderedWrappedInlineRegex) { match -> match.groupValues[1] }
            .trimEnd()
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

    private class PreviewVisitor : AbstractVisitor() {
        private val builder = StringBuilder()
        private var listDepth = 0
        private val orderedCounters = mutableListOf<Int>()

        fun render(node: Node): String {
            node.accept(this)
            return builder.toString()
        }

        override fun visit(document: Document) {
            visitChildren(document)
        }

        override fun visit(heading: Heading) {
            appendLineFromChildren(heading)
        }

        override fun visit(paragraph: Paragraph) {
            if (paragraph.parent is ListItem) {
                visitChildren(paragraph)
            } else {
                appendLineFromChildren(paragraph)
            }
        }

        override fun visit(bulletList: BulletList) {
            if (bulletList.parent is ListItem) {
                appendNewLineIfNeeded()
            }
            listDepth += 1
            visitChildren(bulletList)
            listDepth -= 1
        }

        override fun visit(orderedList: OrderedList) {
            if (orderedList.parent is ListItem) {
                appendNewLineIfNeeded()
            }
            orderedCounters.add(orderedList.markerStartNumber ?: 1)
            listDepth += 1
            visitChildren(orderedList)
            listDepth -= 1
            orderedCounters.removeAt(orderedCounters.lastIndex)
        }

        override fun visit(listItem: ListItem) {
            val prefix = buildListPrefix(listItem)
            builder.append("  ".repeat((listDepth - 1).coerceAtLeast(0)))
            builder.append(prefix)
            visitChildren(listItem)
            appendNewLineIfNeeded()
        }

        override fun visit(text: Text) {
            builder.append(text.literal)
        }

        override fun visit(code: Code) {
            builder.append(code.literal)
        }

        override fun visit(link: Link) {
            visitChildren(link)
        }

        override fun visit(emphasis: Emphasis) {
            visitChildren(emphasis)
        }

        override fun visit(strongEmphasis: StrongEmphasis) {
            visitChildren(strongEmphasis)
        }

        override fun visit(softLineBreak: SoftLineBreak) {
            builder.append('\n')
        }

        override fun visit(hardLineBreak: HardLineBreak) {
            builder.append('\n')
        }

        private fun appendLineFromChildren(node: Node) {
            visitChildren(node)
            appendNewLineIfNeeded()
        }

        private fun appendNewLineIfNeeded() {
            if (builder.isNotEmpty() && builder.last() != '\n') {
                builder.append('\n')
            }
        }

        private fun buildListPrefix(listItem: ListItem): String {
            findTaskMarker(listItem)?.let { marker ->
                return if (marker.isChecked) "☑ " else "☐ "
            }

            val text = firstTextChild(listItem)
            val taskMatch = text?.literal?.let { parsedTaskItemRegex.find(it) }
            if (taskMatch != null) {
                val checked = taskMatch.groupValues[1].equals("x", ignoreCase = true)
                text.literal = text.literal.replaceFirst(parsedTaskItemRegex, "")
                return if (checked) "☑ " else "☐ "
            }

            val orderedCounterIndex = orderedCounters.lastIndex
            if (orderedCounterIndex >= 0 && listItem.parent is OrderedList) {
                val number = orderedCounters[orderedCounterIndex]
                orderedCounters[orderedCounterIndex] = number + 1
                return "$number. "
            }

            return "• "
        }

        private fun findTaskMarker(node: Node): TaskListItemMarker? {
            var current = node.firstChild
            while (current != null) {
                if (current is TaskListItemMarker) {
                    return current
                }
                findTaskMarker(current)?.let { return it }
                current = current.next
            }
            return null
        }

        private fun firstTextChild(node: Node): Text? {
            var current = node.firstChild
            while (current != null) {
                if (current is Text) {
                    return current
                }
                firstTextChild(current)?.let { return it }
                current = current.next
            }
            return null
        }
    }

    companion object {
        private const val DEFAULT_PREVIEW_LINE_COUNT = 18
        private val extensions: List<Extension> = listOf(
            TaskListItemsExtension.create(),
            StrikethroughExtension.create()
        )
        private val parsedTaskItemRegex = Regex("^\\s*\\[([ xX])]\\s+")
        private val calloutMarkerRegex = Regex("^\\s*>\\s*\\[![A-Za-z]+]\\s*")
        private val wikiLinkWithAliasRegex = Regex("\\[\\[([^]|]+)\\|([^]]+)]]")
        private val wikiLinkRegex = Regex("\\[\\[([^]]+)]]")
        private val renderedLinkRegex = Regex("\"([^\"]+)\" \\([^)]*\\)")
        private val renderedQuotedInlineRegex = Regex("\"([^\"]+)\"")
        private val renderedWrappedInlineRegex = Regex("^[/_](.+)[/_]$")
    }
}
