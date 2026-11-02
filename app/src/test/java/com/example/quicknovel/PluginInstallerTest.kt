package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginTemplateHints
import com.lagradost.quicknovel.plugins.PluginTemplateKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PluginInstallerTest {
    @Test
    fun catalogInstall_neverAutoAppliesMadaraOrBloggerCss() {
        val madara = PluginManifestItem(
            id = "arnovel",
            name = "ArNovel",
            site = "https://ar-no.com/",
            lang = "العربية",
            version = "2.3.0",
            url = "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/.js/src/plugins/arabic/ArNovel[madara].js",
            iconUrl = "https://raw.githubusercontent.com/lnreader/lnreader-plugins/plugins/v3.0.0/public/static/multisrc/madara/arnovel/icon.png",
        )
        assertEquals(PluginTemplateKind.MADARA, PluginTemplateHints.detect(madara))
        assertNull(PluginInstaller.configFromManifest(madara))

        val blogger = PluginManifestItem(
            id = "my-blogger-site",
            name = "Blog Novel",
            site = "https://mynovel.blogspot.com/",
            lang = "English",
            url = "https://cdn.example/blogger.js",
        )
        assertEquals(PluginTemplateKind.BLOGGER, PluginTemplateHints.detect(blogger))
        assertNull(PluginInstaller.configFromManifest(blogger))
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
}
