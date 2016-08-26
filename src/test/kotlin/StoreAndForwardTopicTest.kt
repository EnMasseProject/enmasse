import enmasse.perf.EnMasseClient
import enmasse.perf.Environment
import enmasse.perf.createTopicContext
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
        val client = EnMasseClient(createTopicContext(Environment.endpoint, dest))

        "Messages should be delivered to multiple subscribers" {
            val msgs = listOf("foo", "bar", "baz")

            val countdownLatch = CountDownLatch(3)
            val executor = Executors.newFixedThreadPool(3)
            val results = 1.rangeTo(3).map { executor.submit<Int> {
                countdownLatch.countDown()
                client.recvMessages(dest, msgs.size).size
            }}

            countdownLatch.await(5, TimeUnit.MINUTES)
            client.sendMessages(dest, msgs) shouldBe msgs.size
            println("Sent ${msgs.size} messages")

            results.forEach { future ->
                val result = future.get(5, TimeUnit.MINUTES)
                result shouldBe msgs.size
            }

            executor.shutdown()
        }
    }
}