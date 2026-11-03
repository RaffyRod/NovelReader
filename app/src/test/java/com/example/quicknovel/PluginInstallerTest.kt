package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginInstaller
import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginTemplateHints
import com.lagradost.quicknovel.plugins.PluginTemplateKind
import com.lagradost.quicknovel.providers.CustomProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginInstallerTest {
    @Test
    fun madaraTagged_autoMapsMadaraCss_forSearch() {
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
        val cfg = PluginInstaller.configFromManifest(madara)
        assertNotNull(cfg)
        assertEquals(CustomProviderConfig.KIND_CSS, cfg!!.kind)
        assertEquals(CustomProviderConfig.DEFAULT_SITE_SEARCH_URL, cfg.searchUrl)
        assertFalse(cfg.searchUrl.contains("wp-manga"))
        assertTrue(cfg.chaptersViaAjax)
        assertTrue(cfg.resultItem.contains("page-item-detail"))
        assertEquals("https://ar-no.com", cfg.baseUrl)
    }

    @Test
    fun bloggerSite_autoMapsBloggerCss_forSearch() {
        val blogger = PluginManifestItem(
            id = "my-blogger-site",
            name = "Blog Novel",
            site = "https://mynovel.blogspot.com/",
            lang = "English",
            url = "https://cdn.example/blogger.js",
        )
        assertEquals(PluginTemplateKind.BLOGGER, PluginTemplateHints.detect(blogger))
        val cfg = PluginInstaller.configFromManifest(blogger)
        assertNotNull(cfg)
        assertEquals(CustomProviderConfig.KIND_BLOGGER, cfg!!.kind)
        assertEquals("https://mynovel.blogspot.com", cfg.baseUrl)
    }

    @Test
    fun untaggedJs_withSite_mapsGenericBridge_notMadara() {
        val item = PluginManifestItem(
            id = "novelfire",
            name = "NovelFire",
            site = "https://novelfire.net/",
            lang = "English",
            version = "1.0.0",
            url = "https://cdn.example/english/NovelFire.js",
        )
        assertEquals(PluginTemplateKind.UNSUPPORTED_JS, PluginTemplateHints.detect(item))
        val cfg = PluginInstaller.configFromManifest(item)
        assertNotNull(cfg)
        assertEquals("/?s={query}", cfg!!.searchUrl)
        assertFalse("must not invent wp-manga for untagged JS", cfg.searchUrl.contains("wp-manga"))
        assertFalse("must not invent Madara ajax for untagged JS", cfg.chaptersViaAjax)
        assertEquals("https://novelfire.net", cfg.baseUrl)
    }

    @Test
    fun pluginWithoutSite_cannotJoinSearch() {
        val item = PluginManifestItem(
            id = "orphan",
            name = "Orphan",
            site = "",
            url = "https://cdn.example/Orphan[madara].js",
        )
        assertEquals(PluginTemplateKind.MADARA, PluginTemplateHints.detect(item))
        assertNull(PluginInstaller.configFromManifest(item))
    }
}
