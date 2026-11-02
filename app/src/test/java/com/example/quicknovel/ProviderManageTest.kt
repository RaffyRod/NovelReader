package com.example.quicknovel

import com.lagradost.quicknovel.util.ProviderManage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderManageTest {
    private val rows = listOf(
        ProviderManage.Row(
            key = "custom:A",
            title = "Alpha Site",
            subtitle = "https://a.test",
            kind = ProviderManage.Kind.CUSTOM,
        ),
        ProviderManage.Row(
            key = "plugin:b",
            title = "Beta Novel",
            subtitle = "beta.test · JS",
            kind = ProviderManage.Kind.PLUGIN,
        ),
        ProviderManage.Row(
            key = "plugin:c",
            title = "Gamma",
            subtitle = "g.test · Madara",
            kind = ProviderManage.Kind.PLUGIN,
        ),
    )

    @Test
    fun filter_matchesNameOnly() {
        assertEquals(1, ProviderManage.filter(rows, "alpha").size)
        assertEquals(1, ProviderManage.filter(rows, "Beta").size)
        assertTrue(ProviderManage.filter(rows, "https://a.test").isEmpty())
        assertEquals(3, ProviderManage.filter(rows, "").size)
    }

    @Test
    fun paging_coversAllWithoutHardCap() {
        val many = (1..130).map {
            ProviderManage.Row(
                key = "p$it",
                title = "Provider $it",
                subtitle = "x",
                kind = ProviderManage.Kind.PLUGIN,
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
