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
            val results = 1.rangeTo(2).map { executor.submit<Int> {
                client.recvMessages(dest, msgs.size, connectListener = { countdownLatch.countDown() }).size
            }}

            countdownLatch.await(1, TimeUnit.MINUTES)
            client.sendMessages(dest, msgs) shouldBe msgs.size
            println("Sent ${msgs.size} messages")

            results.forEach { future ->
                val result = future.get(2, TimeUnit.MINUTES)
                result shouldBe msgs.size
            }

            executor.shutdown()
        }
    }
}