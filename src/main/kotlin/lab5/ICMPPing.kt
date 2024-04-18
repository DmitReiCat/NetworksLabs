package lab5

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.lang.System.nanoTime
import java.net.InetAddress

private const val TARGET_HOST = "ya.ru"
private const val TIMEOUT = 200
private const val NUM_OF_PACKETS = 4

fun main(): Unit =
    runBlocking {
        val remoteAddress = InetAddress.getByName(TARGET_HOST)
        val responseTimes = mutableListOf<Long>()
        var lostCount = 0

        println("NETWORK CHECKER for $TARGET_HOST, $remoteAddress:")

        repeat(NUM_OF_PACKETS) {
            val startTime = nanoTime()

            try {
                if (remoteAddress.isReachable(TIMEOUT)) {
                    val responseTime = nanoTime() - startTime
                    responseTimes.add(responseTime)
                    println("Received response from " + remoteAddress + ": time=" + responseTime + "ms")
                } else {
                    println("Request timed out")
                    lostCount++
                }
            } catch (e: IOException) {
                println("Error: " + e.message)
            }

            delay(1000L)
        }

        if (responseTimes.isNotEmpty()) {
            val minResponseTime = responseTimes.min()
            val maxResponseTime = responseTimes.min()
            val sumResponseTime = responseTimes.sum()
            val avgResponseTime = sumResponseTime / responseTimes.size
            val lossPercentage = lostCount / NUM_OF_PACKETS * 100L

            println("\n--- $TARGET_HOST network statistics ---")
            println(
                NUM_OF_PACKETS.toString() + " packets sent, " + (NUM_OF_PACKETS - lostCount) + " received, " +
                    lossPercentage + "% packet loss",
            )
            println("Response time min/avg/max = $minResponseTime/$avgResponseTime/$maxResponseTime ms")
        }
    }
