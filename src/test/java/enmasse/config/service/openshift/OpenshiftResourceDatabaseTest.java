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

package enmasse.config.service.openshift;

import enmasse.config.service.TestResource;
import enmasse.config.service.config.AddressConfigCodec;
import enmasse.config.service.model.Subscriber;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.api.model.ListMeta;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import io.fabric8.openshift.client.OpenShiftClient;
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

public class OpenshiftResourceDatabaseTest {
    private OpenshiftResourceDatabase database;
    private String key = "maas";
    private OpenShiftClient client;
    private ScheduledExecutorService executor;
    private ClientMixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, ClientResource<ConfigMap, DoneableConfigMap>> mapOp = mock(ClientMixedOperation.class);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        client = mock(OpenShiftClient.class);
        Watch mockWatcher = mock(Watch.class);
        executor = Executors.newSingleThreadScheduledExecutor();
        database = new OpenshiftResourceDatabase(client, Collections.singletonMap(key, new TestSubscriptionConfig()));
        when(client.configMaps()).thenReturn(mapOp);

        when(mapOp.withLabels(any())).thenReturn(mapOp);
        when(mapOp.withResourceVersion(anyString())).thenReturn(mapOp);
        when(mapOp.watch(any())).thenReturn(() -> {});

        ListMeta listMeta = new ListMeta();
        listMeta.setResourceVersion("1234");
        when(mapOp.list()).thenReturn(new ConfigMapList("v1", Collections.emptyList(), "List", listMeta));
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
    public void testSubscribeWithBadKey() {
        Subscriber sub = mock(Subscriber.class);
        assertFalse(database.subscribe("nosuchkey", Collections.emptyMap(), sub));
    }

    @Test
    public void testSubscribeAfterConnected() throws InterruptedException {

        TestSubscriber sub = new TestSubscriber();

        assertTrue(database.subscribe(key, Collections.emptyMap(), sub));
        waitForExecutor();
        Watcher listener = getListener();

        listener.eventReceived(Watcher.Action.ADDED, createResource("r1"));

        assertNotNull(sub.lastValue);
        assertValue(sub.lastValue, "r1");
    }

    private void waitForExecutor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        executor.execute(() -> latch.countDown());
        latch.await(10, TimeUnit.SECONDS);
    }

    @Test
    public void testUpdates() throws InterruptedException {
        Map<String, String> test1 = AddressConfigCodec.encodeLabels("foo", true, false);
        TestSubscriber sub = new TestSubscriber();

        assertTrue(database.subscribe(key, Collections.emptyMap(), sub));
        waitForExecutor();

        Watcher listener = getListener();
        listener.eventReceived(Watcher.Action.ADDED, createResource("r1"));

        assertNotNull(sub.lastValue);
        assertValue(sub.lastValue, "r1");

        listener.eventReceived(Watcher.Action.ADDED, createResource("r2"));

        assertNotNull(sub.lastValue);
        assertValue(sub.lastValue, "r1", "r2");
    }

    private static void assertValue(Message message, String ... resourceIds) {
        AmqpSequence seq = (AmqpSequence) message.getBody();
        Set<String> expected = new LinkedHashSet<>(Arrays.asList(resourceIds));
        Set<String> actual = new LinkedHashSet<>();
        for (Object o : seq.getValue()) {
            actual.add((String)o);
        }
        assertEquals(expected, actual);
    }
    private static TestResource createResource(String name) {
        return new TestResource(name, Collections.singletonMap("key", "value"));
    }

    public static class TestSubscriber implements Subscriber {
        public Message lastValue = null;

        @Override
        public void resourcesUpdated(Message message) {
            lastValue = message;
        }
    }
}
