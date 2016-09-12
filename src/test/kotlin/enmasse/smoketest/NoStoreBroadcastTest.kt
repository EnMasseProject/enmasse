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
class NoStoreBroadcastTest {
    @Test
    fun testMultipleRecievers() {
        val dest = "broadcast"
        val client = EnMasseClient(createQueueContext(dest), 4, true)
        val msgs = listOf("foo", "bar", "baz")

        val executor = Executors.newFixedThreadPool(4)
        val recvResults = 1.rangeTo(3).map { i -> executor.submit<List<String>> { client.recvMessages(dest, msgs.size) } }
        val sendResult = executor.submit<Int> { client.sendMessages(dest, msgs) }

        executor.shutdown()
        assertTrue("Clients did not terminate within timeout", executor.awaitTermination(60, TimeUnit.SECONDS))

        assertEquals(msgs.size, sendResult.get())
        recvResults.forEach { result -> assertEquals(msgs.size, result.get().size) }
    }
}