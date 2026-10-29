package com.lagradost.quicknovel

/** One page of a list: [page] is zero based, [size] is [ChapterPaging.ALL] when nothing is split. */
data class ChapterPage<T>(
    val items: List<T>,
    val page: Int,
    val pageCount: Int,
    val size: Int,
    val total: Int,
) {
    val hasPrevious: Boolean get() = page > 0
    val hasNext: Boolean get() = page < pageCount - 1

    /** Position of the first item of this page in the whole list, 1 based. */
    val firstPosition: Int get() = if (items.isEmpty()) 0 else page * effectiveSize(size, total) + 1
    val lastPosition: Int get() = if (items.isEmpty()) 0 else firstPosition + items.size - 1

    private fun effectiveSize(size: Int, total: Int) = if (size == ChapterPaging.ALL) total else size
}

/** Rules for splitting the chapter list in pages the user can size. */
object ChapterPaging {
    /** Page size meaning "show every chapter in one page". */
    const val ALL = 0
    const val DEFAULT_SIZE = 20
    const val MAX_SIZE = 200
    val PRESETS = listOf(10, 20, 30)

    /** Keeps a size usable: zero or less means all chapters, and huge values are capped. */
    fun normalizeSize(size: Int): Int = if (size <= 0) ALL else size.coerceAtMost(MAX_SIZE)

    fun pageCount(total: Int, size: Int): Int {
        val normalized = normalizeSize(size)
        if (total <= 0 || normalized == ALL) return 1
        return (total + normalized - 1) / normalized
    }

    /** Returns [page] of [list], moving the page into range when it is too low or too high. */
    fun <T> pageOf(list: List<T>, page: Int, size: Int): ChapterPage<T> {
        val normalized = normalizeSize(size)
        val count = pageCount(list.size, normalized)
        val safePage = page.coerceIn(0, count - 1)
        val items = if (normalized == ALL) {
            list
        } else {
            list.drop(safePage * normalized).take(normalized)
        }
        return ChapterPage(items, safePage, count, normalized, list.size)
    }

    /**
     * Page that keeps the chapter the reader was looking at when the page size changes,
     * so changing from 10 to 30 per page does not throw the reader somewhere else.
     */
    fun pageAfterResize(current: ChapterPage<*>, newSize: Int): Int {
        val normalized = normalizeSize(newSize)
        if (normalized == ALL || current.items.isEmpty()) return 0
        return (current.firstPosition - 1) / normalized
    }

    /** Zero based page that holds the chapter at [position] (1 based), or null when it is out of range. */
    fun pageOfPosition(position: Int, total: Int, size: Int): Int? {
        if (position !in 1..total) return null
        val normalized = normalizeSize(size)
        return if (normalized == ALL) 0 else (position - 1) / normalized
    }

    /** Parses the number typed in the custom size box, null when it is not a valid size. */
    fun parseSize(text: String): Int? =
        text.trim().toIntOrNull()?.takeIf { it in 1..MAX_SIZE }
}
