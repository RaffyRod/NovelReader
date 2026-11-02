package com.example.quicknovel

import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginCatalogFilter
import com.lagradost.quicknovel.plugins.PluginManifestItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginCatalogFilterTest {
    private val sky = PluginManifestItem(
        id = "skynovels",
        name = "SkyNovels",
        site = "https://skynovels.net/",
        lang = "Español",
        version = "1.1.0",
        url = "https://cdn.example/SkyNovels.js",
    )
    private val madara = PluginManifestItem(
        id = "lightnoveldaily",
        name = "LightNovelDaily",
        site = "https://lightnoveldaily.com/",
        lang = "Español",
        version = "1.0.2",
        url = "https://cdn.example/LightNovelDaily[madara].js",
    )
    private val blogger = PluginManifestItem(
        id = "blog-x",
        name = "BlogX",
        site = "https://blogx.blogspot.com/",
        lang = "English",
        version = "1.0.0",
        url = "https://cdn.example/BlogX.js",
    )
    private val catalog = listOf(sky, madara, blogger)

    @Test
    fun query_matchesNameOnly() {
        assertTrue(PluginCatalogFilter.matchesQuery(sky, "sky"))
        assertTrue(PluginCatalogFilter.matchesQuery(sky, "SkyNovels"))
        assertFalse(PluginCatalogFilter.matchesQuery(sky, "skynovels.net"))
        assertFalse(PluginCatalogFilter.matchesQuery(sky, "Español"))
        assertFalse(PluginCatalogFilter.matchesQuery(sky, "madara"))
    }

    @Test
    fun status_newUpdatesInstalled() {
        val installed = mapOf(
            "lightnoveldaily" to InstalledPlugin(
                id = "lightnoveldaily",
                name = "LightNovelDaily",
                version = "1.0.1",
            ),
            "blog-x" to InstalledPlugin(
                id = "blog-x",
                name = "BlogX",
                version = "1.0.0",
            ),
        )
        assertEquals(
            listOf(sky),
            PluginCatalogFilter.filter(catalog, status = PluginCatalogFilter.Status.NEW, installedById = installed),
        )
        assertEquals(
            listOf(madara),
            PluginCatalogFilter.filter(catalog, status = PluginCatalogFilter.Status.UPDATES, installedById = installed),
        )
        assertEquals(
            listOf(madara, blogger),
            PluginCatalogFilter.filter(catalog, status = PluginCatalogFilter.Status.INSTALLED, installedById = installed),
        )
    }

    @Test
    fun kind_madaraBloggerJs() {
        assertEquals(
            listOf(madara),
            PluginCatalogFilter.filter(catalog, kind = PluginCatalogFilter.Kind.MADARA),
        )
        assertEquals(
            listOf(blogger),
            PluginCatalogFilter.filter(catalog, kind = PluginCatalogFilter.Kind.BLOGGER),
        )
        assertEquals(
            listOf(sky),
            PluginCatalogFilter.filter(catalog, kind = PluginCatalogFilter.Kind.JS),
        )
    }

    @Test
    fun paging_hasNoHardCapOf100() {
        val many = (1..250).map {
            PluginManifestItem(
                id = "p$it",
                name = "Plugin $it",
                site = "https://example.com/$it",
                lang = "en",
                version = "1.0.0",
                url = "https://cdn.example/p$it.js",
            )
        }
        assertEquals(5, PluginCatalogFilter.pageCount(many.size))
        assertEquals(60, PluginCatalogFilter.pageSlice(many, 0).size)
        assertEquals(60, PluginCatalogFilter.pageSlice(many, 3).size)
        assertEquals(10, PluginCatalogFilter.pageSlice(many, 4).size)
        assertEquals("Plugin 61", PluginCatalogFilter.pageSlice(many, 1).first().name)
    }

    @Test
    fun parseGoToPage_emptyOrInvalid_isNull() {
        assertEquals(null, PluginCatalogFilter.parseGoToPage("", 5))
        assertEquals(null, PluginCatalogFilter.parseGoToPage("   ", 5))
        assertEquals(null, PluginCatalogFilter.parseGoToPage(null, 5))
        assertEquals(null, PluginCatalogFilter.parseGoToPage("abc", 5))
        assertEquals(null, PluginCatalogFilter.parseGoToPage("3", 0))
    }

    @Test
    fun parseGoToPage_returnsZeroBased_andCoercesBounds() {
        // Empty dialog + type "3" → page 3 (index 2), never "13" from a prefilled 1.
        assertEquals(2, PluginCatalogFilter.parseGoToPage("3", 5))
        assertEquals(0, PluginCatalogFilter.parseGoToPage("1", 5))
        assertEquals(0, PluginCatalogFilter.parseGoToPage("0", 5))
        assertEquals(4, PluginCatalogFilter.parseGoToPage("99", 5))
        assertEquals(12, PluginCatalogFilter.parseGoToPage("13", 13))
    }
}
