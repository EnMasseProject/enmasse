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

import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.model.IConfigMap;
import enmasse.config.service.amqp.subscription.AddressConfigCodec;
import enmasse.config.service.model.Config;
import enmasse.config.service.model.ConfigSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OpenshiftConfigDatabaseTest {
    private OpenshiftConfigDatabase database;
    private String key = "maas";
    private OpenshiftClient client;
    private ScheduledExecutorService executor;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        client = mock(OpenshiftClient.class);
        IWatcher mockWatcher = mock(IWatcher.class);
        executor = Executors.newSingleThreadScheduledExecutor();
        database = new OpenshiftConfigDatabase(executor, client);
        when(client.watch(any(), any())).thenReturn(mockWatcher);
    }

    public IOpenShiftWatchListener getListener() {
        ArgumentCaptor<IOpenShiftWatchListener> captor = ArgumentCaptor.forClass(IOpenShiftWatchListener.class);
        verify(client).watch(captor.capture(), anyString(), anyString());
        return captor.getValue();
    }

    @After
    public void teardown() throws Exception {
        database.close();
    }

    @Test
    public void testSubscribeWithBadKey() {
        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        assertFalse(database.subscribe("nosuchkey", sub));
    }

    @Test
    public void testSubscribeAfterConnected() throws InterruptedException {

        Map<String, String> testValue = AddressConfigCodec.encodeLabels("foo", true, false);
        TestSubscriber sub = new TestSubscriber();

        assertTrue(database.subscribe(key, sub));
        waitForExecutor();
        IOpenShiftWatchListener listener = getListener();

        listener.connected(Collections.singletonList(mockMap(testValue)));

        assertNotNull(sub.lastValue);
        assertFalse(sub.lastValue.isEmpty());
        assertConfig(sub.lastValue.get(0), testValue);
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

        assertTrue(database.subscribe(key, sub));
        waitForExecutor();

        IOpenShiftWatchListener listener = getListener();
        listener.connected(Collections.singletonList(mockMap(test1)));

        assertNotNull(sub.lastValue);
        assertFalse(sub.lastValue.isEmpty());
        assertConfig(sub.lastValue.get(0), test1);

        Map<String, String> test2 = AddressConfigCodec.encodeLabels("bar", true, false);
        listener.connected(Collections.singletonList(mockMap(test2)));

        assertNotNull(sub.lastValue);
        assertFalse(sub.lastValue.isEmpty());
        assertConfig(sub.lastValue.get(0), test2);
    }

    private static void assertConfig(Config config, Map<String, String> testValue) {
        for (Map.Entry<String, String> entry : testValue.entrySet()) {
            assertThat(config.getValue(entry.getKey()), is(entry.getValue()));
        }
    }

    private IConfigMap mockMap(Map<String, String> testValue) {
        IConfigMap testMap = mock(IConfigMap.class);
        when(testMap.getName()).thenReturn("map1");
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("type", "address-config");
        labels.putAll(testValue);
        when(testMap.getLabels()).thenReturn(labels);
        return testMap;
    }

    public static class TestSubscriber implements ConfigSubscriber {
        public List<Config> lastValue;

        @Override
        public void configUpdated(List<Config> values) {
            lastValue = values;
        }
    }
}
