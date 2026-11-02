package com.example.quicknovel

import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginLangs
import com.lagradost.quicknovel.plugins.PluginManifestClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginManifestClientTest {
    @Test
    fun parsesLnReaderStyleArray() {
        val json = """
            [
              {"id":"ao3","name":"Archive Of Our Own","site":"https://archiveofourown.org/","lang":"English","version":"1.0.5","url":"https://example.com/ao3.js","iconUrl":""},
              {"id":"nova","name":"NOVA","site":"https://novelasligeras.net","lang":"Español","version":"1.1.1","url":"https://example.com/nova.js","iconUrl":""}
            ]
        """.trimIndent()
        val items = PluginManifestClient.parse(json)
        assertEquals(2, items.size)
        assertEquals("ao3", items[0].id)
        assertEquals("Español", items[1].lang)
        val counts = PluginManifestClient.languageCounts(items)
        assertEquals(1, counts["English"])
        assertEquals(1, counts["Español"])
        val ordered = PluginLangs.ordered(counts)
        assertEquals("English", ordered[0].first)
        assertEquals("Español", ordered[1].first)
    }

    @Test
    fun parsesWrappedPluginsObject_andIgnoresUnknownFields() {
        val json = """
            {
              "version": 3,
              "plugins": [
                {"id":"x","name":"X","site":"https://x.test","lang":"English","version":"2","url":"https://x.js","extra":true}
              ]
            }
        """.trimIndent()
        val items = PluginManifestClient.parse(json)
        assertEquals(1, items.size)
        assertEquals("x", items[0].id)
        assertEquals("2", items[0].version)
    }

    @Test
    fun emptyOrBlank_returnsEmptyList() {
        assertTrue(PluginManifestClient.parse("").isEmpty())
        assertTrue(PluginManifestClient.parse("   ").isEmpty())
        assertTrue(PluginManifestClient.parse("[]").isEmpty())
    }

    @Test
    fun fillsMissingIdFromName() {
        val items = PluginManifestClient.parse(
            """[{"name":"My Site","site":"https://a.test","lang":"Español","version":"1","url":""}]"""
        )
        assertEquals(1, items.size)
        assertEquals("my_site", items[0].id)
        assertEquals("My Site", items[0].name)
    }

    @Test
    fun mapsPriorityLangCodes() {
        assertEquals("en", PluginLangs.codeFor("English"))
        assertEquals("es", PluginLangs.codeFor("Español"))
        assertEquals("es", PluginLangs.codeFor("Spanish"))
        assertTrue(PluginLangs.PRIORITY_LABELS.contains("English"))
    }

    @Test
    fun cleanLabel_stripsBidiMarks_andListLabelUsesLatinForRtl() {
        assertEquals("العربية", PluginLangs.cleanLabel("\u200eالعربية"))
        assertEquals("ar", PluginLangs.codeFor("\u200eالعربية"))
        assertEquals("Arabic", PluginLangs.listLabel("\u200eالعربية"))
        assertEquals("Arabic", PluginLangs.listLabel("Arabic"))
    }

    @Test
    fun ordered_keepsPriorityFirst_thenByCount() {
        val counts = mapOf(
            "العربية" to 40,
            "Español" to 5,
            "English" to 10,
            "Français" to 20,
        )
        val ordered = PluginLangs.ordered(counts)
        assertEquals(listOf("English", "Español", "العربية", "Français"), ordered.map { it.first })
        assertEquals("Arabic", PluginLangs.listLabel(ordered[2].first))
    }

    @Test
    fun installedPlugin_hasUpdateWhenLatestDiffers() {
        val base = InstalledPlugin(
            id = "a",
            name = "A",
            version = "1.0.0",
            latestVersion = "1.0.0",
        )
        assertFalse(base.hasUpdate)
        assertTrue(base.copy(latestVersion = "1.0.1").hasUpdate)
    }
}
