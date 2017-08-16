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

package io.enmasse.config.service.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.enmasse.address.model.types.AddressType;
import io.enmasse.address.model.Address;
import io.enmasse.address.model.Status;
import io.enmasse.address.model.v1.CodecV1;
import io.enmasse.address.model.types.standard.StandardType;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ConfigMessageEncoderTest {
    @Test
    public void testEncoder() throws IOException {
        ConfigMessageEncoder encoder = new ConfigMessageEncoder();

        Set<ConfigResource> configSet = new LinkedHashSet<>(Arrays.asList(
                new ConfigResource(createConfigMap("c1", "myqueue", StandardType.QUEUE)),
                new ConfigResource(createConfigMap("c2", "myqueue2", StandardType.QUEUE)),
                new ConfigResource(createConfigMap("c3", "mytopic", StandardType.TOPIC))));

        Message message = encoder.encode(configSet);
        String json = (String) ((AmqpValue) message.getBody()).getValue();
        assertThat(message.getSubject(), is("enmasse.io/v1/AddressList"));
        assertThat(json, is("{\"apiVersion\":\"enmasse.io/v1\",\"kind\":\"AddressList\",\"items\":[{\"metadata\":{\"name\":\"c1\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"type\":\"queue\",\"plan\":\"inmemory\",\"address\":\"myqueue\"},\"status\":{\"isReady\":false}},{\"metadata\":{\"name\":\"c2\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"type\":\"queue\",\"plan\":\"inmemory\",\"address\":\"myqueue2\"},\"status\":{\"isReady\":false}},{\"metadata\":{\"name\":\"c3\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"type\":\"topic\",\"plan\":\"inmemory\",\"address\":\"mytopic\"},\"status\":{\"isReady\":false}}]}"));

    }

    private ConfigMap createConfigMap(String name, String address, AddressType addressType) throws JsonProcessingException, UnsupportedEncodingException {
        Map<String, String> data = new LinkedHashMap<>();
        Address addr = new Address.Builder()
                .setName(name)
                .setAddress(address)
                .setAddressSpace("unknown")
                .setType(addressType)
                .setPlan(addressType.getPlans().get(0))
                .setUuid("1234")
                .setStatus(new Status(false))
                .build();
        data.put("config.json", CodecV1.getMapper().writeValueAsString(addr));
        return new ConfigMapBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .build())
                .withData(data)
                .build();
    }
}
