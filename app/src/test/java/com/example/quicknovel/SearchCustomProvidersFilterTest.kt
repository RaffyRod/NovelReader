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
        override val hasMainPage: Boolean = true,
    ) : MainAPI()

    @Test
    fun filterApis_selectedNamesWinOverLanguage() {
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

        // Buscar grid must list every Fuentes-enabled name, including English when ES-only.
        assertEquals(
            listOf("Royal Road", "SkyNovels Custom", "AllNovel"),
            filtered.map { it.name },
        )
    }

    @Test
    fun filterApis_emptyNames_filtersByLanguageOnly() {
        val vm = HomeViewModel2()
        val apis = listOf(
            FakeApi("Royal Road", "en"),
            FakeApi("SkyNovels", "es"),
        ).toImmutableList()
        assertEquals(
            listOf("SkyNovels"),
            vm.filterApis(apis, persistentSetOf(), persistentSetOf("es")).map { it.name },
        )
        assertEquals(
            listOf("Royal Road"),
            vm.filterApis(apis, persistentSetOf("Royal Road"), persistentSetOf()).map { it.name },
        )
        assertTrue(vm.filterApis(apis, persistentSetOf(), persistentSetOf()).isEmpty())
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
    fun filterApis_and_apisForSearch_agreeWhenNamesSelected() {
        val vm = HomeViewModel2()
        val apis = listOf(
            FakeApi("Royal Road", "en"),
            FakeApi("AllNovel", "en"),
            FakeApi("SkyNovels", "es"),
            FakeApi("Hidden", "zh"),
        ).toImmutableList()
        val names = persistentSetOf("Royal Road", "AllNovel", "SkyNovels")
        val languages = persistentSetOf("es")
        assertEquals(
            vm.apisForSearch(apis, names, languages).map { it.name },
            vm.filterApis(apis, names, languages).map { it.name },
        )
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
