package com.example.quicknovel

import com.lagradost.quicknovel.providers.ProviderHostHealth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderHostHealthTest {
    @Test
    fun `www is ignored when comparing hosts`() {
        assertTrue(ProviderHostHealth.hostsMatch("www.example.com", "example.com"))
        assertFalse(ProviderHostHealth.hostsMatch("example.com", "other.com"))
    }

    @Test
    fun `origin strips path and query`() {
        assertEquals(
            "https://novels.example",
            ProviderHostHealth.originOf("https://novels.example/path?q=1"),
        )
    }

    @Test
    fun `invalid scheme is rejected`() {
        val report = ProviderHostHealth.probe("ftp://bad.example")
        assertFalse(report.reachable)
        assertTrue(report.error!!.contains("http", ignoreCase = true))
    }

    @Test
    fun `reachable same host`() {
        val report = ProviderHostHealth.probe("https://old.example") { url, _ ->
            ProviderHostHealth.ProbeStep(url = url, status = 200)
        }
        assertTrue(report.reachable)
        assertFalse(report.domainChanged)
        assertEquals("https://old.example", report.effectiveBaseUrl())
        assertTrue(report.latencyMs != null && report.latencyMs!! >= 0)
    }

    @Test
    fun `domain change follows redirects`() {
        val report = ProviderHostHealth.probe("https://old.example") { url, _ ->
            when {
                url.contains("old.example") ->
                    ProviderHostHealth.ProbeStep(
                        url = url,
                        status = 301,
                        location = "https://new.example/",
                    )
                else -> ProviderHostHealth.ProbeStep(url = url, status = 200)
            }
        }
        assertTrue(report.reachable)
        assertTrue(report.domainChanged)
        assertEquals("https://new.example", report.effectiveBaseUrl())
        assertTrue(report.userMessage().contains("new.example"))
        assertTrue(report.userMessage().contains("old.example"))
    }

    @Test
    fun `http 503 is not reachable`() {
        val report = ProviderHostHealth.probe("https://dead.example") { url, _ ->
            ProviderHostHealth.ProbeStep(url = url, status = 503)
        }
        assertFalse(report.reachable)
        assertEquals("HTTP 503", report.error)
    }

    @Test
    fun `cloudflare 403 still counts as host alive`() {
        val report = ProviderHostHealth.probe("https://cf.example") { url, _ ->
            ProviderHostHealth.ProbeStep(url = url, status = 403)
        }
        assertTrue(report.reachable)
        assertFalse(report.domainChanged)
    }

    @Test
    fun `head rejection falls back to get`() {
        var sawGet = false
        val report = ProviderHostHealth.probe("https://ok.example") { url, method ->
            if (method == "HEAD") {
                ProviderHostHealth.ProbeStep(url = url, status = 405)
            } else {
                sawGet = true
                ProviderHostHealth.ProbeStep(url = url, status = 200)
            }
        }
        assertTrue(sawGet)
        assertTrue(report.reachable)
    }

    @Test
    fun `relative redirect resolves against base`() {
        val report = ProviderHostHealth.probe("https://old.example/home") { url, _ ->
            when {
                url.endsWith("/home") ->
                    ProviderHostHealth.ProbeStep(url = url, status = 302, location = "/")
                else -> ProviderHostHealth.ProbeStep(url = url, status = 200)
            }
        }
        assertTrue(report.reachable)
        assertFalse(report.domainChanged)
    }
}
