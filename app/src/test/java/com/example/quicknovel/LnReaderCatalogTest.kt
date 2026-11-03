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
import org.junit.BeforeClass
import org.junit.Test

/**
 * Official LNReader v3 catalog: install with a site URL always maps into Search (LN-like).
 */
class LnReaderCatalogTest {

    companion object {
        private lateinit var catalog: List<PluginManifestItem>

        @JvmStatic
        @BeforeClass
        fun loadCatalog() {
            val stream = LnReaderCatalogTest::class.java.classLoader!!
                .getResourceAsStream("plugins.min.json")
            assertNotNull("plugins.min.json missing from test resources", stream)
            catalog = PluginManifestClient.parse(stream!!.bufferedReader().readText())
        }
    }

    @Test
    fun parse_officialCatalog_hasHundredsOfPlugins() {
        assertTrue("expected ~328 plugins, got ${catalog.size}", catalog.size >= 300)
        assertTrue(catalog.all { it.id.isNotBlank() && it.name.isNotBlank() })
    }

    @Test
    fun parse_officialCatalog_threeTimes_stable() {
        val json = LnReaderCatalogTest::class.java.classLoader!!
            .getResourceAsStream("plugins.min.json")!!.bufferedReader().readText()
        val a = PluginManifestClient.parse(json)
        val b = PluginManifestClient.parse(json)
        val c = PluginManifestClient.parse(json)
        assertEquals(a.size, b.size)
        assertEquals(b.size, c.size)
        assertEquals(a.map { it.id }, c.map { it.id })
    }

    @Test
    fun anyPluginWithSite_autoMapsCss_forSearch() {
        val withSite = catalog.filter { it.site.isNotBlank() }
        assertTrue(withSite.size > 100)
        withSite.take(40).forEach { item ->
            val cfg = PluginInstaller.configFromManifest(item)
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

    @Test
    fun lightnovelwp_detectStaysJs_butStillMapsSearchBridge() {
        val lnwp = catalog.filter {
            (it.url.contains("[lightnovelwp]", ignoreCase = true)) ||
                (it.iconUrl.contains("multisrc/lightnovelwp", ignoreCase = true))
        }
        assertTrue(lnwp.isNotEmpty())
        lnwp.forEach { item ->
            assertEquals(
                PluginTemplateKind.UNSUPPORTED_JS,
                PluginTemplateHints.detect(item),
            )
            if (item.site.isNotBlank()) {
                assertNotNull(item.id, PluginInstaller.configFromManifest(item))
            }
        }
    }

    @Test
    fun jsBodyMentionsMadara_doesNotChangeDetectKind() {
        val plain = catalog.first {
            !it.url.contains("[madara]", ignoreCase = true) &&
                !it.iconUrl.contains("multisrc/madara", ignoreCase = true)
        }
        val kind = PluginTemplateHints.detect(
            plain,
            jsSource = "import { Madara } from '../../multisrc/madara/Madara'; [madara]",
        )
        assertEquals(PluginTemplateKind.UNSUPPORTED_JS, kind)
    }

    @Test
    fun selectAll_idsCoverFullLanguageSlice() {
        val spanish = catalog.filter {
            it.displayLang.contains("Español", ignoreCase = true) ||
                it.displayLang.contains("Spanish", ignoreCase = true)
        }
        val ids = com.lagradost.quicknovel.plugins.PluginSelectAll.allIds(spanish)
        assertEquals(spanish.size, ids.size)
        assertEquals(spanish.map { it.id }.toSet(), ids)
    }

    @Test
    fun missingSite_cannotMap() {
        val orphan = PluginManifestItem(
            id = "no-site",
            name = "No Site",
            site = "",
            url = "https://cdn.example/x.js",
        )
        assertNull(PluginInstaller.configFromManifest(orphan))
    }
}
