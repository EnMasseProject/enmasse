package enmasse.smoketest

import io.kotlintest.specs.StringSpec
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class NoStoreAnycastTest: StringSpec() {
    init {
        val dest = "anycast"
        val client = EnMasseClient(createQueueContext(dest), 2, true)

        "Messages should be delivered to receiver" {
            val msgs = listOf("foo", "bar", "baz")

            val executor = Executors.newFixedThreadPool(2)
            val recvResult = executor.submit<List<String>> { client.recvMessages(dest, msgs.size) }
            val sendResult = executor.submit<Int> { client.sendMessages(dest, msgs) }

            executor.shutdown()

            executor.awaitTermination(30, TimeUnit.SECONDS) shouldBe true

            val numReceived = recvResult.get()
            val numSent = sendResult.get()

            numReceived.size shouldBe msgs.size
            numSent shouldBe msgs.size
        }
    }
}