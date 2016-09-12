/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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