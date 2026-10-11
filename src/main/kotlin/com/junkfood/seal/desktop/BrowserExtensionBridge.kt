package com.junkfood.seal.desktop

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class BrowserEnqueueRequest(val url: String)

@Serializable
private data class BrowserEnqueueResponse(val message: String)

internal class BrowserExtensionBridge(
    private val token: String,
    private val onEnqueue: (String) -> Unit,
    private val port: Int = DEFAULT_PORT,
) : AutoCloseable {
    private var started = false
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "seal-browser-bridge").apply { isDaemon = true }
        }
    private val server =
        HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0).apply {
            executor = this@BrowserExtensionBridge.executor
            createContext("/enqueue", ::handleEnqueue)
        }

    init {
        require(token.length >= 32) { "The browser extension token is too short." }
    }

    val address: String
        get() = "http://127.0.0.1:${server.address.port}"

    @Synchronized
    fun start() {
        check(!started) { "The browser extension bridge is already running." }
        try {
            server.start()
            started = true
        } catch (exception: Exception) {
            executor.shutdownNow()
            throw exception
        }
    }

    @Synchronized
    override fun close() {
        if (started) server.stop(0)
        started = false
        executor.shutdownNow()
    }

    private fun handleEnqueue(exchange: HttpExchange) {
        try {
            val origin = exchange.requestHeaders.getFirst("Origin").orEmpty()
            if (!EXTENSION_ORIGIN.matches(origin)) {
                respond(exchange, 403, "This request did not come from a supported browser extension.")
                return
            }
            exchange.responseHeaders.set("Access-Control-Allow-Origin", origin)
            exchange.responseHeaders.set("Vary", "Origin")
            if (exchange.requestMethod == "OPTIONS") {
                exchange.responseHeaders.set("Access-Control-Allow-Methods", "POST, OPTIONS")
                exchange.responseHeaders.set("Access-Control-Allow-Headers", "Authorization, Content-Type")
                exchange.sendResponseHeaders(204, -1)
                return
            }
            if (exchange.requestMethod != "POST") {
                respond(exchange, 405, "Use POST to send a video URL.")
                return
            }
            val authorization = exchange.requestHeaders.getFirst("Authorization").orEmpty()
            val suppliedToken = authorization.removePrefix("Bearer ")
            if (
                !authorization.startsWith("Bearer ") ||
                !MessageDigest.isEqual(token.toByteArray(StandardCharsets.UTF_8), suppliedToken.toByteArray(StandardCharsets.UTF_8))
            ) {
                respond(exchange, 401, "The browser extension token is invalid.")
                return
            }
            val body = exchange.requestBody.use { input -> input.readNBytes(MAX_REQUEST_BYTES + 1) }
            require(body.size <= MAX_REQUEST_BYTES) { "The request is too large." }
            val request = Json.decodeFromString<BrowserEnqueueRequest>(body.toString(StandardCharsets.UTF_8))
            val url = request.url.trim()
            require(isYouTubeVideoUrl(url) && !isYouTubePlaylistUrl(url)) {
                "Send a single YouTube video URL from the extension."
            }
            onEnqueue(url)
            respond(exchange, 202, "The video was added to the download queue.")
        } catch (exception: IllegalArgumentException) {
            respond(exchange, 400, exception.message ?: "The video URL is invalid.")
        } catch (exception: Exception) {
            respond(exchange, 500, exception.message ?: "The browser request could not be queued.")
        } finally {
            exchange.close()
        }
    }

    private fun respond(exchange: HttpExchange, status: Int, message: String) {
        val body = Json.encodeToString(BrowserEnqueueResponse(message))
            .toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }

    companion object {
        const val DEFAULT_PORT = 43721
        private const val MAX_REQUEST_BYTES = 4096
        private val EXTENSION_ORIGIN = Regex("""^chrome-extension://[a-p]{32}$""")
    }
}
