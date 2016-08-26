import enmasse.perf.EnMasseClient
import enmasse.perf.Environment
import enmasse.perf.createQueueContext
import io.kotlintest.specs.StringSpec
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class NoStoreAnycastTest: StringSpec() {
    init {
        val dest = "anycast"
        val client = EnMasseClient(createQueueContext(Environment.endpoint, dest))

        "Messages should be delivered to receiver" {
            val msgs = listOf("foo", "bar", "baz")

            val executor = Executors.newFixedThreadPool(1)
            val receiver = executor.submit<Int> { client.recvMessages(dest, msgs.size).size }
            client.sendMessages(dest, msgs) shouldBe msgs.size
            println("Sent ${msgs.size} messages")

            val result = receiver.get(5, TimeUnit.MINUTES)

            executor.shutdown()

            println("Received ${result} messages")
            result shouldBe msgs.size
        }
    }
}