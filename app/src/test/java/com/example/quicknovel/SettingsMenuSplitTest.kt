package com.example.quicknovel

import com.lagradost.quicknovel.ui.settings.SettingsMenuSplit
import com.lagradost.quicknovel.ui.settings.SettingsMenuTab
import com.mihon.presentation.settings.Preference
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsMenuSplitTest {
    private fun group(
        title: String,
        section: SettingsMenuTab,
    ): Preference.PreferenceGroup = Preference.PreferenceGroup(
        title = title,
        preferenceItems = persistentListOf(),
        section = section,
    )

    private val items = listOf(
        group("General", SettingsMenuTab.CONFIGURATION),
        group("Theme", SettingsMenuTab.CONFIGURATION),
        group("Custom providers", SettingsMenuTab.ADMINISTRATION),
        group("Plugins", SettingsMenuTab.ADMINISTRATION),
        group("App settings", SettingsMenuTab.ADMINISTRATION),
        group("Translation", SettingsMenuTab.CONFIGURATION),
    )

    @Test
    fun forTab_configuration_keepsOnlyDailyGroups() {
        val filtered = SettingsMenuSplit.forTab(items, SettingsMenuTab.CONFIGURATION)
        assertEquals(
            listOf("General", "Theme", "Translation"),
            filtered.map { it.title },
        )
    }

    @Test
    fun forTab_administration_keepsOnlyPowerTools() {
        val filtered = SettingsMenuSplit.forTab(items, SettingsMenuTab.ADMINISTRATION)
        assertEquals(
            listOf("Custom providers", "Plugins", "App settings"),
            filtered.map { it.title },
        )
    }

    @Test
    fun forTab_keepsUngroupedPreferenceItemsOnEveryTab() {
        val orphan = Preference.PreferenceItem.InfoPreference(title = "Orphan tip")
        val mixed = items + orphan
        assertTrue(
            SettingsMenuSplit.forTab(mixed, SettingsMenuTab.CONFIGURATION)
                .any { it.title == "Orphan tip" },
        )
        assertTrue(
            SettingsMenuSplit.forTab(mixed, SettingsMenuTab.ADMINISTRATION)
                .any { it.title == "Orphan tip" },
        )
    }

    @Test
    fun forTab_partitionsCoverAllGroups() {
        val config = SettingsMenuSplit.forTab(items, SettingsMenuTab.CONFIGURATION)
        val admin = SettingsMenuSplit.forTab(items, SettingsMenuTab.ADMINISTRATION)
        assertEquals(items.size, config.size + admin.size)
        assertTrue(config.none { it in admin })
    }
}
