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

package enmasse.config.service.model;

import com.openshift.restclient.model.IConfigMap;
import enmasse.config.service.amqp.subscription.AddressConfigCodec;
import enmasse.config.service.openshift.ConfigResourceListener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigResourceListenerTest {

    @Captor
    private ArgumentCaptor<List<Config>> captor;

    @Test
    public void testSubscribing() {
        ConfigResourceListener listener = new ConfigResourceListener();
        ConfigSubscriber mockSub = mock(ConfigSubscriber.class);
        listener.subscribe(mockSub);
        listener.resourcesUpdated(Collections.singleton(mockMap(AddressConfigCodec.encodeLabels("foo", true, false))));
        verify(mockSub).configUpdated(captor.capture());

        List<Config> cfg = captor.getValue();
        assertThat(cfg.size(), is(1));
        assertNotNull(cfg.get(0));
        assertThat(cfg.get(0).getValue("address"), is("foo"));
        assertThat(cfg.get(0).getValue("store_and_forward"), is("true"));
        assertThat(cfg.get(0).getValue("multicast"), is("false"));

    }

    private IConfigMap mockMap(Map<String, String> testValue) {
        IConfigMap testMap = mock(IConfigMap.class);
        when(testMap.getName()).thenReturn("foo");
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("type", "address-config");
        labels.putAll(testValue);
        when(testMap.getLabels()).thenReturn(labels);
        return testMap;
    }
}
