package com.example.quicknovel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards Sections → translation history: Material Translate (A+文), not the globe Language icon.
 */
class LibraryRailTranslationIconTest {
    private val root = File("src/main")

    @Test
    fun rail_usesTranslateDrawable_notGlobe() {
        val rail = File(root, "java/com/lagradost/quicknovel/ui/download/LibraryRail.kt").readText()
        assertTrue(
            "LibraryRail translation history must use ic_baseline_translate_24",
            rail.contains("ic_baseline_translate_24"),
        )
        assertFalse(
            "LibraryRail must not use the globe language icon for translation history",
            rail.contains("ic_baseline_language_24"),
        )
    }

    @Test
    fun translateDrawable_exists_withMaterialTranslatePath() {
        val drawable = File(root, "res/drawable/ic_baseline_translate_24.xml")
        assertTrue("Missing ${drawable.path}", drawable.isFile)
        val xml = drawable.readText()
        assertTrue(xml.contains("pathData="))
        // Distinct from globe (language): translate paths include the 文 / latin-A glyphs.
        assertTrue(xml.contains("M12.87,15.07") || xml.contains("M12.87 15.07"))
        assertFalse(xml.contains("M11.99,2C6.47,2"))
    }
}
