package com.example.quicknovel

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.lagradost.quicknovel.R
import com.lagradost.quicknovel.plugins.InstalledPlugin
import com.lagradost.quicknovel.plugins.PluginHubUi
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginSelectAll
import com.lagradost.quicknovel.plugins.PluginStore
import com.lagradost.quicknovel.ui.settings.PluginProviderDialogs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

/**
 * Device automation for soft-card plugin hub / catalog select rules.
 */
@RunWith(AndroidJUnit4::class)
class PluginHubInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)

    @Test
    fun appContext_usesUltimatePackageFamily() {
        assertTrue(
            "Unexpected package: ${context.packageName}",
            context.packageName.startsWith("com.ultimateSoft.quicknovel.ultimate"),
        )
    }

    @Test
    fun selectRules_onDevice_lockSameVersion_allowUpdate() {
        val catalog = listOf(
            PluginManifestItem(id = "new", name = "New", version = "1.0.0"),
            PluginManifestItem(id = "same", name = "Same", version = "2.0.0"),
            PluginManifestItem(id = "upd", name = "Upd", version = "3.1.0"),
        )
        val installed = mapOf(
            "same" to InstalledPlugin(id = "same", name = "Same", version = "2.0.0"),
            "upd" to InstalledPlugin(id = "upd", name = "Upd", version = "3.0.0"),
        )
        val selectable = PluginSelectAll.selectableIds(catalog, installed)
        assertEquals(setOf("new", "upd"), selectable)
        assertFalse(PluginSelectAll.isSelectable(catalog[1], installed))
        assertTrue(PluginSelectAll.hasCatalogUpdate(installed.getValue("upd"), catalog[2]))
    }

    @Test
    fun hubUi_onDevice_formatsSoftCardMeta() {
        val item = PluginManifestItem(
            id = "demo",
            name = "Demo",
            site = "https://demo.example/",
            lang = "Español",
            version = "1.2.3",
            url = "https://cdn.example/Demo[madara].js",
        )
        assertEquals("Madara", PluginHubUi.kindLabel(item))
        assertEquals("demo.example", PluginHubUi.siteHost(item.site))
        assertEquals("v1.2.3 · Español · Madara", PluginHubUi.catalogMetaLine(item))
    }

    @Test
    fun showHub_opensSoftCardDialogWithTitleAndActions() {
        PluginStore.saveInstalled(
            InstalledPlugin(
                id = "hub_auto_demo",
                name = "Hub Auto Demo",
                version = "1.0.0",
                site = "https://auto.example/",
                lang = "Español",
                jsUrl = "https://cdn.example/HubAutoDemo.js",
            ),
        )
        val scenario = ActivityScenario.launch(PluginHubHostActivity::class.java)
        try {
            device.waitForIdle(2_000)
            scenario.onActivity { activity ->
                PluginStore.ensureOfficialRepo()
                PluginProviderDialogs.showHub(activity) {}
            }
            instrumentation.waitForIdleSync()

            val hierarchy = waitForHierarchyContaining(
                "Hub Auto Demo",
                "Plugins instalados",
                "Installed plugins",
            )
            assertTrue(
                "Hub soft-card dialog missing title/card.\n$hierarchy",
                hierarchy.contains("Hub Auto Demo") &&
                    (hierarchy.contains("Plugins instalados") || hierarchy.contains("Installed plugins")),
            )
            assertTrue(
                "Update action missing.\n$hierarchy",
                hierarchy.contains("Actualizar") || hierarchy.contains("Update"),
            )
            assertTrue(
                "Uninstall action missing.\n$hierarchy",
                hierarchy.contains("Desinstalar") || hierarchy.contains("Uninstall"),
            )
            assertTrue(
                "Browse action missing.\n$hierarchy",
                hierarchy.contains("Explorar catálogo") || hierarchy.contains("Browse catalog"),
            )
        } finally {
            device.pressBack()
            scenario.close()
            PluginStore.removeInstalled("hub_auto_demo")
        }
    }

    @Test
    fun selectStrings_forSoftCards_arePresent() {
        assertTrue(context.getString(R.string.plugin_already_installed).isNotBlank())
        assertTrue(
            context.getString(R.string.plugin_update_available, "1.0.0", "1.0.1").contains("1.0.1"),
        )
        assertTrue(context.getString(R.string.plugin_install_or_update).isNotBlank())
        assertTrue(
            context.getString(R.string.plugin_cards_hint, 10, 3, 2).isNotBlank(),
        )
    }

    /**
     * Polls the accessibility window hierarchy until any needle is found or timeout elapses.
     */
    private fun waitForHierarchyContaining(vararg needles: String, timeoutMs: Long = 12_000L): String {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var last = ""
        while (SystemClock.uptimeMillis() < deadline) {
            val out = ByteArrayOutputStream()
            device.dumpWindowHierarchy(out)
            last = out.toString("UTF-8")
            if (needles.any { last.contains(it) }) return last
            SystemClock.sleep(300)
        }
        return last
    }
}
