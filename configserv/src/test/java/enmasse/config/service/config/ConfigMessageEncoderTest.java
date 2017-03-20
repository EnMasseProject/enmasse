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

package enmasse.config.service.config;

import enmasse.config.AddressEncoder;
import enmasse.config.LabelKeys;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigMessageEncoderTest {
    @Test
    public void testEncoder() throws IOException {
        ConfigMessageEncoder encoder = new ConfigMessageEncoder();

        Set<ConfigResource> configSet = new LinkedHashSet<>(Arrays.asList(
                new ConfigResource(createConfigMap("c1", Arrays.asList("myqueue", "myqueue2"), true, false)),
                new ConfigResource(createConfigMap("c2", Arrays.asList("mytopic"), true, true))));

        Message message = encoder.encode(configSet);
        String json = (String) ((AmqpValue) message.getBody()).getValue();
        assertThat(json, is("{\"myqueue\":{\"store_and_forward\":true,\"multicast\":false,\"group_id\":\"c1\"},\"myqueue2\":{\"store_and_forward\":true,\"multicast\":false,\"group_id\":\"c1\"},\"mytopic\":{\"store_and_forward\":true,\"multicast\":true,\"group_id\":\"c2\"}}"));
    }

    private ConfigMap createConfigMap(String name, List<String> addresses, boolean storeAndForward, boolean multicast) {
        Map<String, String> data = new LinkedHashMap<>();
        for (String address : addresses) {
            AddressEncoder encoder = new AddressEncoder();
            encoder.encode(storeAndForward, multicast, Optional.empty());
            data.put(address, encoder.toJson());
        }
        return new ConfigMapBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .addToLabels(LabelKeys.GROUP_ID, name)
                        .build())
                .withData(data)
                .build();
    }
}
