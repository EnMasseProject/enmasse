import enmasse.perf.EnMasseClient
import enmasse.perf.createQueueContext
import io.kotlintest.specs.StringSpec

/**
 * @author Ulf Lilleengen
 */
class StoreAndForwardQueueTest : StringSpec() {
    init {
        val dest = "myqueue"
        val client = EnMasseClient(createQueueContext(dest))

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

