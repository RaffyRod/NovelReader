package com.example.quicknovel

import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginHubUi
import com.lagradost.quicknovel.plugins.PluginManifestItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginHubUiTest {
    @Test
    fun kindLabel_jsWhenNotCssMapped() {
        val plugin = InstalledPlugin(id = "x", name = "X", cssMapped = false, providerName = "")
        assertEquals("JS", PluginHubUi.kindLabel(plugin))
    }

    @Test
    fun kindLabel_madaraWhenCssMapped() {
        val plugin = InstalledPlugin(id = "x", name = "X", cssMapped = true, providerName = "X")
        assertEquals("Madara", PluginHubUi.kindLabel(plugin))
    }

    @Test
    fun kindLabel_catalogDetectsMadaraBloggerAndJs() {
        assertEquals(
            "Madara",
            PluginHubUi.kindLabel(
                PluginManifestItem(
                    id = "m",
                    name = "M",
                    url = "https://cdn.example/Site[madara].js",
                ),
            ),
        )
        assertEquals(
            "Blogger",
            PluginHubUi.kindLabel(
                PluginManifestItem(
                    id = "blog-site",
                    name = "Blog",
                    site = "https://novel.blogspot.com/",
                    url = "https://cdn.example/blog.js",
                ),
            ),
        )
        assertEquals(
            "JS",
            PluginHubUi.kindLabel(
                PluginManifestItem(id = "plain", name = "Plain", url = "https://cdn.example/plain.js"),
            ),
        )
    }

    @Test
    fun metaLine_includesUpdateMarker() {
        val plugin = InstalledPlugin(
            id = "x",
            name = "X",
            localName = "AllNovel",
            lang = "English",
            version = "1.0.0",
            latestVersion = "1.0.1",
            cssMapped = false,
            providerName = "",
        )
        val meta = PluginHubUi.metaLine(plugin)
        assertTrue(meta.contains("v1.0.0"))
        assertTrue(meta.contains("English"))
        assertTrue(meta.contains("JS"))
        assertTrue(meta.contains("1.0.1"))
    }

    @Test
    fun catalogMetaLine_formatsVersionLangAndKind() {
        val item = PluginManifestItem(
            id = "x",
            name = "X",
            lang = "Español",
            version = "2.3.0",
            url = "https://cdn.example/X[madara].js",
        )
        assertEquals("v2.3.0 · Español · Madara", PluginHubUi.catalogMetaLine(item))
    }

    @Test
    fun siteHost_stripsScheme() {
        val plugin = InstalledPlugin(
            id = "x",
            name = "X",
            site = "https://novelfull.com/",
        )
        assertEquals("novelfull.com", PluginHubUi.siteHost(plugin))
        assertEquals("allnovelread.com", PluginHubUi.siteHost("http://allnovelread.com"))
        assertEquals("", PluginHubUi.siteHost("   "))
    }
}
