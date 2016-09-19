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
import com.openshift.restclient.ResourceKind;
import com.openshift.restclient.model.IConfigMap;
import enmasse.config.bridge.model.ConfigMap;
import enmasse.config.bridge.model.ConfigSubscriber;
import enmasse.config.bridge.model.LabelSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class OpenshiftConfigMapDatabaseTest {
    private OpenshiftConfigMapDatabase database;
    private LabelSet key = LabelSet.fromString("foo=bar");

    @Before
    public void setup() {
        IClient client = mock(IClient.class);
        database = new OpenshiftConfigMapDatabase(client, "testspace");
        database.start();
        verify(client).watch("testspace", database, ResourceKind.CONFIG_MAP);
    }

    @After
    public void teardown() throws Exception {
        database.close();
    }

    @Test
    public void testSubscribeBeforeConnected() {
        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        assertFalse(database.subscribe(key, sub));
    }

    @Test
    public void testSubscribeAfterConnected() {

        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        connectWithValues(testValue);

        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        assertTrue(database.subscribe(key, sub));

        verify(sub).configUpdated(Collections.singletonMap("foo", new ConfigMap(testValue)));
    }

    @Test
    public void testUpdates() {
        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        connectWithValues(testValue);

        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        database.subscribe(key, sub);

        verify(sub).configUpdated(Collections.singletonMap("foo", new ConfigMap(testValue)));

        testValue = Collections.singletonMap("quux", "bim");
        IConfigMap newMap = mockMap(testValue);
        database.received(newMap, IOpenShiftWatchListener.ChangeType.MODIFIED);

        verify(sub).configUpdated(Collections.singletonMap("foo", new ConfigMap(testValue)));
    }

    private IConfigMap mockMap(Map<String, String> testValue) {
        IConfigMap testMap = mock(IConfigMap.class);
        when(testMap.getName()).thenReturn("foo");
        Map<String, String> labels = new LinkedHashMap<>(key.getLabelMap());
        labels.put("extra", "label");
        when(testMap.getLabels()).thenReturn(labels);
        when(testMap.getData()).thenReturn(testValue);
        return testMap;
    }

    private void connectWithValues(Map<String, String> testValue) {
        IConfigMap testMap = mockMap(testValue);
        database.connected(Collections.singletonList(testMap));
    }
}
