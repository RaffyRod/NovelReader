package com.example.quicknovel

import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.util.ProviderManage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderManageTest {
    private val alphaConfig = CustomProviderConfig(
        name = "Alpha Site",
        baseUrl = "https://a.test",
        searchUrl = "/?s={query}",
        resultItem = "a",
        novelTitle = "h1",
        chapterLink = "a",
        content = "div",
    )
    private val betaPlugin = InstalledPlugin(
        id = "b",
        name = "Beta Novel",
        site = "https://beta.test/novels",
        templateKind = "css",
    )
    private val rows = listOf(
        ProviderManage.Row(
            key = "builtin:Sky",
            title = "SkyNovels",
            subtitle = "Built-in",
            kind = ProviderManage.Kind.BUILTIN,
            searchName = "SkyNovels",
            activeInSearch = true,
            canToggleSearch = true,
        ),
        ProviderManage.Row(
            key = "custom:A",
            title = "Alpha Site",
            subtitle = "a.test · Proveedor",
            kind = ProviderManage.Kind.CUSTOM,
            searchName = "Alpha Site",
            activeInSearch = false,
            canToggleSearch = true,
            config = alphaConfig,
        ),
        ProviderManage.Row(
            key = "plugin:b",
            title = "Beta Novel",
            subtitle = "beta.test · Plugin",
            kind = ProviderManage.Kind.PLUGIN,
            searchName = "",
            activeInSearch = false,
            canToggleSearch = false,
            plugin = betaPlugin,
        ),
        ProviderManage.Row(
            key = "plugin:c",
            title = "Gamma",
            subtitle = "g.test · Madara",
            kind = ProviderManage.Kind.PLUGIN,
            searchName = "Gamma",
            activeInSearch = true,
            canToggleSearch = true,
        ),
    )

    @Test
    fun filter_matchesNameOrSiteHost() {
        assertEquals(1, ProviderManage.filter(rows, "alpha").size)
        assertEquals(1, ProviderManage.filter(rows, "Beta").size)
        assertEquals(listOf("Alpha Site"), ProviderManage.filter(rows, "a.test").map { it.title })
        assertEquals(listOf("Alpha Site"), ProviderManage.filter(rows, "https://a.test").map { it.title })
        assertEquals(listOf("Beta Novel"), ProviderManage.filter(rows, "beta.test").map { it.title })
        // Substring trap: a.test must not match beta.test
        assertFalse(com.lagradost.quicknovel.plugins.PluginHubUi.siteMatches("beta.test", "a.test"))
        assertEquals(4, ProviderManage.filter(rows, "").size)
        assertTrue(ProviderManage.filter(rows, "zzz-missing").isEmpty())
    }

    @Test
    fun filterTab_activeInstalledAll() {
        assertEquals(2, ProviderManage.filterTab(rows, ProviderManage.Tab.ACTIVE).size)
        assertEquals(3, ProviderManage.filterTab(rows, ProviderManage.Tab.INSTALLED).size)
        assertEquals(4, ProviderManage.filterTab(rows, ProviderManage.Tab.ALL).size)
        assertTrue(
            ProviderManage.filterTab(rows, ProviderManage.Tab.INSTALLED)
                .none { it.kind == ProviderManage.Kind.BUILTIN },
        )
    }

    @Test
    fun paging_coversAllWithoutHardCap() {
        val many = (1..130).map {
            ProviderManage.Row(
                key = "p$it",
                title = "Provider $it",
                subtitle = "x",
                kind = ProviderManage.Kind.PLUGIN,
                searchName = "Provider $it",
                activeInSearch = false,
                canToggleSearch = true,
            )
        }
        assertEquals(3, ProviderManage.pageCount(many.size))
        assertEquals(60, ProviderManage.pageSlice(many, 0).size)
        assertEquals(60, ProviderManage.pageSlice(many, 1).size)
        assertEquals(10, ProviderManage.pageSlice(many, 2).size)
        assertEquals("Provider 61", ProviderManage.pageSlice(many, 1).first().title)
    }

    @Test
    fun parseGoToPage_emptyIsNull() {
        assertEquals(null, ProviderManage.parseGoToPage("", 5))
        assertEquals(2, ProviderManage.parseGoToPage("3", 5))
        assertFalse(ProviderManage.matchesName(rows[0], "zzz"))
    }
}
