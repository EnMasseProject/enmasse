package enmasse.smoketest

import org.junit.Test
import org.junit.Assert.*

/**
 * @author Ulf Lilleengen
 */
class StoreAndForwardQueueTest {

    @Test
    fun testQueue() {
        val dest = "myqueue"
        val client = EnMasseClient(createQueueContext(dest), 1, false)

        val msgs = listOf("foo", "bar", "baz")
        val numSent = client.sendMessages(dest, msgs)
        assertEquals(msgs.size, numSent)

        val received = client.recvMessages(dest, msgs.size)
        assertEquals(msgs.size, received.size)
    }
}

