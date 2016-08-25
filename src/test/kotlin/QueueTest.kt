import enmasse.perf.Environment
import enmasse.perf.TestReceiver
import enmasse.perf.TestSender
import enmasse.perf.createQueueContext
import io.kotlintest.specs.StringSpec

/**
 * @author Ulf Lilleengen
 */
class QueueTest : StringSpec() {
    init {
        val ctx = createQueueContext(Environment.endpoint, "myqueue")
        "Messages should be queued" {
            val sender = TestSender(ctx)
            sender.sendMessages(10, "myqueue") shouldBe 10

            val receiver = TestReceiver(ctx)
            receiver.recvMessages(10, "myqueue") shouldBe 10
        }
    }
}

