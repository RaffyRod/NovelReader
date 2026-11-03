package com.example.quicknovel

import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.ui.search.HomeViewModel2
import com.lagradost.quicknovel.ui.search.SearchProviderScope
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchProviderScopeTest {
    private class FakeApi(
        override val name: String,
        override val mainUrl: String,
        override val lang: String = "en",
        override val hasMainPage: Boolean = true,
    ) : MainAPI()

    private val wattpad = FakeApi("Wattpad", "https://www.wattpad.com")
    private val royal = FakeApi("Royal Road", "https://www.royalroad.com")
    private val sky = FakeApi("SkyNovels", "https://skynovels.net", "es")
    private val wuxia = FakeApi("WuxiaBox", "https://wuxiabox.com")
    private val all = listOf(wattpad, royal, sky, wuxia)

    @Test
    fun resolve_scopesToWattpad_byName() {
        val r = SearchProviderScope.resolve("Wattpad", all)
        assertTrue(r.scopedToProvider)
        assertEquals(listOf("Wattpad"), r.apis.map { it.name })
        assertEquals("", r.novelQuery)
    }

    @Test
    fun resolve_scopesToWattpad_byHost() {
        val r = SearchProviderScope.resolve("wattpad.com", all)
        assertTrue(r.scopedToProvider)
        assertEquals(listOf("Wattpad"), r.apis.map { it.name })
    }

    @Test
    fun resolve_scopesTypoPrefix_wattap() {
        val r = SearchProviderScope.resolve("wattap", all)
        assertTrue(r.scopedToProvider)
        assertEquals(listOf("Wattpad"), r.apis.map { it.name })
        assertEquals("", r.novelQuery)
    }

    @Test
    fun resolve_wattpadPlusNovelKeepsNovelQuery() {
        val r = SearchProviderScope.resolve("Wattpad romance", all)
        assertTrue(r.scopedToProvider)
        assertEquals(listOf("Wattpad"), r.apis.map { it.name })
        assertEquals("romance", r.novelQuery)
    }

    @Test
    fun resolve_genericNovelQuery_keepsAllProviders() {
        val r = SearchProviderScope.resolve("Mother of Learning", all)
        assertFalse(r.scopedToProvider)
        assertEquals(4, r.apis.size)
        assertEquals("Mother of Learning", r.novelQuery)
    }

    @Test
    fun filterGrid_byPageHost_keepsOnlyMatchingTiles() {
        val filtered = SearchProviderScope.filterGrid(all, "wuxiabox.com")
        assertEquals(listOf("WuxiaBox"), filtered.map { it.name })
    }

    @Test
    fun filterGrid_byProviderName_prefix() {
        val filtered = SearchProviderScope.filterGrid(all, "Sky")
        assertEquals(listOf("SkyNovels"), filtered.map { it.name })
    }

    @Test
    fun filterGrid_blank_returnsExactlyAll() {
        assertEquals(all.map { it.name }, SearchProviderScope.filterGrid(all, "").map { it.name })
        assertEquals(all.map { it.name }, SearchProviderScope.filterGrid(all, "   ").map { it.name })
    }

    @Test
    fun filterApis_enabledNames_exactCount_noMissing() {
        val vm = HomeViewModel2()
        val apis = all.toImmutableList()
        val names = persistentSetOf("Wattpad", "Royal Road", "SkyNovels", "WuxiaBox")
        val filtered = vm.filterApis(apis, names, persistentSetOf("es"))
        assertEquals(names.sorted(), filtered.map { it.name }.sorted())
        assertEquals(4, filtered.size)
    }

    @Test
    fun matchesIdentity_doesNotConfuseShortQueries() {
        assertFalse(SearchProviderScope.matchesIdentity(wattpad, "wa"))
        assertTrue(SearchProviderScope.matchesIdentity(wattpad, "watt"))
    }

    @Test
    fun homeViewModel_resolveNovelSearch_respectsEnabledNames() {
        val vm = HomeViewModel2()
        val apis = all.toImmutableList()
        val scoped = vm.resolveNovelSearch(
            "Wattpad",
            apis,
            names = persistentSetOf("Wattpad", "Royal Road", "SkyNovels"),
            languages = persistentSetOf("en", "es"),
        )
        assertEquals(listOf("Wattpad"), scoped.apis.map { it.name })
        val onlyRoyal = vm.resolveNovelSearch(
            "Wattpad",
            apis,
            names = persistentSetOf("Royal Road"),
            languages = persistentSetOf("en"),
        )
        assertEquals(listOf("Royal Road"), onlyRoyal.apis.map { it.name })
        assertFalse(onlyRoyal.scopedToProvider)
    }
}
