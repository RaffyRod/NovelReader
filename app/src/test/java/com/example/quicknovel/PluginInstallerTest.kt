package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginTemplateHints
import com.lagradost.quicknovel.plugins.PluginTemplateKind
import com.lagradost.quicknovel.providers.CustomProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginInstallerTest {
    @Test
    fun madaraTemplate_onlyWhenCatalogTagsMadara() {
        val config = PluginInstaller.configFromManifest(
            PluginManifestItem(
                id = "arnovel",
                name = "ArNovel",
                site = "https://ar-no.com/",
                lang = "العربية",
                version = "2.3.0",
                url = "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/.js/src/plugins/arabic/ArNovel[madara].js",
                iconUrl = "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/public/static/multisrc/madara/arnovel/icon.png",
            )
        )
        assertNotNull(config)
        assertEquals(CustomProviderConfig.KIND_CSS, config!!.kind)
        assertTrue(config.chaptersViaAjax)
        assertTrue(config.searchUrl.contains("wp-manga"))
    }

    @Test
    fun nonMadaraPlugin_doesNotInventMadaraSelectors() {
        val item = PluginManifestItem(
            id = "novelfire",
            name = "NovelFire",
            site = "https://novelfire.net/",
            lang = "English",
            version = "1.0.0",
            url = "https://cdn.example/english/NovelFire.js",
        )
        assertEquals(PluginTemplateKind.UNSUPPORTED_JS, PluginTemplateHints.detect(item))
        assertNull(PluginInstaller.configFromManifest(item))
    }

    @Test
    fun bloggerTemplate_whenUrlOrIdHintsBlogger() {
        val config = PluginInstaller.configFromManifest(
            PluginManifestItem(
                id = "my-blogger-site",
                name = "Blog Novel",
                site = "https://mynovel.blogspot.com/",
                lang = "English",
                url = "https://cdn.example/blogger.js",
            )
        )
        assertNotNull(config)
        assertEquals(CustomProviderConfig.KIND_BLOGGER, config!!.kind)
        assertEquals("en", config.lang)
        assertEquals("Blog Novel", config.name)
    }

    @Test
    fun localName_overridesManifestDisplayName() {
        val config = PluginInstaller.configFromManifest(
            PluginManifestItem(
                id = "x",
                name = "Remote",
                site = "https://example.com",
                lang = "Français",
                url = "https://cdn.example/Remote[madara].js",
            ),
            localName = "Mi fuente",
        )
        assertNotNull(config)
        assertEquals("Mi fuente", config!!.name)
        assertEquals("fr", config.lang)
    }
}
