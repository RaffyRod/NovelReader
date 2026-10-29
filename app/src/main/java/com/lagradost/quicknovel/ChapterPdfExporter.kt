package com.lagradost.quicknovel

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import org.jsoup.Jsoup
import java.io.OutputStream

/** A chapter ready to be rendered, with its title and plain text paragraphs. */
data class PdfChapter(val title: String, val paragraphs: List<String>)

object ChapterPdfExporter {
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48
    private const val BODY_SIZE = 11
    private const val TITLE_SIZE = 18
    private const val FOOTER_SIZE = 9
    private const val BREAK_TOKEN = "§§BR§§"

    private val whitespaceRegex = Regex("\\s+")
    private val lineBreakRegex = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private const val BLOCK_SELECTOR = "p,h1,h2,h3,h4,h5,h6,li"

    /**
     * Converts the html of a chapter into plain text paragraphs, keeping explicit line breaks.
     * Falls back to the whole body text when the chapter has no block level elements.
     */
    fun htmlToParagraphs(html: String): List<String> {
        val normalized = html
            .replace(whitespaceRegex, " ")
            .replace(lineBreakRegex, BREAK_TOKEN)
        val document = Jsoup.parse(normalized)
        document.select("script,style").remove()

        val leafBlocks = document.select(BLOCK_SELECTOR)
            .filter { block -> block.select(BLOCK_SELECTOR).size == 1 }
        val rawParagraphs = if (leafBlocks.isEmpty()) {
            listOf(document.body().text())
        } else {
            leafBlocks.map { block -> block.text() }
        }

        return rawParagraphs
            .map { text ->
                text.replace(BREAK_TOKEN, "\n")
                    .lines()
                    .joinToString("\n") { line -> line.trim() }
                    .trim()
            }
            .filter { text -> text.isNotBlank() }
    }

    /**
     * Writes all the chapters into a single A4 PDF, starting every chapter on a new page.
     * The stream is not closed by this function.
     */
    fun write(chapters: List<PdfChapter>, output: OutputStream) {
        val document = PdfDocument()
        val contentWidth = PAGE_WIDTH - 2 * MARGIN
        val contentHeight = PAGE_HEIGHT - 2 * MARGIN

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = BODY_SIZE.toFloat()
            color = Color.BLACK
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = FOOTER_SIZE.toFloat()
            color = Color.GRAY
            textAlign = Paint.Align.CENTER
        }

        var pageNumber = 0
        for (chapter in chapters) {
            val layout = buildLayout(chapter, bodyPaint, contentWidth)
            val lineCount = layout.lineCount
            var firstLine = 0

            do {
                val pageTop = layout.getLineTop(firstLine)
                var lastLine = firstLine
                while (lastLine + 1 < lineCount &&
                    layout.getLineBottom(lastLine + 1) - pageTop <= contentHeight
                ) {
                    lastLine++
                }

                pageNumber++
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber)
                    .create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                canvas.save()
                canvas.translate(MARGIN.toFloat(), MARGIN - pageTop.toFloat())
                canvas.clipRect(
                    0f,
                    pageTop.toFloat(),
                    contentWidth.toFloat(),
                    layout.getLineBottom(lastLine).toFloat()
                )
                layout.draw(canvas)
                canvas.restore()

                canvas.drawText(
                    pageNumber.toString(),
                    PAGE_WIDTH / 2f,
                    PAGE_HEIGHT - MARGIN / 2f,
                    footerPaint
                )
                document.finishPage(page)

                firstLine = lastLine + 1
            } while (firstLine < lineCount)
        }

        document.writeTo(output)
        document.close()
    }

    private fun buildLayout(chapter: PdfChapter, paint: TextPaint, width: Int): StaticLayout {
        val text = SpannableStringBuilder()
        val title = chapter.title.trim()
        if (title.isNotEmpty()) {
            text.append(title)
            text.setSpan(
                AbsoluteSizeSpan(TITLE_SIZE),
                0,
                title.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            text.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                title.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            text.append("\n\n")
        }
        text.append(chapter.paragraphs.joinToString("\n\n"))

        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1f)
            .setIncludePad(false)
            .build()
    }
}
