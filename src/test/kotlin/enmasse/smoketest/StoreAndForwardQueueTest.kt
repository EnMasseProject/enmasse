package enmasse.smoketest

import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class StoreAndForwardQueueTest {

    fun testQueue() {
        val dest = "myqueue"
        val client = EnMasseClient(createQueueContext(dest), 1, false)

        val executor = Executors.newSingleThreadExecutor()

        val msgs = listOf("foo", "bar", "baz")
        val numSent = executor.submit<Int>{ client.sendMessages(dest, msgs) }
        val received = executor.submit<List<String>>{ client.recvMessages(dest, msgs.size) }

        executor.shutdown()
        assertTrue("Clients did not terminate within timeout", executor.awaitTermination(30, TimeUnit.SECONDS))

        assertEquals(msgs.size, numSent.get())
        assertEquals(msgs.size, received.get().size)
    }
}

