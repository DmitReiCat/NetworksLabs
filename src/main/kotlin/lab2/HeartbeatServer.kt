package lab2

import Config.PORT
import Config.TIMEOUT
import java.lang.Thread.sleep
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.random.Random

fun main() {
    val server = HeartbeatServer(PORT)
    server.run()
}

class HeartbeatServer(
    private val port: Int,
    private val bufferSize: Int = 1000,
    private val timeout: Int = TIMEOUT,
) {
    fun run() {
        DatagramSocket(port).use { serverSocket ->
            val clients: MutableMap<String, Long> = mutableMapOf()

            while (true) {
                val data = ByteArray(bufferSize)
                val receivePacket = DatagramPacket(data, data.size)
                serverSocket.receive(receivePacket)

                val message = receivePacket.data.decodeToString(0, receivePacket.length)
                processHeartbeat(message, clients)
                serverSocket.sendResponse(receivePacket)
            }
        }
    }

    private fun processHeartbeat(
        message: String,
        clients: MutableMap<String, Long>,
    ) {
        val messageData = message.split(" ")
        if (messageData.size == 3 && messageData[0] == "Heartbeat") {
            val clientId = messageData[1]
            clients[clientId] = System.currentTimeMillis()
            println("Heartbeat received from client $clientId")
            clients.entries.removeIf { System.currentTimeMillis() - it.value > timeout }
            clients.forEach { (key: String, value: Long) ->
                if (System.currentTimeMillis() - value > timeout) {
                    println("Client $key is inactive. Application may have stopped.")
                }
            }
        }
    }

    private fun DatagramSocket.sendResponse(receivePacket: DatagramPacket) {
        val clientAddress: InetAddress = receivePacket.address
        val clientPort: Int = receivePacket.port
        send(DatagramPacket(receivePacket.data, receivePacket.length, clientAddress, clientPort))
    }
}
