package com.lagradost.quicknovel.ui

/**
 * Pure Glyph Pulse (F) loading copy — chapter title + status, never a raw URL as title.
 */
object ReaderGlyphLoading {
    data class Model(
        val title: String,
        val subtitle: String,
        val sourceChip: String?,
    )

    fun isHttpUrl(value: String): Boolean =
        value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)

    fun looksLikeUrlPath(value: String): Boolean {
        if (value.contains("://")) return true
        // Progress strings like "Traduciendo X (3/40)" have '/' and '.' but also spaces.
        if (value.any { it.isWhitespace() }) return false
        return value.contains('/') && value.contains('.')
    }

    /**
     * Host from an http(s) URL without Android Uri (JVM unit-test friendly).
     */
    fun hostFromUrl(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null
        val hostPort = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = hostPort.substringBefore(':').removePrefix("www.")
        return host.takeIf { it.isNotBlank() }
    }

    fun resolve(
        status: String?,
        chapterTitle: String?,
        sourceChip: String?,
        loadingChapterLabel: String,
        loadingFallback: String,
    ): Model {
        when {
            status.isNullOrBlank() -> {
                return Model(
                    title = chapterTitle ?: loadingFallback,
                    subtitle = loadingChapterLabel,
                    sourceChip = sourceChip,
                )
            }
            isHttpUrl(status) -> {
                val host = hostFromUrl(status) ?: sourceChip
                return Model(
                    title = chapterTitle ?: loadingFallback,
                    subtitle = loadingChapterLabel,
                    sourceChip = host,
                )
            }
            looksLikeUrlPath(status) -> {
                return Model(
                    title = chapterTitle ?: loadingFallback,
                    subtitle = loadingChapterLabel,
                    sourceChip = sourceChip,
                )
            }
            else -> {
                val looksLikeProgress =
                    status.contains('(') ||
                        status.contains('%') ||
                        status.length > 48 ||
                        (chapterTitle != null &&
                            !status.equals(chapterTitle, ignoreCase = true))
                return if (looksLikeProgress && chapterTitle != null) {
                    Model(
                        title = chapterTitle,
                        subtitle = status,
                        sourceChip = sourceChip,
                    )
                } else {
                    Model(
                        title = status,
                        subtitle = loadingChapterLabel,
                        sourceChip = sourceChip,
                    )
                }
            }
        }
    }
}
