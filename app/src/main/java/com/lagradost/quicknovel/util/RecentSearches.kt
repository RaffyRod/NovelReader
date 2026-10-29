package com.lagradost.quicknovel.util

import com.lagradost.quicknovel.BaseApplication.Companion.getKey
import com.lagradost.quicknovel.BaseApplication.Companion.setKey

/** The words the user searched for last, newest first, without repeats. */
object RecentSearches {
    private const val FOLDER = "search_recent"
    private const val KEY = "list"
    private const val SEPARATOR = "\n"
    const val MAX = 8

    fun add(current: List<String>, query: String): List<String> {
        val clean = query.trim()
        if (clean.isBlank()) return current
        return (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(MAX)
    }

    fun load(): List<String> =
        getKey<String>(FOLDER, KEY)?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()

    fun save(list: List<String>) = setKey(FOLDER, KEY, list.joinToString(SEPARATOR))
}
