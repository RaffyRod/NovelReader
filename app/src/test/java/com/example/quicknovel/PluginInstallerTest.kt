package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.providers.CustomProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginInstallerTest {
    @Test
    fun madaraTemplate_forTypicalMultisrcSite() {
        val config = PluginInstaller.configFromManifest(
            PluginManifestItem(
                id = "nova",
                name = "NOVA",
                site = "https://novelasligeras.net/",
                lang = "Español",
                version = "1.0.0",
                url = "https://cdn.example/nova.js",
            )
        )
        assertEquals("NOVA", config.name)
        assertEquals("es", config.lang)
        assertEquals(CustomProviderConfig.KIND_CSS, config.kind)
        assertTrue(config.chaptersViaAjax)
        assertTrue(config.baseUrl.contains("novelasligeras.net"))
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
        assertEquals(CustomProviderConfig.KIND_BLOGGER, config.kind)
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
            ),
            localName = "Mi fuente",
        )
        assertEquals("Mi fuente", config.name)
        assertEquals("fr", config.lang)
    }
}
