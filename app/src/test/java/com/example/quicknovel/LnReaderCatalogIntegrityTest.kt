package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginManifestClient
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginTemplateHints
import com.lagradost.quicknovel.plugins.PluginTemplateKind
import com.lagradost.quicknovel.providers.CustomProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uses the real LNReader `plugins.min.json` (v3.0.0) shipped as a test resource.
 * Ensures Madara CSS / `wp-manga` is never invented for untagged plugins.
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

    /** Madara count comes only from `[madara]` / `multisrc/madara` tags — not lightnovelwp. */
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
    fun untaggedPlugins_neverGetWpMangaSearchUrl() {
        val items = loadCatalog()
        for (item in items) {
            val taggedMadara =
                item.url.contains("[madara]", ignoreCase = true) ||
                    item.iconUrl.contains("multisrc/madara", ignoreCase = true) ||
                    item.id.contains("[madara]", ignoreCase = true)
            val config = PluginInstaller.configFromManifest(item)
            if (!taggedMadara && PluginTemplateHints.detect(item) != PluginTemplateKind.BLOGGER) {
                assertNull("${item.id} must not invent CSS", config)
            }
            if (config != null && config.kind == CustomProviderConfig.KIND_CSS) {
                assertTrue(
                    "${item.id} CSS only when Madara tagged",
                    taggedMadara,
                )
                assertTrue(config.searchUrl.contains("wp-manga") || config.searchUrl.contains("{query}"))
            }
        }
    }

    @Test
    fun taggedMadara_getsMadaraTemplate_notBlank() {
        val items = loadCatalog()
        val sample = items.first {
            it.url.contains("[madara]", ignoreCase = true)
        }
        val config = PluginInstaller.configFromManifest(sample)
        assertNotNull(config)
        assertEquals(CustomProviderConfig.KIND_CSS, config!!.kind)
        assertTrue(config.searchUrl.contains("wp-manga"))
        assertFalse(config.baseUrl.isBlank())
        assertEquals(sample.name, config.name)
    }

    @Test
    fun knownJsOnly_likeNovelFire_orUntaggedArabic_stayUnsupported() {
        val items = loadCatalog().associateBy { it.id }
        for (id in listOf("azorafly", "dilartube", "galaxynovels")) {
            val item = items[id] ?: continue
            assertEquals(id, PluginTemplateKind.UNSUPPORTED_JS, PluginTemplateHints.detect(item))
            assertNull(id, PluginInstaller.configFromManifest(item))
        }
    }

    @Test
    fun lightnovelwp_isNotTreatedAsMadara() {
        val items = loadCatalog().filter {
            it.url.contains("[lightnovelwp]", ignoreCase = true) ||
                it.iconUrl.contains("multisrc/lightnovelwp", ignoreCase = true)
        }
        assertTrue(items.isNotEmpty())
        items.forEach { item ->
            assertEquals(item.id, PluginTemplateKind.UNSUPPORTED_JS, PluginTemplateHints.detect(item))
            assertNull(item.id, PluginInstaller.configFromManifest(item))
        }
    }
}
