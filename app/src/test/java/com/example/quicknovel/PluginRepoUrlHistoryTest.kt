package com.example.quicknovel

import com.lagradost.quicknovel.plugins.PluginRepoUrlHistoryLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginRepoUrlHistoryTest {
    @Test
    fun add_movesExistingToFrontAndDedupes() {
        val first = PluginRepoUrlHistoryLogic.add(emptyList(), "https://a.example/plugins.min.json", "A")
        val second = PluginRepoUrlHistoryLogic.add(first, "https://b.example/plugins.min.json", "B")
        val again = PluginRepoUrlHistoryLogic.add(second, "https://a.example/plugins.min.json", "A2")
        assertEquals("https://a.example/plugins.min.json", again.first().url)
        assertEquals(2, again.size)
        assertEquals("A2", again.first().label)
    }

    @Test
    fun remove_and_clear() {
        val list = PluginRepoUrlHistoryLogic.add(
            PluginRepoUrlHistoryLogic.add(emptyList(), "https://a.example/x", "A"),
            "https://b.example/y",
            "B",
        )
        assertEquals(1, PluginRepoUrlHistoryLogic.remove(list, "https://a.example/x").size)
        assertTrue(PluginRepoUrlHistoryLogic.clear().isEmpty())
    }

    @Test
    fun hostOf_stripsScheme() {
        assertEquals(
            "raw.githubusercontent.com",
            PluginRepoUrlHistoryLogic.hostOf("https://raw.githubusercontent.com/org/repo/file.json"),
        )
    }
}
