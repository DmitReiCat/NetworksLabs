package lab2

import Config.HOST
import Config.PORT
import kotlinx.coroutines.runBlocking
import java.lang.System.currentTimeMillis
import java.lang.System.nanoTime
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

fun main() {
    val sleepInterval = 1000L
    val client = HeartbeatClient(HOST, PORT, sleepInterval)
    client.run()
}

private var pingUuid = 0
private var clientUuid = 0

class HeartbeatClient(
    private val serverHost: String,
    private val serverPort: Int,
    private val delay: Long,
    private val bufferSize: Int = 1000,
) {
    fun run() =
        runBlocking {
            DatagramSocket().use { clientSocket ->
                val serverAddress = InetAddress.getByName(serverHost)
                val clientId = clientUuid++
                while (true) {
                    clientSocket.run {
                        val heartbeatMessage = "Heartbeat client:$clientId timestamp:${currentTimeMillis()}"
                        sendPacket(serverAddress, heartbeatMessage)

                        Thread.sleep(delay)
                        val pingMessage = "Ping ${pingUuid++} ${currentTimeMillis()}"
                        sendPacket(serverAddress, pingMessage)

                        receiveResponse()
                    }
                }
            }
        }

    private fun DatagramSocket.sendPacket(
        serverAddress: InetAddress,
        message: String,
    ) {
        val data = message.toByteArray()
        val packet = DatagramPacket(data, data.size, serverAddress, this@HeartbeatClient.serverPort)
        send(packet)
    }

    private fun DatagramSocket.receiveResponse() {
        val startTime = nanoTime()
        val data = ByteArray(bufferSize)
        val packet = DatagramPacket(data, data.size)
        receive(packet)
        val response = packet.data.decodeToString(0, packet.length)
        val endTime = nanoTime()
        val rtt = (endTime - startTime) / 1000000.0 // Calculating RTT in milliseconds
        println("Response from server, rtt:$rtt\tmilliseconds,\tresponse:$response")
    }
}
