package enmasse.smoketest

import io.kotlintest.specs.StringSpec
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class StoreAndForwardTopicTest: StringSpec() {
    init {
        val dest = "mytopic"
        val client = EnMasseClient(createTopicContext(dest), 2, true)

        "Messages should be delivered to multiple subscribers" {
            val msgs = listOf("foo", "bar", "baz")

            val executor = Executors.newFixedThreadPool(2)
            val recvResults = 1.rangeTo(1).map { i -> executor.submit<List<String>> { client.recvMessages(dest, msgs.size) } }
            val sendResult = executor.submit<Int> { client.sendMessages(dest, msgs) }

            executor.shutdown()

            executor.awaitTermination(30, TimeUnit.SECONDS) shouldBe true

            sendResult.get() shouldBe msgs.size
            recvResults.forEach { result -> result.get().size shouldBe msgs.size }
        }
    }
}