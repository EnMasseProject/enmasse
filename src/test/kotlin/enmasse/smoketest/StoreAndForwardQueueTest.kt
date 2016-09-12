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

import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class StoreAndForwardQueueTest {

    @Test
    fun testQueue() {
        val dest = "myqueue"
        val client = EnMasseClient(createQueueContext(dest), 1, false)

        val executor = Executors.newSingleThreadExecutor()

        val msgs = listOf("foo", "bar", "baz")
        val numSent = executor.submit<Int>{ client.sendMessages(dest, msgs) }
        val received = executor.submit<List<String>>{ client.recvMessages(dest, msgs.size) }

        executor.shutdown()
        assertTrue("Clients did not terminate within timeout", executor.awaitTermination(60, TimeUnit.SECONDS))

        assertEquals(msgs.size, numSent.get())
        assertEquals(msgs.size, received.get().size)
    }
}

