package lab2

import Config.BUFFER_SIZE
import Config.HOST
import Config.PORT
import Config.TIMEOUT
import java.lang.System.nanoTime
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

fun main() {
    val client = UDPPingClient2(HOST, PORT)
    client.run()
}

class UDPPingClient2(
    private val host: String,
    private val port: Int,
    private val timeout: Int = TIMEOUT,
) {
    fun run() {
        val clientSocket = DatagramSocket().apply { soTimeout = timeout }
        val serverAddress = InetAddress.getByName(host)
        val rttTimes = LongArray(10)
        var lostPackets = 0

        clientSocket.use { socket ->
            repeat(10) { iteration ->
                val startTime = nanoTime() // Засекаем начальное время перед отправкой сообщения
                val message = "Ping $iteration ${nanoTime() / 1000000.0}"
                val sendData = message.toByteArray()

                val sendPacket = DatagramPacket(sendData, sendData.size, serverAddress, port)
                socket.send(sendPacket)

                try {
                    val receiveData = ByteArray(BUFFER_SIZE)
                    val receivePacket = DatagramPacket(receiveData, receiveData.size)
                    socket.receive(receivePacket)
                    val endTime = nanoTime() // Засекаем время приема ответа
                    val rtt = (endTime - startTime) / 1000000.0 // Вычисляем время RTT в миллисекундах
                    val response = String(receivePacket.data).trim { it <= ' ' }
                    println(
                        "Response from ${receivePacket.address}:${receivePacket.port} " +
                            "resp:$response RTT= $rtt milliseconds",
                    )
                    rttTimes[iteration] = rtt.toLong()
                } catch (e: SocketTimeoutException) {
                    lostPackets++
                    println("Request timed out")
                }

                var totalRtt = 0.0
                for (rtt in rttTimes) {
                    totalRtt += rtt.toDouble()
                }
                val packetLoss = lostPackets.toDouble() / 10 * 100
                val minRtt = rttTimes.min() / 1000000.0
                val maxRtt = rttTimes.max() / 1000000.0
                val agvRtt = totalRtt / (10 - lostPackets) / 1000000.0
                println(
                    "\t[Ping statistics]\t\t" +
                        "10 packets transmitted, ${10 - lostPackets} packets received, $packetLoss packet loss," +
                        " Min RTT $minRtt\tMax RTT $maxRtt\tAverage RTT $agvRtt",
                )
            }
        }
    }
}
