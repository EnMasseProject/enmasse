/*
 * Copyright 2018-2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.k8s.api.cache;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class EventCacheTest {

    @SuppressWarnings("unchecked")
    private <T> Processor<T> mockProcessor() {
        return mock(Processor.class);
    }

    @Test
    public void testAdd() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.add(map("ns", "k1"));
        assertTrue(queue.hasSynced());
        assertFalse(queue.listKeys().contains("ns/k1"));

        Processor<ConfigMap> mockProc = mockProcessor();
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq(map("ns", "k1")));
        assertTrue(queue.listKeys().contains("ns/k1"));
        assertTrue(queue.list().contains(map("ns", "k1")));
    }

    @Test
    public void testAddMultiple() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.add(map("ns1", "k1"));
        queue.add(map("ns2", "k1"));
        assertTrue(queue.hasSynced());
        assertFalse(queue.listKeys().contains("ns1/k1"));
        assertFalse(queue.listKeys().contains("ns2/k1"));

        Processor<ConfigMap> mockProc = mockProcessor();
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        // we only get the first event
        verify(mockProc).process(eq(map("ns1", "k1")));

        assertTrue(queue.listKeys().contains("ns1/k1"));
        assertTrue(queue.listKeys().contains("ns2/k1"));
        assertTrue(queue.list().contains(map("ns1", "k1")));
        assertTrue(queue.list().contains(map("ns2", "k1")));
    }

    @Test
    public void testUpdate() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.update(map("ns", "k1"));
        assertFalse(queue.listKeys().contains("ns/k1"));
        assertFalse(queue.list().contains(map("ns", "k1")));

        Processor<ConfigMap> mockProc = mockProcessor();
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq(map("ns", "k1")));
        assertTrue(queue.listKeys().contains("ns/k1"));
        assertTrue(queue.list().contains(map("ns", "k1")));
    }

    @Test
    public void testRemove() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.add(map("ns", "k1"));
        queue.delete(map("ns", "k1"));
        assertTrue(queue.hasSynced());
        assertTrue(queue.listKeys().isEmpty());

        Processor<ConfigMap> mockProc = mockProcessor();
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq(map("ns", "k1")));
        assertTrue(queue.listKeys().isEmpty());
        assertTrue(queue.list().isEmpty());

        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(eq(map("ns", "k1")));
        assertTrue(queue.listKeys().isEmpty());
        assertTrue(queue.list().isEmpty());
    }

    @Test
    public void testEmpty() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        Processor<ConfigMap> mockProc = mockProcessor();
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verifyZeroInteractions(mockProc);
        assertFalse(queue.hasSynced());
    }

    @Test
    public void testWakeup() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.wakeup();
        Processor<ConfigMap> mockProc = mockProcessor();
        queue.pop(mockProc, 1, TimeUnit.DAYS);
        verifyZeroInteractions(mockProc);
        assertFalse(queue.hasSynced());
    }

    @Test
    public void testSync() throws Exception {
        WorkQueue<ConfigMap> queue = new EventCache<>(new HasMetadataFieldExtractor<>());
        queue.replace(Arrays.asList(map("ns", "k1"), map("ns", "k2"), map("ns", "k3")), "33");
        assertFalse(queue.hasSynced());
        assertFalse(queue.list().isEmpty());

        Processor<ConfigMap> mockProc = mockProcessor();
        queue.pop(mockProc, 0, TimeUnit.SECONDS);
        verify(mockProc).process(null);
        assertTrue(queue.hasSynced());
    }

    public static ConfigMap map(String namespace, String name) {
        return new ConfigMapBuilder()
                .editOrNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .build();
    }
}
