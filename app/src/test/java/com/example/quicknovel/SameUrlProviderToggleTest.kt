package com.example.quicknovel

import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.providers.CustomProviderConfig
import com.lagradost.quicknovel.ui.search.HomeViewModel2
import com.lagradost.quicknovel.ui.search.SearchProviderScope
import com.lagradost.quicknovel.util.CustomProviders
import com.lagradost.quicknovel.util.ProviderManage
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Same site URL, distinct display / Search names must toggle independently
 * (Fuentes switches and Buscar enable set).
 */
class SameUrlProviderToggleTest {
    private val sharedUrl = "https://mirror.example"

    private val alpha = CustomProviderConfig(
        name = "Mirror Alpha",
        baseUrl = sharedUrl,
        searchUrl = "/?s={query}",
        resultItem = "a",
        novelTitle = "h1",
        chapterLink = "a",
        content = "div",
    )
    private val beta = CustomProviderConfig(
        name = "Mirror Beta",
        baseUrl = sharedUrl,
        searchUrl = "/?s={query}",
        resultItem = "a",
        novelTitle = "h1",
        chapterLink = "a",
        content = "div",
    )

    private val rows = listOf(
        ProviderManage.Row(
            key = "custom:Mirror Alpha",
            title = "Mirror Alpha",
            subtitle = "mirror.example · Proveedor",
            kind = ProviderManage.Kind.CUSTOM,
            searchName = "Mirror Alpha",
            activeInSearch = true,
            canToggleSearch = true,
            config = alpha,
        ),
        ProviderManage.Row(
            key = "custom:Mirror Beta",
            title = "Mirror Beta",
            subtitle = "mirror.example · Proveedor",
            kind = ProviderManage.Kind.CUSTOM,
            searchName = "Mirror Beta",
            activeInSearch = true,
            canToggleSearch = true,
            config = beta,
        ),
        ProviderManage.Row(
            key = "plugin:p1",
            title = "Mirror Plugin",
            subtitle = "mirror.example · Plugin",
            kind = ProviderManage.Kind.PLUGIN,
            searchName = "Mirror Plugin",
            activeInSearch = true,
            canToggleSearch = true,
        ),
    )

    private class FakeApi(
        override val name: String,
        override val mainUrl: String,
        override val lang: String = "es",
        override val hasMainPage: Boolean = true,
    ) : MainAPI()

    @Test
    fun nextSearchNames_disablingOneKeepsSameUrlSibling() {
        val current = setOf("Mirror Alpha", "Mirror Beta", "Mirror Plugin", "SkyNovels")
        val afterAlphaOff = CustomProviders.nextSearchNames(current, "Mirror Alpha", enabled = false)
        assertFalse(afterAlphaOff.contains("Mirror Alpha"))
        assertTrue(afterAlphaOff.contains("Mirror Beta"))
        assertTrue(afterAlphaOff.contains("Mirror Plugin"))
        assertTrue(afterAlphaOff.contains("SkyNovels"))

        val afterPluginOff = CustomProviders.nextSearchNames(
            afterAlphaOff,
            "Mirror Plugin",
            enabled = false,
        )
        assertFalse(afterPluginOff.contains("Mirror Plugin"))
        assertTrue(afterPluginOff.contains("Mirror Beta"))
        assertEquals(setOf("Mirror Beta", "SkyNovels"), afterPluginOff)
    }

    @Test
    fun withSearchActive_sameUrlRowsStayIndependent() {
        val active = setOf("Mirror Beta", "Mirror Plugin")
        val painted = ProviderManage.withSearchActive(rows, active)
        assertFalse(painted[0].activeInSearch)
        assertTrue(painted[1].activeInSearch)
        assertTrue(painted[2].activeInSearch)
        assertEquals(sharedUrl, painted[0].config?.baseUrl)
        assertEquals(sharedUrl, painted[1].config?.baseUrl)
    }

    @Test
    fun filterApis_and_resolve_keepSiblingWhenOneSameHostDisabled() {
        val vm = HomeViewModel2()
        val apis = listOf(
            FakeApi("Mirror Alpha", sharedUrl),
            FakeApi("Mirror Beta", sharedUrl),
            FakeApi("SkyNovels", "https://skynovels.net"),
        ).toImmutableList()

        val enabled = vm.filterApis(
            apis,
            names = persistentSetOf("Mirror Beta", "SkyNovels"),
            languages = persistentSetOf("es"),
        )
        assertEquals(listOf("Mirror Beta", "SkyNovels"), enabled.map { it.name })

        val scoped = SearchProviderScope.resolve("mirror.example", enabled)
        assertTrue(scoped.scopedToProvider)
        assertEquals(listOf("Mirror Beta"), scoped.apis.map { it.name })
    }

    @Test
    fun resolve_multipleSameHostMirrors_scopesOnlyToThem() {
        val apis = listOf(
            FakeApi("Mirror Alpha", sharedUrl),
            FakeApi("Mirror Beta", sharedUrl),
            FakeApi("SkyNovels", "https://skynovels.net"),
        )
        val scoped = SearchProviderScope.resolve("mirror.example", apis)
        assertTrue(scoped.scopedToProvider)
        assertEquals(listOf("Mirror Alpha", "Mirror Beta"), scoped.apis.map { it.name })
    }
}
