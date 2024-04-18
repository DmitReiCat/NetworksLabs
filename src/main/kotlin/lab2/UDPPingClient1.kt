package lab2

import Config.BUFFER_SIZE
import Config.HOST
import Config.PORT
import Config.TIMEOUT
import java.lang.System.currentTimeMillis
import java.lang.System.nanoTime
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.charset.Charset

fun main() {
    val client = UDPPingClient1(HOST, PORT)
    client.run()
}

class UDPPingClient1(
    private val host: String,
    private val port: Int,
    private val timeout: Int = TIMEOUT
) {
    fun run() {
        val clientSocket = DatagramSocket().apply { soTimeout = timeout }
        val serverAddress = InetAddress.getByName(host)

        clientSocket.use { socket ->
            repeat(10) { iteration ->
                try {
                    val startTime = nanoTime() // Засекаем начальное время перед отправкой сообщения
                    val message = "Ping $iteration"
                    val sendData = message.toByteArray()

                    val sendPacket = DatagramPacket(sendData, sendData.size, serverAddress, port)
                    socket.send(sendPacket)

                    val receiveData = ByteArray(BUFFER_SIZE)
                    val receivePacket = DatagramPacket(receiveData, receiveData.size)
                    socket.receive(receivePacket)
                    val endTime = nanoTime() // Засекаем время приема ответа
                    val rtt = (endTime - startTime) / 1000000.0 // Вычисляем время RTT в миллисекундах
                    val response: String = receivePacket.data.decodeToString(0, receivePacket.length)
                    println(
                        "Response from ${receivePacket.address}:${receivePacket.port}: $response RTT= $rtt milliseconds"
                    )
                } catch (e: SocketTimeoutException) {
                    println("Request timed out")
                }
            }
        }
    }
}
