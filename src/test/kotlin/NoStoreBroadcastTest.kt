import enmasse.perf.EnMasseClient
import enmasse.perf.Endpoint
import enmasse.perf.Environment
import enmasse.perf.createQueueContext
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
        val client = EnMasseClient(createQueueContext(Environment.endpoint, dest))

        "Messages should be delivered to multiple receivers" {
            val msgs = listOf("foo", "bar", "baz")

            val countdownLatch = CountDownLatch(2)
            val executor = Executors.newFixedThreadPool(2)
            val results = 1.rangeTo(2).map { executor.submit<Int> {
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