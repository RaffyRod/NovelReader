package com.example.quicknovel

import com.lagradost.quicknovel.GeminiTranslator
import com.lagradost.quicknovel.util.Apis
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps download / translate / provider pacing inside sane bounds for CI and devices.
 */
class TimingGuardsTest {
    @Test
    fun gemini_defaultInterval_isPatientForFreeTier() {
        val translator = GeminiTranslator(apiKey = "test", model = "gemini-test")
        // Reflect default via constructing with defaults — minInterval must stay >= 3s.
        val field = GeminiTranslator::class.java.getDeclaredField("minIntervalMs")
        field.isAccessible = true
        val interval = field.getLong(translator)
        assertTrue("Gemini minIntervalMs too aggressive: $interval", interval in 3000L..15_000L)
    }

    @Test
    fun provider_rateLimits_areBounded() {
        val limits = Apis.builtInApis.map { it.name to it.rateLimitTime }
        for ((name, ms) in limits) {
            assertTrue("$name rateLimitTime negative", ms >= 0L)
            assertTrue("$name rateLimitTime too high: $ms", ms <= 10_000L)
        }
    }
}
