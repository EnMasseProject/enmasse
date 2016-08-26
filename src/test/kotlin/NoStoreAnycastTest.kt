import enmasse.perf.Environment
import enmasse.perf.TestReceiver
import enmasse.perf.TestSender
import enmasse.perf.createQueueContext
import io.kotlintest.specs.StringSpec
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class NoStoreAnycastTest: StringSpec() {
    init {
        val ctx = createQueueContext(Environment.endpoint, "anycast")
        "Messages should be delivered to receiver" {
            val sender = TestSender(ctx, "myqueue")
            val msgs = listOf("foo", "bar", "baz")
            sender.sendMessages(msgs) shouldBe msgs.size
            println("Sent ${msgs.size} messages")

            val receiver = TestReceiver(ctx, "myqueue")
            val received = receiver.recvMessages(msgs.size)

            println("Received ${received.size} messages")
            received.size shouldBe msgs.size
        }
    }
}