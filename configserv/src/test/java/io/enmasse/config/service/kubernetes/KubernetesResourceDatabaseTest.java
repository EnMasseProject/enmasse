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

package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.TestResource;
import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.config.service.model.Subscriber;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.apache.qpid.proton.amqp.messaging.AmqpSequence;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class KubernetesResourceDatabaseTest {
    private KubernetesResourceDatabase<TestResource> database;
    private KubernetesClient client;
    private ScheduledExecutorService executor;
    private MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>> mapOp = mock(MixedOperation.class);
    private Map<String, String> testLabels = Collections.singletonMap("l1", "v1");
    private Map<String, String> testAnnotations = Collections.singletonMap("a1", "v1");

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        client = mock(KubernetesClient.class);
        Watch mockWatcher = mock(Watch.class);
        executor = Executors.newSingleThreadScheduledExecutor();
        database = new KubernetesResourceDatabase<>(client, new TestSubscriptionConfig());
        when(client.configMaps()).thenReturn(mapOp);

        when(mapOp.withLabels(any())).thenReturn(mapOp);
        when(mapOp.withResourceVersion(anyString())).thenReturn(mapOp);
        when(mapOp.watch(any())).thenReturn(() -> {});

        when(mapOp.list()).thenReturn(new ConfigMapListBuilder().withNewMetadata()
                .withResourceVersion("1234")
                .endMetadata().addToItems(createResource("r1")).build());
    }

    public Watcher getListener() {
        ArgumentCaptor<Watcher> captor = ArgumentCaptor.forClass(Watcher.class);
        verify(mapOp).watch(captor.capture());
        return captor.getValue();
    }

    @After
    public void teardown() throws Exception {
        database.close();
    }

    @Test
    public void testSubscribeAfterConnected() throws Exception {

        TestSubscriber sub = new TestSubscriber();
        ObserverKey key = new ObserverKey(Collections.emptyMap(), Collections.emptyMap());

        database.subscribe(key, sub);
        waitForExecutor();
        Watcher listener = getListener();

        listener.eventReceived(Watcher.Action.ADDED, createResource("r1"));

        assertNotNull(sub.lastValue);
        assertValue(sub.lastValue, "val");
    }

    private void waitForExecutor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> latch.countDown());
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testUpdates() throws Exception {
        TestSubscriber sub = new TestSubscriber();
        ObserverKey key = new ObserverKey(testLabels, testAnnotations);

        database.subscribe(key, sub);
        waitForExecutor();

        Watcher listener = getListener();
        listener.eventReceived(Watcher.Action.ADDED, createResource("r1", "v1"));

        assertNotNull(sub.lastValue);
        assertValue(sub.lastValue, "v1");

        listener.eventReceived(Watcher.Action.ADDED, createResource("r2", "v2"));

        assertNotNull(sub.lastValue);
        assertValue(sub.lastValue, "v1", "v2");

        listener.eventReceived(Watcher.Action.DELETED, createResource("r1"));
        assertNotNull(sub.lastValue);
        assertValue(sub.lastValue, "v2");

        listener.eventReceived(Watcher.Action.MODIFIED, createResource("r2", "v22"));
        assertNotNull(sub.lastValue);
        assertValue(sub.lastValue, "v22");
    }

    private static void assertValue(Message message, String ... values) {
        AmqpSequence seq = (AmqpSequence) message.getBody();
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(values));
        Set<String> actual = new LinkedHashSet<>();
        for (Object o : seq.getValue()) {
            actual.add((String) o);
        }
        assertEquals(expected, actual);
    }

    private TestResource.TestValue createResource(String name) {
        return createResource(name, "val");
    }

    private TestResource.TestValue createResource(String name, String value) {
        return new TestResource.TestValue(name, testLabels, testAnnotations, value);
    }

    public static class TestSubscriber implements Subscriber {
        public Message lastValue = null;

        @Override
        public void resourcesUpdated(Message message) {
            lastValue = message;
        }
    }
}
