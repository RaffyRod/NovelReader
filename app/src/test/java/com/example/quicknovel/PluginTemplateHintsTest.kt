package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginManifestItem
import com.lagradost.quicknovel.plugins.PluginTemplateHints
import com.lagradost.quicknovel.plugins.PluginTemplateKind
import org.junit.Assert.assertEquals
import org.junit.Test

class PluginTemplateHintsTest {
    @Test
    fun detect_madaraFromBracketTag() {
        val kind = PluginTemplateHints.detect(
            PluginManifestItem(
                id = "x",
                url = "https://cdn.example/Site[madara].js",
            )
        )
        assertEquals(PluginTemplateKind.MADARA, kind)
    }

    @Test
    fun detect_madaraFromMultisrcIcon() {
        val kind = PluginTemplateHints.detect(
            PluginManifestItem(
                id = "x",
                iconUrl = "https://cdn.example/public/static/multisrc/madara/x/icon.png",
            )
        )
        assertEquals(PluginTemplateKind.MADARA, kind)
    }

    @Test
    fun detect_unsupportedWhenNoTemplateTag() {
        val kind = PluginTemplateHints.detect(
            PluginManifestItem(
                id = "novelfire",
                url = "https://cdn.example/english/NovelFire.js",
                site = "https://novelfire.net/",
            )
        )
        assertEquals(PluginTemplateKind.UNSUPPORTED_JS, kind)
    }

    @Test
    fun detect_ignoresMadaraMentionsInsideJsBody() {
        val kind = PluginTemplateHints.detect(
            PluginManifestItem(
                id = "novelfull",
                url = "https://cdn.example/english/NovelFull.js",
                site = "https://novelfull.com/",
            ),
            jsSource = """
                import { MadaraParser } from '../multisrc/madara/Madara';
                // [madara] helper reused by many plugins
                export default class NovelFull { }
            """.trimIndent(),
        )
        assertEquals(PluginTemplateKind.UNSUPPORTED_JS, kind)
    }
}
