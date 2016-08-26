import enmasse.perf.EnMasseClient
import enmasse.perf.Environment
import enmasse.perf.createQueueContext
import io.kotlintest.specs.StringSpec

/**
 * @author Ulf Lilleengen
 */
class QueueTest : StringSpec() {
    init {
        val dest = "myqueue"
        val client = EnMasseClient(createQueueContext(Environment.endpoint, dest))

        "Messages should be queued" {
            val msgs = listOf("foo", "bar", "baz")
            client.sendMessages(dest, msgs) shouldBe msgs.size
            println("Sent ${msgs.size} messages")

            val received = client.recvMessages(dest, msgs.size)

            println("Received ${received.size} messages")
            received.size shouldBe msgs.size
        }
    }
}

