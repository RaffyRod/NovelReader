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
        val plugin = InstalledPlugin(
            id = "x",
            name = "X",
            cssMapped = true,
            providerName = "X",
            templateKind = "madara",
        )
        assertEquals("Madara", PluginHubUi.kindLabel(plugin))
    }

    @Test
    fun kindLabel_bloggerWhenTemplateKindBlogger() {
        val plugin = InstalledPlugin(
            id = "b",
            name = "Blog",
            cssMapped = true,
            providerName = "Blog",
            templateKind = "blogger",
        )
        assertEquals("Blogger", PluginHubUi.kindLabel(plugin))
    }

    @Test
    fun metaLine_marksPluginsNotInSearch() {
        val plugin = InstalledPlugin(
            id = "x",
            name = "X",
            cssMapped = false,
            providerName = "",
            templateKind = "js",
        )
        assertTrue(PluginHubUi.metaLine(plugin, notInSearchLabel = "sin búsqueda").contains("sin búsqueda"))
        assertTrue(PluginHubUi.metaLine(plugin).contains("not in Search"))
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

    @Test
    fun filterInstalled_matchesNameOrHost() {
        val plugins = listOf(
            InstalledPlugin(id = "1", name = "Bright Novel", localName = "Bright Novel", site = "https://bright.test/"),
            InstalledPlugin(id = "2", name = "Asian", localName = "Asian Novel", site = "https://asian.example/"),
            InstalledPlugin(id = "3", name = "Other", localName = "Other", site = "https://novelfull.com/"),
        )
        assertEquals(1, PluginHubUi.filterInstalled(plugins, "bright").size)
        assertEquals(1, PluginHubUi.filterInstalled(plugins, "asian.example").size)
        assertEquals(1, PluginHubUi.filterInstalled(plugins, "NovelFull").size)
        assertEquals(3, PluginHubUi.filterInstalled(plugins, "").size)
        assertEquals(0, PluginHubUi.filterInstalled(plugins, "zzz").size)
    }
}
