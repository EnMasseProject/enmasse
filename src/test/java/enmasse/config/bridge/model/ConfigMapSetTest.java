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

package enmasse.config.bridge.model;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ConfigMapSetTest {
    @Test
    public void testSubscribing() {
        ConfigMapSet map = new ConfigMapSet();
        ConfigSubscriber mockSub = mock(ConfigSubscriber.class);
        map.subscribe(mockSub);
        map.mapUpdated("1234", Collections.singletonMap("foo", "bar"));
        ArgumentCaptor<Map<String, ConfigMap>> capture = ArgumentCaptor.forClass(Map.class);
        verify(mockSub).configUpdated(capture.capture());

        Map<String, ConfigMap> cfg = capture.getValue();
        assertThat(cfg.size(), is(1));
        assertNotNull(cfg.get("1234"));
        assertThat(cfg.get("1234").getData().size(), is(1));
        assertThat(cfg.get("1234").getData().get("foo"), is("bar"));
    }
}
