import enmasse.perf.*
import io.kotlintest.specs.StringSpec

/**
 * @author Ulf Lilleengen
 */
class QueueTest : StringSpec() {
    init {
        val ctx = createQueueContext(Environment.endpoint, "myqueue")
        "Messages should be queued" {
            val sender = TestSender(ctx, "myqueue")
            val msgs = listOf("foo", "bar", "baz")
            sender.sendMessages(msgs) shouldBe msgs.size
            println("Sent ${msgs.size} messages")

            val receiver = TestReceiver(ctx, "myqueue")
            val received = receiver.recvMessages(msgs.size)
            println("Received messages: ${received}")
        }
    }
}

