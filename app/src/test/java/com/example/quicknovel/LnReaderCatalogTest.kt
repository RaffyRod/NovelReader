package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginManifestClient
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginTemplateHints
import com.lagradost.quicknovel.plugins.PluginTemplateKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Validates against the official LNReader v3.0.0 plugins.min.json (bundled test resource).
 * Ensures Madara CSS / wp-manga is never invented for every provider.
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
    fun madaraTagged_getMadaraConfig_andWpMangaQuery() {
        val madara = catalog.filter {
            PluginTemplateHints.detect(it) == PluginTemplateKind.MADARA
        }
        assertTrue(madara.isNotEmpty())
        madara.take(20).forEach { item ->
            val config = PluginInstaller.configFromManifest(item)
            assertNotNull("${item.id} should map to Madara CSS", config)
            assertTrue(
                "${item.id} should use wp-manga search",
                config!!.searchUrl.contains("wp-manga"),
            )
        }
    }

    @Test
    fun nonMadara_neverGetWpMangaQuery() {
        val nonMadara = catalog.filter {
            PluginTemplateHints.detect(it) != PluginTemplateKind.MADARA
        }
        assertTrue(nonMadara.size > 100)
        nonMadara.forEach { item ->
            val kind = PluginTemplateHints.detect(item)
            val config = PluginInstaller.configFromManifest(item)
            when (kind) {
                PluginTemplateKind.BLOGGER -> {
                    assertNotNull(config)
                    assertFalse(
                        "${item.id} blogger must not use wp-manga",
                        config!!.searchUrl.contains("wp-manga"),
                    )
                }
                PluginTemplateKind.UNSUPPORTED_JS -> {
                    assertNull(
                        "${item.id} must not invent Madara selectors",
                        config,
                    )
                }
                PluginTemplateKind.MADARA -> error("filtered out")
            }
        }
    }

    @Test
    fun lightnovelwp_isNotTreatedAsMadara() {
        val lnwp = catalog.filter {
            (it.url.contains("[lightnovelwp]", ignoreCase = true)) ||
                (it.iconUrl.contains("multisrc/lightnovelwp", ignoreCase = true))
        }
        assertTrue(lnwp.isNotEmpty())
        lnwp.forEach { item ->
            assertEquals(
                "${item.id} lightnovelwp must stay JS-only (no Madara overwrite)",
                PluginTemplateKind.UNSUPPORTED_JS,
                PluginTemplateHints.detect(item),
            )
            assertNull(PluginInstaller.configFromManifest(item))
        }
    }

    @Test
    fun jsBodyMentionsMadara_stillDoesNotOverwrite() {
        val plain = catalog.first {
            !it.url.contains("[madara]", ignoreCase = true) &&
                !it.iconUrl.contains("multisrc/madara", ignoreCase = true)
        }
        val kind = PluginTemplateHints.detect(
            plain,
            jsSource = "import { Madara } from '../../multisrc/madara/Madara'; [madara]",
        )
        assertEquals(PluginTemplateKind.UNSUPPORTED_JS, kind)
        assertNull(PluginInstaller.configFromManifest(plain, jsSource = "Madara helper"))
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
}
