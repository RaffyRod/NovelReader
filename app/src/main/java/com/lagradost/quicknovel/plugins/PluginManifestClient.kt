package com.lagradost.quicknovel.plugins

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.quicknovel.MainActivity.Companion.app

/** Fetches and parses LNReader-style plugin manifests (JSON catalog). */
object PluginManifestClient {
    private val mapper = jacksonObjectMapper()

    suspend fun fetch(url: String): List<PluginManifestItem> {
        val text = app.get(url).text
        return parse(text)
    }

    fun parse(json: String): List<PluginManifestItem> {
        val trimmed = json.trim()
        if (trimmed.isEmpty()) return emptyList()
        val items: List<PluginManifestItem> = when {
            trimmed.startsWith("[") -> mapper.readValue(trimmed)
            else -> {
                val tree = mapper.readTree(trimmed)
                when {
                    tree.isArray -> mapper.convertValue(
                        tree,
                        mapper.typeFactory.constructCollectionType(List::class.java, PluginManifestItem::class.java),
                    )
                    tree.has("plugins") -> mapper.convertValue(
                        tree.get("plugins"),
                        mapper.typeFactory.constructCollectionType(List::class.java, PluginManifestItem::class.java),
                    )
                    else -> emptyList()
                }
            }
        }
        return items.filter { it.id.isNotBlank() || it.name.isNotBlank() }
            .map { item ->
                item.copy(
                    id = item.id.ifBlank { item.name.lowercase().replace("\\s+".toRegex(), "_") },
                    name = item.name.ifBlank { item.id },
                    lang = item.displayLang,
                )
            }
    }

    fun languageCounts(items: List<PluginManifestItem>): Map<String, Int> {
        val map = linkedMapOf<String, Int>()
        for (item in items) {
            val lang = item.displayLang.ifBlank { "Multi" }
            map[lang] = (map[lang] ?: 0) + 1
        }
        return map
    }
}
