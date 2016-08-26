package enmasse.smoketest

import io.kotlintest.specs.StringSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class StoreAndForwardTopicTest: StringSpec() {
    init {
        val dest = "mytopic"
        val client = EnMasseClient(createTopicContext(dest))

        "Messages should be delivered to multiple subscribers" {
            val msgs = listOf("foo", "bar", "baz")

            val countdownLatch = CountDownLatch(3)
            val executor = Executors.newFixedThreadPool(3)
            val results = 1.rangeTo(3).map { i -> executor.submit<Int> {
                println("Running client ${i}")
                val sz = client.recvMessages(dest, msgs.size, connectListener = { countdownLatch.countDown() }).size
                println("Client ${i} recieved ${sz} messages")
                sz
            }}

            countdownLatch.await(20, TimeUnit.SECONDS)
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