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
    fun query_matchesNameSiteIdLangAndHost() {
        assertTrue(PluginCatalogFilter.matchesQuery(sky, "sky"))
        assertTrue(PluginCatalogFilter.matchesQuery(sky, "skynovels.net"))
        assertTrue(PluginCatalogFilter.matchesQuery(sky, "Español"))
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
}
