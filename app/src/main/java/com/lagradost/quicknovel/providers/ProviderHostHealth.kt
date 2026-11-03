package com.lagradost.quicknovel.providers

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

/**
 * Lightweight reachability + domain-change probe for provider base URLs.
 * Used when adding a custom provider and before search / homepage browse.
 */
object ProviderHostHealth {
    private const val TIMEOUT_MS = 12_000
    private const val MAX_REDIRECTS = 8
    private val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

    data class ProbeStep(
        val url: String,
        val status: Int,
        val location: String? = null,
    )

    data class Report(
        val configuredUrl: String,
        val reachable: Boolean,
        val finalUrl: String? = null,
        val httpStatus: Int? = null,
        val domainChanged: Boolean = false,
        val error: String? = null,
        val steps: List<ProbeStep> = emptyList(),
        /** Wall time for the full probe (redirects included). */
        val latencyMs: Long? = null,
    ) {
        /** Origin to persist when the host redirected to a new domain. */
        fun effectiveBaseUrl(): String {
            val final = finalUrl ?: return configuredUrl.trim().trimEnd('/')
            return originOf(final) ?: configuredUrl.trim().trimEnd('/')
        }

        fun userMessage(): String = when {
            !reachable ->
                error?.takeIf { it.isNotBlank() }
                    ?: "Site unreachable: ${hostOf(configuredUrl) ?: configuredUrl}"
            domainChanged -> {
                val from = hostOf(configuredUrl).orEmpty()
                val to = hostOf(finalUrl).orEmpty()
                "Site redirected to $to (was $from). Update the provider URL."
            }
            else -> "OK"
        }
    }

    fun normalizeHost(host: String?): String? {
        if (host.isNullOrBlank()) return null
        var h = host.trim().lowercase(Locale.ROOT)
        if (h.startsWith("www.")) h = h.removePrefix("www.")
        return h
    }

    fun hostsMatch(a: String?, b: String?): Boolean {
        val left = normalizeHost(a) ?: return false
        val right = normalizeHost(b) ?: return false
        return left == right
    }

    fun hostOf(url: String?): String? = runCatching {
        if (url.isNullOrBlank()) return null
        URI(url.trim()).host
    }.getOrNull()

    fun originOf(url: String?): String? = runCatching {
        if (url.isNullOrBlank()) return null
        val uri = URI(url.trim())
        val scheme = uri.scheme ?: return null
        val host = uri.host ?: return null
        val port = uri.port
        if (port > 0 && port != defaultPort(scheme)) "$scheme://$host:$port" else "$scheme://$host"
    }.getOrNull()

    /**
     * Probes [baseUrl]. [open] is injectable for unit tests.
     * Follows redirects and flags when the final registrable host differs (ignoring www).
     */
    fun probe(
        baseUrl: String,
        open: (url: String, method: String) -> ProbeStep = ::defaultOpen,
    ): Report {
        val configured = baseUrl.trim().trimEnd('/')
        if (configured.isBlank() || (!configured.startsWith("http://") && !configured.startsWith("https://"))) {
            return Report(
                configuredUrl = configured,
                reachable = false,
                error = "Invalid URL (must start with http:// or https://)",
            )
        }
        val steps = mutableListOf<ProbeStep>()
        var current = configured
        var lastStatus: Int? = null
        val startedAt = System.currentTimeMillis()
        fun elapsed() = System.currentTimeMillis() - startedAt
        try {
            for (i in 0..MAX_REDIRECTS) {
                val step = open(current, if (i == 0) "HEAD" else "GET").let { head ->
                    // Some hosts reject HEAD — retry once with GET on the same URL.
                    if (i == 0 && (head.status == 405 || head.status == 403 || head.status == 501)) {
                        open(current, "GET")
                    } else {
                        head
                    }
                }
                steps += step
                lastStatus = step.status
                val loc = step.location
                if (isRedirect(step.status) && !loc.isNullOrBlank()) {
                    current = resolveUrl(current, loc)
                    continue
                }
                val finalUrl = current
                val changed = !hostsMatch(hostOf(configured), hostOf(finalUrl))
                // 2xx/3xx = OK. 401/403/429 = host answered (often Cloudflare / rate limit), not "domain dead".
                if (step.status in 200..399 || step.status == 401 || step.status == 403 || step.status == 429) {
                    return Report(
                        configuredUrl = configured,
                        reachable = true,
                        finalUrl = finalUrl,
                        httpStatus = step.status,
                        domainChanged = changed,
                        steps = steps,
                        latencyMs = elapsed(),
                    )
                }
                return Report(
                    configuredUrl = configured,
                    reachable = false,
                    finalUrl = finalUrl,
                    httpStatus = step.status,
                    error = "HTTP ${step.status}",
                    steps = steps,
                    latencyMs = elapsed(),
                )
            }
            return Report(
                configuredUrl = configured,
                reachable = false,
                finalUrl = current,
                httpStatus = lastStatus,
                error = "Too many redirects",
                steps = steps,
                latencyMs = elapsed(),
            )
        } catch (e: Exception) {
            return Report(
                configuredUrl = configured,
                reachable = false,
                finalUrl = current,
                httpStatus = lastStatus,
                error = e.message ?: e.javaClass.simpleName,
                steps = steps,
                latencyMs = elapsed(),
            )
        }
    }

    private fun isRedirect(status: Int) = status in listOf(301, 302, 303, 307, 308)

    private fun defaultPort(scheme: String) = when (scheme.lowercase(Locale.ROOT)) {
        "https" -> 443
        "http" -> 80
        else -> -1
    }

    private fun resolveUrl(base: String, location: String): String {
        val trimmed = location.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return URL(URL(base), trimmed).toString()
    }

    private fun defaultOpen(url: String, method: String): ProbeStep {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = method
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "text/html,*/*")
            // Avoid downloading large bodies when using GET.
            if (method == "GET") setRequestProperty("Range", "bytes=0-0")
        }
        return try {
            val code = conn.responseCode
            ProbeStep(url = url, status = code, location = conn.getHeaderField("Location"))
        } finally {
            conn.disconnect()
        }
    }
}
