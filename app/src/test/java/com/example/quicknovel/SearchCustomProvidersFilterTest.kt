package com.example.quicknovel

import com.lagradost.quicknovel.MainAPI
import com.lagradost.quicknovel.ui.search.HomeViewModel2
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCustomProvidersFilterTest {
    private class FakeApi(
        override val name: String,
        override val lang: String,
    ) : MainAPI()

    @Test
    fun filterApis_includesCustomWhenNameAndLangAllowed() {
        val vm = HomeViewModel2()
        val apis = listOf(
            FakeApi("Royal Road", "en"),
            FakeApi("SkyNovels Custom", "es"),
            FakeApi("AllNovel", "en"),
        ).toImmutableList()

        val filtered = vm.filterApis(
            apis,
            names = persistentSetOf("Royal Road", "SkyNovels Custom", "AllNovel"),
            languages = persistentSetOf("es"),
        )

        assertEquals(listOf("SkyNovels Custom"), filtered.map { it.name })
    }

    @Test
    fun filterApis_emptyFilters_returnsNone() {
        val vm = HomeViewModel2()
        val apis = listOf(FakeApi("X", "es")).toImmutableList()
        assertTrue(vm.filterApis(apis, persistentSetOf(), persistentSetOf("es")).isEmpty())
        assertTrue(vm.filterApis(apis, persistentSetOf("X"), persistentSetOf()).isEmpty())
    }

    @Test
    fun filterApis_keepsAddedCustomProviderWhenEnabled() {
        val vm = HomeViewModel2()
        val apis = listOf(
            FakeApi("Royal Road", "en"),
            FakeApi("Mi Blog Custom", "es"),
        ).toImmutableList()
        val filtered = vm.filterApis(
            apis,
            names = persistentSetOf("Royal Road", "Mi Blog Custom"),
            languages = persistentSetOf("es", "en"),
        )
        assertEquals(listOf("Royal Road", "Mi Blog Custom"), filtered.map { it.name })
    }

    @Test
    fun apisForSearch_keepsEnglishWhenSelectedEvenIfLangFilterIsSpanishOnly() {
        val vm = HomeViewModel2()
        val apis = listOf(
            FakeApi("Royal Road", "en"),
            FakeApi("AllNovel", "en"),
            FakeApi("SkyNovels", "es"),
        ).toImmutableList()
        val filtered = vm.apisForSearch(
            apis,
            names = persistentSetOf("Royal Road", "AllNovel", "SkyNovels"),
            languages = persistentSetOf("es"),
        )
        assertEquals(
            listOf("Royal Road", "AllNovel", "SkyNovels"),
            filtered.map { it.name },
        )
    }
}
