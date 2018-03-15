/*
 * Copyright 2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class FifoQueueTest {
    @Test
    public void testAdd() throws Exception {
        WorkQueue<String> queue = new FifoQueue<>(s -> s);
        queue.add("k1");
        assertTrue(queue.hasSynced());
        assertFalse(queue.listKeys().contains("k1"));

        Processor<String> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq("k1"));
        assertTrue(queue.listKeys().contains("k1"));
        assertTrue(queue.list().contains("k1"));
    }

    @Test
    public void testUpdate() throws Exception {
        WorkQueue<String> queue = new FifoQueue<>(s -> s);
        queue.update("k1");
        assertFalse(queue.listKeys().contains("k1"));
        assertFalse(queue.list().contains("k1"));

        Processor<String> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq("k1"));
        assertTrue(queue.listKeys().contains("k1"));
        assertTrue(queue.list().contains("k1"));
    }

    @Test
    public void testRemove() throws Exception {
        WorkQueue<String> queue = new FifoQueue<>(s -> s);
        queue.add("k1");
        queue.delete("k1");
        assertTrue(queue.hasSynced());
        assertTrue(queue.listKeys().isEmpty());

        Processor<String> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq("k1"));
        assertTrue(queue.listKeys().isEmpty());
        assertTrue(queue.list().isEmpty());

        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq("k1"));
        assertTrue(queue.listKeys().isEmpty());
        assertTrue(queue.list().isEmpty());
    }

    @Test
    public void testEmpty() throws Exception {
        WorkQueue<String> queue = new FifoQueue<>(s -> s);
        Processor<String> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verifyZeroInteractions(mockProc);
        assertFalse(queue.hasSynced());
    }

    @Test
    public void testSync() throws Exception {
        WorkQueue<String> queue = new FifoQueue<>(s -> s);
        queue.replace(Arrays.asList("k1", "k2", "k3"), "33");
        assertFalse(queue.hasSynced());
        assertFalse(queue.list().isEmpty());

        Processor<String> mockProc = mock(Processor.class);
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(null);
        assertTrue(queue.hasSynced());
    }
}
