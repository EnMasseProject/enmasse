package enmasse.smoketest

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class NoStoreAnycastTest {

    @Test
    fun testMessagesDeliveredToReceiver() {
        val dest = "anycast"
        val client = EnMasseClient(createQueueContext(dest), 2, true)

        val msgs = listOf("foo", "bar", "baz")

        val executor = Executors.newFixedThreadPool(2)
        val recvResult = executor.submit<List<String>> { client.recvMessages(dest, msgs.size) }
        val sendResult = executor.submit<Int> { client.sendMessages(dest, msgs) }

        executor.shutdown()
        assertTrue("Clients did not terminate within timeout", executor.awaitTermination(60, TimeUnit.SECONDS))

        val numReceived = recvResult.get()
        val numSent = sendResult.get()

        assertEquals(msgs.size, numReceived.size)
        assertEquals(msgs.size, numSent)
    }
}