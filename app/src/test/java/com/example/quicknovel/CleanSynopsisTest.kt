package com.example.quicknovel

import com.lagradost.quicknovel.cleanSynopsis
import org.junit.Assert.assertEquals
import org.junit.Test

class CleanSynopsisTest {
    @Test
    fun `elementor css left in the description is removed`() {
        val raw = "/*! elementor - v3.18.0 - 20-12-2023 */<br />>.elementor-widget-image{text-align:center}" +
            ".elementor-widget-image a{display:inline-block}.elementor-widget-image a img[src\$=\".svg\"]{width:48px}" +
            ".elementor-widget-image img{vertical-align:middle;display:inline-block}Ancient power returns."
        assertEquals("Ancient power returns.", raw.cleanSynopsis())
    }

    @Test
    fun `style and script blocks are removed`() {
        val raw = "<style>p{color:red}</style>Hello<script>alert(1)</script> world"
        assertEquals("Hello world", raw.cleanSynopsis())
    }

    @Test
    fun `plain descriptions are not touched`() {
        val raw = "He said: {hello} and left."
        assertEquals(raw, raw.cleanSynopsis())
    }

    @Test
    fun `text with no code is returned as is`() {
        assertEquals("Una novela normal.", "Una novela normal.".cleanSynopsis())
    }
}
