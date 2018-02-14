/*
 * Copyright 2016-2018, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package io.enmasse.config.service.kubernetes;

import io.enmasse.config.service.TestResource;
import io.enmasse.config.service.model.ObserverKey;
import io.enmasse.config.service.model.Subscriber;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class KubernetesResourceDatabaseTest {
    private KubernetesResourceDatabase<TestResource> database;
    private MixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, Resource<ConfigMap, DoneableConfigMap>> mapOp = mock(MixedOperation.class);
    private Map<String, String> testLabels = Collections.singletonMap("l1", "v1");
    private Map<String, String> testAnnotations = Collections.singletonMap("a1", "v1");
    private TestSubscriptionConfig.TestWatch watch;
    private BlockingQueue<Set<TestResource>> resourceQueue;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        this.watch = new TestSubscriptionConfig.TestWatch();
        resourceQueue = new LinkedBlockingQueue<>();
        database = new KubernetesResourceDatabase<>(null, new TestSubscriptionConfig(watch, resourceQueue));
    }

    @After
    public void teardown() throws Exception {
        database.close();
    }

    @Test
    public void testSubscribeAfterConnected() throws Exception {

        TestSubscriber sub = new TestSubscriber();
        ObserverKey key = new ObserverKey(Collections.emptyMap(), Collections.emptyMap());
        resourceQueue.put(Collections.singleton(new TestResource("k1", "v1")));

        database.subscribe(key, sub);

        waitForMessage(sub, Arrays.asList("v1"));
    }

    private void waitForMessage(TestSubscriber sub, List<String> expected) throws InterruptedException {
        long endTime = System.currentTimeMillis() + 60_000;
        List<String> values = null;
        while (System.currentTimeMillis() < endTime) {

            if (sub.lastValue != null) {
                values = (List<String>) ((AmqpValue)sub.lastValue.getBody()).getValue();
                if (expected.equals(values)) {
                    break;
                }
            }
            Thread.sleep(1000);
        }
        assertNotNull(values);
        assertEquals(expected, values);
    }

    @Test
    public void testUpdates() throws Exception {
        TestSubscriber sub = new TestSubscriber();
        ObserverKey key = new ObserverKey(testLabels, testAnnotations);


        database.subscribe(key, sub);

        resourceQueue.put(Collections.singleton(new TestResource("r1", "v1")));

        waitForMessage(sub, Arrays.asList("v1"));

        resourceQueue.put(Collections.singleton(new TestResource("r1", "v2")));

        waitForMessage(sub, Arrays.asList("v2"));
    }

    public static class TestSubscriber implements Subscriber {
        public volatile Message lastValue = null;

        @Override
        public String getId() {
            return "test";
        }

        @Override
        public void resourcesUpdated(Message message) {
            lastValue = message;
        }
    }
}
