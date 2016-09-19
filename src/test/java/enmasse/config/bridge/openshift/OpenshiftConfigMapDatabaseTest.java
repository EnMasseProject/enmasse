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

package enmasse.config.bridge.openshift;

import com.openshift.restclient.IClient;
import com.openshift.restclient.IOpenShiftWatchListener;
import com.openshift.restclient.IWatcher;
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IConfigMap;
import enmasse.config.bridge.model.ConfigMap;
import enmasse.config.bridge.model.ConfigSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class OpenshiftConfigMapDatabaseTest {
    private OpenshiftConfigMapDatabase database;
    private String key = "maas";
    private IClient client;
    private String namespace = "testspace";

    @Before
    public void setup() {
        client = mock(IClient.class);
        IWatcher mockWatcher = mock(IWatcher.class);
        database = new OpenshiftConfigMapDatabase(client, namespace);
        when(client.watch(any(), any(), any())).thenReturn(mockWatcher);
    }

    public IOpenShiftWatchListener getListener() {
        ArgumentCaptor<IOpenShiftWatchListener> captor = ArgumentCaptor.forClass(IOpenShiftWatchListener.class);
        verify(client).watch(anyString(), captor.capture(), anyString());
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
    public void testSubscribeAfterConnected() {

        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        ConfigSubscriber sub = mock(ConfigSubscriber.class);

        assertTrue(database.subscribe(key, sub));
        IOpenShiftWatchListener listener = getListener();

        listener.connected(Collections.singletonList(mockMap(testValue)));

        verify(sub).configUpdated(Collections.singletonMap("foo", new ConfigMap(testValue)));
    }

    @Test
    public void testUpdates() {
        Map<String, String> testValue = Collections.singletonMap("bar", "baz");

        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        assertTrue(database.subscribe(key, sub));

        IOpenShiftWatchListener listener = getListener();
        listener.connected(Collections.singletonList(mockMap(testValue)));

        verify(sub).configUpdated(Collections.singletonMap("foo", new ConfigMap(testValue)));

        testValue = Collections.singletonMap("quux", "bim");
        listener.connected(Collections.singletonList(mockMap(testValue)));

        verify(sub).configUpdated(Collections.singletonMap("foo", new ConfigMap(testValue)));
    }

    private IConfigMap mockMap(Map<String, String> testValue) {
        IConfigMap testMap = mock(IConfigMap.class);
        when(testMap.getName()).thenReturn("foo");
        Map<String, String> labels = Collections.singletonMap("type", "address-config");
        when(testMap.getLabels()).thenReturn(labels);
        when(testMap.getData()).thenReturn(testValue);
        return testMap;
    }
}
