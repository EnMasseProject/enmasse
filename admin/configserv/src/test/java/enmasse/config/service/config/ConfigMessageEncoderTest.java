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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.enmasse.address.model.AddressType;
import io.enmasse.address.model.impl.Address;
import io.enmasse.address.model.impl.AddressStatus;
import io.enmasse.address.model.impl.k8s.v1.address.AddressCodec;
import io.enmasse.address.model.impl.types.standard.StandardType;
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
        String json = new String((byte[]) ((AmqpValue) message.getBody()).getValue(), "UTF-8");
        assertThat(json, is("{\"kind\":\"AddressList\",\"apiVersion\":\"enmasse.io/v1\",\"items\":[{\"kind\":\"Address\",\"apiVersion\":\"enmasse.io/v1\",\"kind\":\"Address\",\"metadata\":{\"name\":\"c1\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"address\":\"myqueue\",\"plan\":\"inmemory\",\"type\":\"queue\"}},{\"kind\":\"Address\",\"apiVersion\":\"enmasse.io/v1\",\"kind\":\"Address\",\"metadata\":{\"name\":\"c2\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"address\":\"myqueue2\",\"plan\":\"inmemory\",\"type\":\"queue\"}},{\"kind\":\"Address\",\"apiVersion\":\"enmasse.io/v1\",\"kind\":\"Address\",\"metadata\":{\"name\":\"c3\",\"addressSpace\":\"unknown\",\"uuid\":\"1234\"},\"spec\":{\"address\":\"mytopic\",\"plan\":\"inmemory\",\"type\":\"topic\"}}]}"));
    }

    private ConfigMap createConfigMap(String name, String address, AddressType addressType) throws JsonProcessingException, UnsupportedEncodingException {
        Map<String, String> data = new LinkedHashMap<>();
        byte[] json = new AddressCodec().encodeAddress(new Address.Builder()
                .setName(name)
                .setAddress(address)
                .setAddressSpace("unknown")
                .setType(addressType)
                .setPlan(addressType.getPlans().get(0))
                .setUuid("1234")
                .setStatus(new AddressStatus(false))
                .build());
        data.put("json", new String(json, "UTF-8"));
        return new ConfigMapBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(name)
                        .build())
                .withData(data)
                .build();
    }
}
