package ru.network.labs.lab1

import Config.HOST
import Config.PORT
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

fun main() {
    val client = Client(HOST, PORT, "src/main/kotlin/lab1/test1")
    client.makeRequest()
}

class Client(
    private val host: String,
    private val port: Int,
    private val filePath: String
) {
    fun makeRequest() =
        runBlocking {
            try {
                val selectorManager = ActorSelectorManager(Dispatchers.IO)
                aSocket(selectorManager).tcp().connect(InetSocketAddress(host, port)).use { socket ->
                    val request = "GET $filePath HTTP/1.1\r\nHost: $host\r\n\r\n"

                    val input = socket.openReadChannel()
                    val output = socket.openWriteChannel(autoFlush = true)

                    output.writeStringUtf8(request)

                    var responseLine = input.readUTF8Line()
                    while (responseLine != null) {
                        println(responseLine)
                        responseLine = input.readUTF8Line()
                    }
                }
            } catch (e: Exception) {
                println("ERROR: ${e.message}")
            }
        }
}
