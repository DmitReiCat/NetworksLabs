package ru.network.labs.lab1

import Config.HOST
import Config.PORT
import io.ktor.network.selector.ActorSelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

fun main() {
    val server = Server(HOST, PORT)
    server.run()
}

class Server(
    private val host: String,
    private val port: Int
) {
    private val serverCoroutineScope = CoroutineScope(Dispatchers.IO)

    fun run() =
        runBlocking {
            val selectorManager = ActorSelectorManager(Dispatchers.IO)
            val serverSocket = aSocket(selectorManager).tcp().bind(host, port)
            println("Server is running on port $host:$port")

            while (true) {
                val socket = serverSocket.accept()
                serverCoroutineScope.launch { // запуск обработки запроса на отдельном легковесном потоке
                    println("Connection accepted: ${socket.remoteAddress}")

                    val input = socket.openReadChannel()
                    val output = socket.openWriteChannel(autoFlush = true)

                    try {
                        val requestLine = input.readUTF8Line()
                        val filename = requestLine?.split(" ")?.get(1) // Пропоустим первый символ "/"

                        if (filename != null) {
                            val file = File(filename)
                            if (file.exists() && file.isFile) {
                                output.writeStringUtf8("HTTP/1.1 200 OK\r\nContent-Length: ${file.length()}\r\n\r\n")
                                output.writeFully(file.readBytes())
                            } else {
                                output.writeStringUtf8("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n")
                            }
                        } else {
                            output.writeStringUtf8("HTTP/1.1 400 Bad Request\r\nContent-Length: 0\r\n\r\n")
                        }
                    } finally {
                        socket.close()
                    }
                }
            }
        }
}
