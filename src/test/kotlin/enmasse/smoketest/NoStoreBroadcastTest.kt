package enmasse.smoketest

import io.kotlintest.specs.StringSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class NoStoreBroadcastTest: StringSpec() {
    init {
        val dest = "broadcast"
        val client = EnMasseClient(createQueueContext(dest))

        "Messages should be delivered to multiple receivers" {
            val msgs = listOf("foo", "bar", "baz")

            val countdownLatch = CountDownLatch(2)
            val executor = Executors.newFixedThreadPool(2)
            val results = 1.rangeTo(2).map { i -> executor.submit<Int> {
                val sz = client.recvMessages(dest, msgs.size, connectListener = { countdownLatch.countDown() }).size
                println("Client ${i} recieved ${sz} messages")
                sz
            }}

            countdownLatch.await(10, TimeUnit.SECONDS)
            client.sendMessages(dest, msgs) shouldBe msgs.size
            println("Sent ${msgs.size} messages")

            executor.shutdown()
            executor.awaitTermination(20, TimeUnit.SECONDS) shouldBe true

            results.forEach { future ->
                val result = future.get()
                result shouldBe msgs.size
            }
        }
    }
}