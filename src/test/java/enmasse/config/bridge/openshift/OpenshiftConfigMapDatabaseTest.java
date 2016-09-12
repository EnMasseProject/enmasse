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
import enmasse.config.bridge.model.ConfigSubscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import enmasse.config.bridge.openshift.OpenshiftConfigMapDatabase;

import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.*;

public class OpenshiftConfigMapDatabaseTest {
    private OpenshiftConfigMapDatabase database;

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
        database.subscribe("foo", sub);

        verifyZeroInteractions(sub);

        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        connectWithValues(testValue);

        verify(sub).configUpdated("foo", "1234", testValue);
    }

    @Test
    public void testSubscribeAfterConnected() {

        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        connectWithValues(testValue);

        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        database.subscribe("foo", sub);

        verify(sub).configUpdated("foo", "1234", testValue);
    }

    @Test
    public void testUpdates() {
        Map<String, String> testValue = Collections.singletonMap("bar", "baz");
        connectWithValues(testValue);

        ConfigSubscriber sub = mock(ConfigSubscriber.class);
        database.subscribe("foo", sub);

        verify(sub).configUpdated("foo", "1234", testValue);

        testValue = Collections.singletonMap("quux", "bim");
        IConfigMap newMap = mock(IConfigMap.class);
        when(newMap.getName()).thenReturn("foo");
        when(newMap.getResourceVersion()).thenReturn("1235");
        when(newMap.getData()).thenReturn(testValue);
        database.received(newMap, IOpenShiftWatchListener.ChangeType.MODIFIED);

        verify(sub).configUpdated("foo", "1235", testValue);
    }

    private void connectWithValues(Map<String, String> testValue) {
        IConfigMap testMap = mock(IConfigMap.class);
        when(testMap.getName()).thenReturn("foo");
        when(testMap.getResourceVersion()).thenReturn("1234");
        when(testMap.getData()).thenReturn(testValue);
        database.connected(Collections.singletonList(testMap));
    }
}
