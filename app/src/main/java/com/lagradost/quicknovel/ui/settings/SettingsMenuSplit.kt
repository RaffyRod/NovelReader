package com.lagradost.quicknovel.ui.settings

import com.mihon.presentation.settings.Preference

/** Settings hub tabs (mock 9): daily prefs vs power tools. */
enum class SettingsMenuTab {
    CONFIGURATION,
    ADMINISTRATION,
}

/**
 * Splits [Preference] groups into Configuración / Administración without dropping items.
 */
object SettingsMenuSplit {
    fun forTab(items: List<Preference>, tab: SettingsMenuTab): List<Preference> =
        items.mapNotNull { pref ->
            when (pref) {
                is Preference.PreferenceGroup ->
                    if (pref.section == tab) pref else null
                is Preference.PreferenceItem<*, *> -> pref
            }
        }
}
