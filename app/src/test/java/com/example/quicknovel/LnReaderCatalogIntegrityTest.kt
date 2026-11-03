package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginManifestClient
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginTemplateHints
import com.lagradost.quicknovel.plugins.PluginTemplateKind
import com.lagradost.quicknovel.providers.CustomProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Real LNReader catalog: detect stays tag-based; install maps every site into Search (LN-like).
 */
class LnReaderCatalogIntegrityTest {

    private fun loadCatalog(): List<PluginManifestItem> {
        val stream = requireNotNull(
            javaClass.classLoader!!.getResourceAsStream("plugins.min.json")
        ) { "Missing test resource plugins.min.json" }
        val json = stream.bufferedReader().use { it.readText() }
        val items = PluginManifestClient.parse(json)
        assertTrue("catalog must parse", items.size > 100)
        return items
    }

    private fun countKinds(items: List<PluginManifestItem>): Triple<Int, Int, Int> {
        var madara = 0
        var blogger = 0
        var unsupported = 0
        for (item in items) {
            when (PluginTemplateHints.detect(item)) {
                PluginTemplateKind.MADARA -> madara++
                PluginTemplateKind.BLOGGER -> blogger++
                PluginTemplateKind.UNSUPPORTED_JS -> unsupported++
            }
        }
        return Triple(madara, blogger, unsupported)
    }

    private fun expectedMadaraFromTags(items: List<PluginManifestItem>): Int =
        items.count { item ->
            item.url.contains("[madara]", ignoreCase = true) ||
                item.iconUrl.contains("multisrc/madara", ignoreCase = true) ||
                item.id.contains("[madara]", ignoreCase = true)
        }

    @Test
    fun fullCatalog_detectIsStableAcrossRepeatedPasses() {
        val items = loadCatalog()
        val expectedMadara = expectedMadaraFromTags(items)
        assertTrue("catalog should include Madara tags", expectedMadara > 50)

        val first = countKinds(items)
        assertEquals("madara detect matches catalog tags", expectedMadara, first.first)
        assertTrue("blogger", first.second >= 1)
        assertEquals(
            "unsupported is the remainder",
            items.size - first.first - first.second,
            first.third,
        )
        assertTrue(first.third > first.first)

        repeat(2) { pass ->
            val again = countKinds(items)
            assertEquals("pass ${pass + 2} madara stable", first.first, again.first)
            assertEquals("pass ${pass + 2} blogger stable", first.second, again.second)
            assertEquals("pass ${pass + 2} unsupported stable", first.third, again.third)
        }
    }

    @Test
    fun everySiteUrl_mapsIntoSearch_onInstall() {
        val items = loadCatalog()
        for (item in items) {
            val cfg = PluginInstaller.configFromManifest(item)
            if (item.site.isBlank()) {
                assertNull(item.id, cfg)
            } else {
                assertNotNull(item.id, cfg)
                when (PluginTemplateHints.detect(item)) {
                    PluginTemplateKind.BLOGGER ->
                        assertEquals(item.id, CustomProviderConfig.KIND_BLOGGER, cfg!!.kind)
                    PluginTemplateKind.MADARA -> {
                        assertEquals(item.id, CustomProviderConfig.KIND_CSS, cfg!!.kind)
                        assertEquals(item.id, CustomProviderConfig.DEFAULT_SITE_SEARCH_URL, cfg.searchUrl)
                        assertTrue(item.id, cfg.chaptersViaAjax)
                        assertTrue(item.id, !cfg.searchUrl.contains("wp-manga"))
                    }
                    PluginTemplateKind.UNSUPPORTED_JS -> {
                        assertEquals(item.id, CustomProviderConfig.KIND_CSS, cfg!!.kind)
                        assertEquals(item.id, "/?s={query}", cfg.searchUrl)
                        assertTrue(item.id, !cfg.searchUrl.contains("wp-manga"))
                        assertTrue(item.id, !cfg.chaptersViaAjax)
                    }
                }
            }
        }
    }

    @Test
    fun lightnovelwp_isNotDetectedAsMadaraTag_butStillJoinsSearch() {
        val items = loadCatalog().filter {
            it.url.contains("[lightnovelwp]", ignoreCase = true) ||
                it.iconUrl.contains("multisrc/lightnovelwp", ignoreCase = true)
        }
        assertTrue(items.isNotEmpty())
        items.forEach { item ->
            assertEquals(item.id, PluginTemplateKind.UNSUPPORTED_JS, PluginTemplateHints.detect(item))
            if (item.site.isNotBlank()) {
                val cfg = PluginInstaller.configFromManifest(item)
                assertNotNull(item.id, cfg)
                assertTrue(item.id, !cfg!!.searchUrl.contains("wp-manga"))
                assertTrue(item.id, !cfg.chaptersViaAjax)
            }
        }
    }
}
