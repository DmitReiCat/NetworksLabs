@file:Suppress("ktlint:standard:no-wildcard-imports")

package lab4

import Config.PORT
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.lang.System.currentTimeMillis
import java.util.concurrent.ConcurrentHashMap

private const val TTL = 5000L

data class CachedContent(
    val content: ByteArray,
    val contentType: ContentType?,
    val timestamp: Long,
)

suspend fun main() {
    val cache = ConcurrentHashMap<String, CachedContent>()
    val client =
        HttpClient(CIO) {
            expectSuccess = false
        }

    embeddedServer(Netty, port = PORT) {
        install(CallLogging)
        routing {
            get("/{...}") {
                val proxyUrl = call.getProxyUrl()

                println("proxyUrl: $proxyUrl")

                val cachedContent = cache[proxyUrl]
                if (cachedContent != null && cachedContent.timestamp + TTL < currentTimeMillis()) {
                    println("Cache hit!")
                    call.respondBytes(
                        bytes = cachedContent.content,
                    )
                } else {
                    println("Cache not found, saving cache")
                    try {
                        val httpResponse: HttpResponse = client.get(proxyUrl)
                        val contentBytes = httpResponse.readBytes()
                        cache[proxyUrl] = CachedContent(contentBytes, httpResponse.contentType(), currentTimeMillis())
                        println("Answering from latest cache")
                        call.respondBytes(
                            bytes = cache[proxyUrl]!!.content
                        )
                    } catch (e: Exception) {
                        val errorPage = "<html><body><h1>Error 404: Not Found</h1></body></html>"
                        call.respondBytes(
                            status = HttpStatusCode.NotFound,
                            bytes = errorPage.toByteArray()
                        )
                    }
                }
            }

            post("/{...}") {
                var proxyUrl = call.getProxyUrl()

                val response: HttpResponse =
                    client.post(proxyUrl) {
                        setBody(call.receiveText())
                    }
                val contentBytes = response.readBytes()
                // Кэширование для POST запросов может быть реализовано по аналогии с GET, если это необходимо.
                // В данном случае ответы POST запросов не кэшируются,
                // поскольку содержание POST запросов часто является динамическим и может изменяться.

                call.respondBytes(
                    bytes = contentBytes,
                    contentType = response.contentType() ?: ContentType.Application.OctetStream,
                )
            }
        }
    }.start(wait = true)
}

private fun ApplicationCall.getProxyUrl(): String {
    val referer = request.headers["Referer"]
    var proxyUrl = if (referer != null) {
        referer.replace(oldValue = "http://0.0.0.0:$PORT/", newValue = "") + request.uri
    } else {
        request.uri.substring(startIndex = 1)
    }
    if (!proxyUrl.startsWith("http")) proxyUrl = "http://$proxyUrl"
    return proxyUrl
}
