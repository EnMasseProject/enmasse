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

import com.fasterxml.jackson.databind.ObjectMapper;
import enmasse.config.service.amqp.subscription.AddressConfigCodec;
import enmasse.config.service.model.Config;
import enmasse.config.service.model.ConfigSubscriber;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.ClientMixedOperation;
import io.fabric8.kubernetes.client.dsl.ClientResource;
import io.fabric8.kubernetes.client.dsl.ClientScaleableResource;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.fabric8.kubernetes.client.dsl.internal.ConfigMapOperationsImpl;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigList;
import io.fabric8.openshift.api.model.DoneableDeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;
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
    private OpenShiftClient client;
    private ScheduledExecutorService executor;
    private ClientMixedOperation<ConfigMap, ConfigMapList, DoneableConfigMap, ClientResource<ConfigMap, DoneableConfigMap>> mapOp = mock(ClientMixedOperation.class);
    private ClientMixedOperation<DeploymentConfig, DeploymentConfigList, DoneableDeploymentConfig, ClientScaleableResource<DeploymentConfig, DoneableDeploymentConfig>> dcOp = mock(ClientMixedOperation.class);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        client = mock(OpenShiftClient.class);
        Watch mockWatcher = mock(Watch.class);
        executor = Executors.newSingleThreadScheduledExecutor();
        database = new OpenshiftConfigDatabase(client);
        when(client.configMaps()).thenReturn(mapOp);
        when(client.deploymentConfigs()).thenReturn(dcOp);

        when(mapOp.withLabels(any())).thenReturn(mapOp);
        when(dcOp.withLabels(any())).thenReturn(dcOp);
        when(mapOp.withResourceVersion(anyString())).thenReturn(mapOp);
        when(dcOp.withResourceVersion(anyString())).thenReturn(dcOp);

        when(mapOp.watch(any())).thenReturn(() -> {});
        when(dcOp.watch(any())).thenReturn(() -> {});

        ListMeta listMeta = new ListMeta();
        listMeta.setResourceVersion("1234");
        when(mapOp.list()).thenReturn(new ConfigMapList("v1", Collections.emptyList(), "List", listMeta));
        when(dcOp.list()).thenReturn(new DeploymentConfigList("v1", Collections.emptyList(), "List", listMeta));
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
        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        assertFalse(database.subscribe("nosuchkey", sub));
    }

    @Test
    public void testSubscribeAfterConnected() throws InterruptedException {

        Map<String, String> testValue = AddressConfigCodec.encodeLabels("foo", true, false);
        TestSubscriber sub = new TestSubscriber();

        assertTrue(database.subscribe(key, sub));
        waitForExecutor();
        Watcher listener = getListener();

        listener.eventReceived(Watcher.Action.ADDED, mockMap(testValue));

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

        Watcher listener = getListener();
        listener.eventReceived(Watcher.Action.ADDED, mockMap(test1));

        assertNotNull(sub.lastValue);
        assertFalse(sub.lastValue.isEmpty());
        assertConfig(sub.lastValue.get(0), test1);

        Map<String, String> test2 = AddressConfigCodec.encodeLabels("bar", true, false);
        listener.eventReceived(Watcher.Action.ADDED, mockMap(test2));

        assertNotNull(sub.lastValue);
        assertFalse(sub.lastValue.isEmpty());
        assertConfig(sub.lastValue.get(1), test2);
    }

    private static void assertConfig(Config config, Map<String, String> testValue) {
        for (Map.Entry<String, String> entry : testValue.entrySet()) {
            assertThat(config.getValue(entry.getKey()), is(entry.getValue()));
        }
    }

    private ConfigMap mockMap(Map<String, String> testValue) {
        ConfigMap testMap = new ConfigMap();
        ObjectMeta meta = new ObjectMeta();
        meta.setName("map1");
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("type", "address-config");
        labels.putAll(testValue);
        meta.setLabels(labels);
        testMap.setMetadata(meta);
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
