package com.example.quicknovel

import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginHubUi
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginSelectAll
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Soft-card catalog select rules: new / locked installed / selectable update.
 */
class PluginCatalogSelectRulesTest {
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
    private val allNovel = PluginManifestItem(
        id = "allnovelread",
        name = "AllNovelRead",
        site = "https://allnovelread.com/",
        lang = "Español",
        version = "1.1.12",
        url = "https://cdn.example/AllNovelRead.js",
    )

    private val catalog = listOf(sky, madara, allNovel)

    @Test
    fun newPlugin_isSelectableAndNotTreatedAsUpdate() {
        val installed = emptyMap<String, InstalledPlugin>()
        assertTrue(PluginSelectAll.isSelectable(sky, installed))
        assertFalse(
            PluginSelectAll.hasCatalogUpdate(
                InstalledPlugin(id = "skynovels", name = "SkyNovels", version = "1.1.0"),
                sky,
            ),
        )
    }

    @Test
    fun sameVersionInstalled_isLocked() {
        val installed = mapOf(
            "allnovelread" to InstalledPlugin(
                id = "allnovelread",
                name = "AllNovelRead",
                version = "1.1.12",
            ),
        )
        assertFalse(PluginSelectAll.isSelectable(allNovel, installed))
        assertEquals(
            setOf("skynovels", "lightnoveldaily"),
            PluginSelectAll.selectableIds(catalog, installed),
        )
    }

    @Test
    fun newerCatalogVersion_isSelectableAsUpdate() {
        val installed = mapOf(
            "lightnoveldaily" to InstalledPlugin(
                id = "lightnoveldaily",
                name = "LightNovelDaily",
                version = "1.0.1",
                latestVersion = "1.0.1",
            ),
        )
        assertTrue(PluginSelectAll.hasCatalogUpdate(installed.getValue("lightnoveldaily"), madara))
        assertTrue(PluginSelectAll.isSelectable(madara, installed))
        assertTrue(PluginSelectAll.selectableIds(catalog, installed).contains("lightnoveldaily"))
    }

    @Test
    fun selectAll_includesNewAndUpdates_excludesSameVersion() {
        val installed = mapOf(
            "allnovelread" to InstalledPlugin(id = "allnovelread", name = "AllNovelRead", version = "1.1.12"),
            "lightnoveldaily" to InstalledPlugin(id = "lightnoveldaily", name = "LightNovelDaily", version = "1.0.0"),
        )
        val selected = PluginSelectAll.selectableIds(catalog, installed)
        val chosen = PluginSelectAll.filterSelectable(catalog, selected, installed)
        assertEquals(listOf("skynovels", "lightnoveldaily"), chosen.map { it.id })
        assertFalse(chosen.any { it.id == "allnovelread" })
    }

    @Test
    fun blankRemoteVersion_doesNotCountAsUpdate() {
        val installed = InstalledPlugin(id = "x", name = "X", version = "1.0.0")
        val remote = PluginManifestItem(id = "x", name = "X", version = "  ")
        assertFalse(PluginSelectAll.hasCatalogUpdate(installed, remote))
        assertFalse(PluginSelectAll.isSelectable(remote, mapOf("x" to installed)))
    }

    @Test
    fun catalogSoftCardMeta_matchesHubFamily() {
        assertEquals("JS", PluginHubUi.kindLabel(sky))
        assertEquals("Madara", PluginHubUi.kindLabel(madara))
        assertEquals("skynovels.net", PluginHubUi.siteHost(sky.site))
        assertEquals("v1.1.0 · Español · JS", PluginHubUi.catalogMetaLine(sky))
        assertEquals("v1.0.2 · Español · Madara", PluginHubUi.catalogMetaLine(madara))
    }

    @Test
    fun installDecision_updateUsesOverwrite_newSkipsExisting() {
        val installed = mapOf(
            "lightnoveldaily" to InstalledPlugin(
                id = "lightnoveldaily",
                name = "LightNovelDaily",
                version = "1.0.1",
            ),
            "allnovelread" to InstalledPlugin(
                id = "allnovelread",
                name = "AllNovelRead",
                version = "1.1.12",
            ),
        )
        val chosen = PluginSelectAll.filterSelectable(
            catalog,
            selectedIds = setOf("skynovels", "lightnoveldaily", "allnovelread"),
            installedById = installed,
        )
        val decisions = chosen.map { item ->
            val before = installed[item.id]
            val isUpdate = before != null && PluginSelectAll.hasCatalogUpdate(before, item)
            item.id to (if (isUpdate) "overwrite" else "install_new")
        }
        assertEquals(
            listOf("skynovels" to "install_new", "lightnoveldaily" to "overwrite"),
            decisions,
        )
    }
}
