package com.junkfood.seal.desktop

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class BrowserExtensionBridgeTest {
    @Test
    fun authenticatesExtensionRequestsAndRejectsOtherOrigins() {
        val queued = mutableListOf<String>()
        val bridge = BrowserExtensionBridge("a".repeat(43), { queued.add(it) }, port = 0)
        try {
            bridge.start()
            val client = HttpClient.newHttpClient()
            val uri = URI("${bridge.address}/enqueue")
            val origin = "chrome-extension://${"a".repeat(32)}"

            val preflight =
                client.send(
                    HttpRequest.newBuilder(uri)
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type")
                        .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )
            assertEquals(204, preflight.statusCode())
            assertEquals(origin, preflight.headers().firstValue("Access-Control-Allow-Origin").orElse(null))

            val success =
                client.send(
                    HttpRequest.newBuilder(uri)
                        .header("Origin", origin)
                        .header("Authorization", "Bearer ${"a".repeat(43)}")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("""{"url":"https://youtu.be/abc123"}"""))
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )
            assertEquals(202, success.statusCode())
            assertEquals(listOf("https://youtu.be/abc123"), queued)

            val invalidToken =
                client.send(
                    HttpRequest.newBuilder(uri)
                        .header("Origin", origin)
                        .header("Authorization", "Bearer wrong")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("""{"url":"https://youtu.be/def456"}"""))
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )
            assertEquals(401, invalidToken.statusCode())
            assertEquals(1, queued.size)

            val webOrigin =
                client.send(
                    HttpRequest.newBuilder(uri)
                        .header("Origin", "https://attacker.example")
                        .header("Authorization", "Bearer ${"a".repeat(43)}")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("""{"url":"https://youtu.be/def456"}"""))
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )
            assertEquals(403, webOrigin.statusCode())
            assertEquals(1, queued.size)
        } finally {
            bridge.close()
        }
    }
}
