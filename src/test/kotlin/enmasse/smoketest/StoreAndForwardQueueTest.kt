package enmasse.smoketest

import io.kotlintest.specs.StringSpec

/**
 * @author Ulf Lilleengen
 */
class StoreAndForwardQueueTest : StringSpec() {
    init {
        val dest = "myqueue"
        val client = EnMasseClient(createQueueContext(dest), 1, false)

        "Messages should be queued" {
            val msgs = listOf("foo", "bar", "baz")
            client.sendMessages(dest, msgs) shouldBe msgs.size

            val received = client.recvMessages(dest, msgs.size)
            received.size shouldBe msgs.size
        }
    }
}

